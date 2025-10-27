package ru.netology.nework.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentCreatePostBinding
import ru.netology.nework.data.Post
import ru.netology.nework.data.User
import ru.netology.nework.viewmodel.PostsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreatePostFragment : Fragment(), MenuProvider {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val postsViewModel: PostsViewModel by viewModels()
    private var attachmentUri: Uri? = null
    private var attachmentType: Post.AttachmentType? = null
    private var coordinates: Post.Coordinates? = null
    private var mentionedUserIds: List<Long> = emptyList()

    private val usersPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val users = result.data?.getParcelableArrayListExtra<User>("selectedUsers")
            users?.let { handleUsersSelection(it) }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleMediaSelection(uri, Post.AttachmentType.IMAGE)
            }
        }
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleMediaSelection(uri, Post.AttachmentType.VIDEO)
            }
        }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleMediaSelection(uri, Post.AttachmentType.AUDIO)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        setupTextWatchers()
        setupClickListeners()
        observePostCreation()
        setupAttachmentRemoval()
        setupMapResultListener()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_create_post, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save -> {
                createPost()
                true
            }

            else -> false
        }
    }

    private fun setupMapResultListener() {
        parentFragmentManager.setFragmentResultListener(
            MapFragment.LOCATION_SELECTION_KEY,
            viewLifecycleOwner
        ) { requestKey, bundle ->
            if (requestKey == MapFragment.LOCATION_SELECTION_KEY) {
                handleLocationSelection(bundle)
            }
        }
    }

    private fun handleLocationSelection(bundle: Bundle) {
        val latitude = bundle.getDouble(MapFragment.LATITUDE_KEY)
        val longitude = bundle.getDouble(MapFragment.LONGITUDE_KEY)

        coordinates = Post.Coordinates(latitude, longitude)
        updateSelectedLocationText()

        Snackbar.make(binding.root, "Локация выбрана", Snackbar.LENGTH_SHORT).show()
    }

    private fun openLocationPicker() {
        val currentCoords = coordinates?.let {
            MapFragment.newInstance(it.lat, it.long)
        } ?: MapFragment.newInstance()

        currentCoords.show(parentFragmentManager, MapFragment.TAG)
    }

    private fun openUsersPicker() {
        val dialog = SelectUsersDialog.newInstance(
            initiallySelectedUserIds = mentionedUserIds,
            multiSelect = true
        )

        dialog.onUsersSelected = { selectedUsers ->
            handleUsersSelection(selectedUsers)
        }

        dialog.show(parentFragmentManager, SelectUsersDialog.TAG)
    }

    private fun handleUsersSelection(selectedUsers: List<ru.netology.nework.data.User>) {
        mentionedUserIds = selectedUsers.map { it.id }
        updateMentionedUsersText(selectedUsers)
        println("Выбрано пользователей: ${selectedUsers.size}, IDs: $mentionedUserIds")
    }

    private fun updateMentionedUsersText(selectedUsers: List<ru.netology.nework.data.User>) {
        if (selectedUsers.isNotEmpty()) {
            binding.mentionedUsersText.visibility = View.VISIBLE

            val usersText = when (selectedUsers.size) {
                1 -> "👥 Упомянут: ${selectedUsers.first().name}"
                2, 3, 4 -> "👥 Упомянуты: ${selectedUsers.joinToString { it.name }}"
                else -> "👥 Упомянуто пользователей: ${selectedUsers.size}"
            }

            binding.mentionedUsersText.text = usersText

            binding.mentionedUsersText.setOnClickListener {
                showSelectedUsersPreview(selectedUsers)
            }
        } else {
            binding.mentionedUsersText.visibility = View.GONE
        }
    }

    private fun showSelectedUsersPreview(selectedUsers: List<ru.netology.nework.data.User>) {
        val userNames = selectedUsers.joinToString("\n") { "• ${it.name} (@${it.login})" }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Упомянутые пользователи")
            .setMessage(userNames)
            .setPositiveButton("Изменить") { _, _ ->
                openUsersPicker()
            }
            .setNegativeButton("ОК", null)
            .show()
    }

    private fun createPost() {
        val content = binding.contentEditText.text.toString().trim()

        if (!validateContent(content)) {
            Snackbar.make(binding.root, "Заполните текст поста", Snackbar.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uploadedAttachment = attachmentUri?.let { uri ->
                    try {
                        val mediaType = attachmentType ?: Post.AttachmentType.IMAGE
                        postsViewModel.uploadMedia(uri, mediaType)?.let { url ->
                            Post.Attachment(url, mediaType)
                        }
                    } catch (e: Exception) {
                        showError("Ошибка загрузки медиа: ${e.message}")
                        null
                    }
                }

                val currentDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                        .format(Date())

                val post = Post(
                    id = 0,
                    authorId = 0,
                    author = "",
                    content = content,
                    published = currentDate,
                    coords = coordinates,
                    mentionIds = mentionedUserIds,
                    attachment = uploadedAttachment
                )

                postsViewModel.save(post)
                Snackbar.make(binding.root, "Пост успешно создан", Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                showLoading(false)
                showError(e.message ?: "Неизвестная ошибка при создании поста")
            }
        }
    }

    private fun setupTextWatchers() {
        binding.contentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateContent(s.toString())
            }
        })
    }

    private fun setupClickListeners() {
        binding.selectLocationButton.setOnClickListener {
            openLocationPicker()
        }

        binding.selectUsersButton.setOnClickListener {
            openUsersPicker()
        }

        binding.attachImageButton.setOnClickListener {
            pickImageFromGallery()
        }

        binding.attachVideoButton.setOnClickListener {
            pickVideoFromGallery()
        }

        binding.attachAudioButton.setOnClickListener {
            pickAudioFromStorage()
        }

        binding.removeAttachmentButton.setOnClickListener {
            removeAttachment()
        }
    }

    private fun setupAttachmentRemoval() {
        binding.attachmentPreview.setOnClickListener {
            showAttachmentOptions()
        }
    }

    private fun observePostCreation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

            }
        }
    }


    private fun handleMediaSelection(uri: Uri, type: Post.AttachmentType) {
        attachmentUri = uri
        attachmentType = type

        when (type) {
            Post.AttachmentType.IMAGE -> showImageAttachmentPreview(uri)
            Post.AttachmentType.VIDEO -> showVideoAttachmentPreview(uri)
            Post.AttachmentType.AUDIO -> showAudioAttachmentPreview()
        }

        binding.removeAttachmentButton.visibility = View.VISIBLE
    }

    private fun showImageAttachmentPreview(uri: Uri) {
        binding.attachmentPreview.visibility = View.VISIBLE
        binding.attachmentTypeIndicator.visibility = View.GONE

        Glide.with(this)
            .load(uri)
            .centerCrop()
            .placeholder(R.drawable.ic_image)
            .into(binding.attachmentPreview)
    }

    private fun showVideoAttachmentPreview(uri: Uri) {
        binding.attachmentPreview.visibility = View.VISIBLE
        binding.attachmentTypeIndicator.visibility = View.VISIBLE
        binding.attachmentTypeIndicator.setImageResource(R.drawable.ic_video)

        Glide.with(this)
            .load(uri)
            .centerCrop()
            .placeholder(R.drawable.ic_video)
            .into(binding.attachmentPreview)
    }

    private fun showAudioAttachmentPreview() {
        binding.attachmentPreview.visibility = View.VISIBLE
        binding.attachmentTypeIndicator.visibility = View.VISIBLE
        binding.attachmentTypeIndicator.setImageResource(R.drawable.ic_audio)
        binding.attachmentPreview.setImageResource(R.drawable.ic_audio)
    }

    private fun removeAttachment() {
        attachmentUri = null
        attachmentType = null
        binding.attachmentPreview.visibility = View.GONE
        binding.attachmentTypeIndicator.visibility = View.GONE
        binding.removeAttachmentButton.visibility = View.GONE
    }

    private fun showAttachmentOptions() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Вложение")
            .setItems(arrayOf("Просмотреть", "Удалить")) { _, which ->
                when (which) {
                    0 -> openAttachmentForViewing()
                    1 -> removeAttachment()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openAttachmentForViewing() {
        attachmentUri?.let { uri ->
            val intent = when (attachmentType) {
                Post.AttachmentType.IMAGE -> Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                Post.AttachmentType.VIDEO -> Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                Post.AttachmentType.AUDIO -> Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "audio/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                null -> return
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Не найдено приложение для просмотра",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        pickImageLauncher.launch(intent)
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
        pickVideoLauncher.launch(intent)
    }

    private fun pickAudioFromStorage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickAudioLauncher.launch(intent)
    }

    private fun validateContent(content: String): Boolean {
        return if (content.isBlank()) {
            binding.contentEditText.error = "Текст поста не может быть пустым"
            false
        } else if (content.length < 5) {
            binding.contentEditText.error = "Текст поста должен содержать минимум 5 символов"
            false
        } else {
            binding.contentEditText.error = null
            true
        }
    }

    private suspend fun PostsViewModel.uploadMedia(uri: Uri, type: Post.AttachmentType): String? {
        return try {
            repository.uploadMedia(uri, type)
        } catch (e: Exception) {
            null
        }
    }

    private val locationSelectionListener = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            data?.extras?.let { bundle ->
                handleLocationSelection(bundle)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.contentEditText.isEnabled = !isLoading
        binding.selectLocationButton.isEnabled = !isLoading
        binding.selectUsersButton.isEnabled = !isLoading
        binding.attachImageButton.isEnabled = !isLoading
        binding.attachVideoButton.isEnabled = !isLoading
        binding.attachAudioButton.isEnabled = !isLoading
        binding.removeAttachmentButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        val errorMessage = when {
            message.contains("15", ignoreCase = true) -> "Размер файла превышает 15 МБ"
            message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение"
            message.contains("403", ignoreCase = true) -> "Необходимо авторизоваться"
            message.contains("415", ignoreCase = true) -> "Неподдерживаемый формат файла"
            else -> "Ошибка: $message"
        }

        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bundle = result.data?.extras
            bundle?.let { handleLocationSelection(it) }
        }
    }

    private fun updateSelectedLocationText() {
        coordinates?.let { coords ->
            binding.selectedLocationText.visibility = View.VISIBLE
            binding.selectedLocationText.text =
                "📍 Координаты: ${String.format("%.6f", coords.lat)}, ${
                    String.format(
                        "%.6f",
                        coords.long
                    )
                }"

            binding.selectedLocationText.setOnClickListener {
                showLocationOptions()
            }
        } ?: run {
            binding.selectedLocationText.visibility = View.GONE
        }
    }

    private fun showLocationOptions() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Локация")
            .setItems(arrayOf("Изменить", "Удалить")) { _, which ->
                when (which) {
                    0 -> openLocationPicker()
                    1 -> removeLocation()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun removeLocation() {
        coordinates = null
        updateSelectedLocationText()
        Snackbar.make(binding.root, "Локация удалена", Snackbar.LENGTH_SHORT).show()
    }
}
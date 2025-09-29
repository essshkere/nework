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
import ru.netology.nework.viewmodel.PostsViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreatePostFragment : Fragment(), MenuProvider {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val postsViewModel: PostsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()

    private var attachmentUri: Uri? = null
    private var attachmentType: Post.Attachment.AttachmentType? = null
    private var coordinates: Post.Coordinates? = null
    private var mentionedUserIds: List<Long> = emptyList()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                attachmentUri = uri
                attachmentType = Post.Attachment.AttachmentType.IMAGE
                showAttachmentPreview(uri)
            }
        }
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                attachmentUri = uri
                attachmentType = Post.Attachment.AttachmentType.VIDEO
                showAttachmentPreview(uri)
            }
        }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                attachmentUri = uri
                attachmentType = Post.Attachment.AttachmentType.AUDIO
                showAudioAttachment()
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
            Snackbar.make(binding.root, "Выбор локации будет реализован позже", Snackbar.LENGTH_SHORT).show()
        }

        binding.selectUsersButton.setOnClickListener {
            Snackbar.make(binding.root, "Выбор пользователей будет реализован позже", Snackbar.LENGTH_SHORT).show()
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
    }

    private fun observePostCreation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
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

    private fun showAttachmentPreview(uri: Uri) {
        binding.attachmentPreview.visibility = View.VISIBLE
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.attachmentPreview)
    }

    private fun showAudioAttachment() {
        binding.attachmentPreview.visibility = View.VISIBLE
        binding.attachmentPreview.setImageResource(R.drawable.ic_audio)
    }

    private fun validateContent(content: String): Boolean {
        return if (content.isBlank()) {
            binding.contentEditText.error = "Текст поста не может быть пустым"
            false
        } else {
            binding.contentEditText.error = null
            true
        }
    }

    private fun createPost() {
        val content = binding.contentEditText.text.toString().trim()

        if (!validateContent(content)) {
            Snackbar.make(binding.root, "Заполните текст поста", Snackbar.LENGTH_SHORT).show()
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            .format(Date())

        val post = Post(
            id = 0,
            authorId = 0,
            author = "",
            content = content,
            published = currentDate,
            coords = coordinates,
            mentionIds = mentionedUserIds,
            attachment = attachmentUri?.let { uri ->
                Post.Attachment(
                    url = uri.toString(),
                    type = attachmentType ?: Post.Attachment.AttachmentType.IMAGE
                )
            }
        )

        postsViewModel.save(post)

        Snackbar.make(binding.root, "Пост создан", Snackbar.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun updateSelectedLocationText() {
        coordinates?.let { coords ->
            binding.selectedLocationText.visibility = View.VISIBLE
            binding.selectedLocationText.text = "Координаты: ${coords.lat}, ${coords.long}"
        } ?: run {
            binding.selectedLocationText.visibility = View.GONE
        }
    }

    private fun updateMentionedUsersText() {
        if (mentionedUserIds.isNotEmpty()) {
            binding.mentionedUsersText.visibility = View.VISIBLE
            binding.mentionedUsersText.text = "Упомянуто пользователей: ${mentionedUserIds.size}"
        } else {
            binding.mentionedUsersText.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
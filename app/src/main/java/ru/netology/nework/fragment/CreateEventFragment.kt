package ru.netology.nework.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import ru.netology.nework.data.Event
import ru.netology.nework.data.User
import ru.netology.nework.databinding.FragmentCreateEventBinding
import ru.netology.nework.viewmodel.EventsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateEventFragment : Fragment(), MenuProvider {
    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!
    private val eventsViewModel: EventsViewModel by viewModels()
    private var attachmentUri: Uri? = null
    private var attachmentType: Event.AttachmentType? = null
    private var coordinates: Event.Coordinates? = null
    private var speakerIds: List<Long> = emptyList()
    private var eventDateTime: Date? = null
    private var eventType: Event.EventType = Event.EventType.ONLINE

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleMediaSelection(uri, Event.AttachmentType.IMAGE)
            }
        }
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleMediaSelection(uri, Event.AttachmentType.VIDEO)
            }
        }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleMediaSelection(uri, Event.AttachmentType.AUDIO)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_create_event, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save -> {
                createEvent()
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
        binding.selectDateTimeButton.setOnClickListener {
            showDateTimePicker()
        }

        binding.selectLocationButton.setOnClickListener {
            openLocationPicker()
        }

        binding.selectSpeakersButton.setOnClickListener {
            openSpeakersPicker()
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

    private fun setupEventType() {
        binding.onlineRadioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                eventType = Event.EventType.ONLINE
                binding.selectLocationButton.visibility = View.GONE
                binding.selectedLocationText.visibility = View.GONE
            }
        }
        binding.offlineRadioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                eventType = Event.EventType.OFFLINE
                binding.selectLocationButton.visibility = View.VISIBLE
            }
        }
        binding.onlineRadioButton.isChecked = true
    }

    private fun setupAttachmentRemoval() {
        binding.attachmentPreview.setOnClickListener {
            showAttachmentOptions()
        }
    }

    private fun observeEventCreation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            }
        }
    }

    private fun handleMediaSelection(uri: Uri, type: Event.AttachmentType) {
        attachmentUri = uri
        attachmentType = type

        when (type) {
            Event.AttachmentType.IMAGE -> showImageAttachmentPreview(uri)
            Event.AttachmentType.VIDEO -> showVideoAttachmentPreview(uri)
            Event.AttachmentType.AUDIO -> showAudioAttachmentPreview()
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
                Event.AttachmentType.IMAGE -> Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                Event.AttachmentType.VIDEO -> Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                Event.AttachmentType.AUDIO -> Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "audio/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                null -> return
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Не найдено приложение для просмотра", Snackbar.LENGTH_SHORT).show()
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

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val currentDate = eventDateTime ?: calendar.time

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val timePicker = TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                        }
                        eventDateTime = selectedCalendar.time
                        updateSelectedDateTimeText()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun updateSelectedDateTimeText() {
        eventDateTime?.let { dateTime ->
            binding.selectedDateTimeText.visibility = View.VISIBLE
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            binding.selectedDateTimeText.text = "📅 Дата и время: ${formatter.format(dateTime)}"
        } ?: run {
            binding.selectedDateTimeText.visibility = View.GONE
        }
    }

    private fun validateContent(content: String): Boolean {
        return if (content.isBlank()) {
            binding.contentEditText.error = "Описание события не может быть пустым"
            false
        } else if (content.length < 10) {
            binding.contentEditText.error = "Описание события должно содержать минимум 10 символов"
            false
        } else {
            binding.contentEditText.error = null
            true
        }
    }

    private fun validateDateTime(): Boolean {
        return if (eventDateTime == null) {
            Snackbar.make(binding.root, "Выберите дату и время события", Snackbar.LENGTH_SHORT).show()
            false
        } else if (eventDateTime!!.before(Date())) {
            Snackbar.make(binding.root, "Дата события не может быть в прошлом", Snackbar.LENGTH_SHORT).show()
            false
        } else {
            true
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        setupTextWatchers()
        setupClickListeners()
        setupEventType()
        observeEventCreation()
        setupAttachmentRemoval()
        setupMapResultListener()
    }

    private fun openSpeakersPicker() {
        val dialog = SelectUsersDialog.newInstance(
            initiallySelectedUserIds = speakerIds.toSet(),
            multiSelect = true
        )

        dialog.onUsersSelected = { selectedUsers ->
            handleSpeakersSelection(selectedUsers)
        }

        dialog.show(parentFragmentManager, SelectUsersDialog.TAG)
    }

    private fun handleSpeakersSelection(selectedUsers: List<ru.netology.nework.data.User>) {
        speakerIds = selectedUsers.map { it.id }
        updateSelectedSpeakersText(selectedUsers)

        // Логирование для отладки
        println("Выбрано спикеров: ${selectedUsers.size}, IDs: $speakerIds")
    }

    private fun updateSelectedSpeakersText(selectedUsers: List<ru.netology.nework.data.User>) {
        if (selectedUsers.isNotEmpty()) {
            binding.selectedSpeakersText.visibility = View.VISIBLE

            val speakersText = when (selectedUsers.size) {
                1 -> "🎤 Спикер: ${selectedUsers.first().name}"
                2, 3, 4 -> "🎤 Спикеры: ${selectedUsers.joinToString { it.name }}"
                else -> "🎤 Спикеров: ${selectedUsers.size}"
            }

            binding.selectedSpeakersText.text = speakersText

            binding.selectedSpeakersText.setOnClickListener {
                showSelectedSpeakersPreview(selectedUsers)
            }
        } else {
            binding.selectedSpeakersText.visibility = View.GONE
        }
    }

    private fun showSelectedSpeakersPreview(selectedUsers: List<ru.netology.nework.data.User>) {
        val speakerNames = selectedUsers.joinToString("\n") { "• ${it.name} (@${it.login})" }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Спикеры события")
            .setMessage(speakerNames)
            .setPositiveButton("Изменить") { _, _ ->
                openSpeakersPicker()
            }
            .setNegativeButton("ОК", null)
            .show()
    }

    private fun createEvent() {
        val content = binding.contentEditText.text.toString().trim()

        if (!validateContent(content) || !validateDateTime()) {
            return
        }

        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uploadedAttachment = attachmentUri?.let { uri ->
                    try {
                        val mediaType = when (attachmentType) {
                            Event.AttachmentType.IMAGE -> Event.AttachmentType.IMAGE
                            Event.AttachmentType.VIDEO -> Event.AttachmentType.VIDEO
                            Event.AttachmentType.AUDIO -> Event.AttachmentType.AUDIO
                            null -> Event.AttachmentType.IMAGE
                        }
                        eventsViewModel.uploadMedia(uri, mediaType)
                    } catch (e: Exception) {
                        showError("Ошибка загрузки медиа: ${e.message}")
                        null
                    }
                }

                val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                    .format(Date())

                val eventDateTimeFormatted = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                    .format(eventDateTime!!)

                val event = Event(
                    id = 0,
                    authorId = 0,
                    author = "",
                    content = content,
                    datetime = eventDateTimeFormatted,
                    published = currentDate,
                    coords = coordinates,
                    type = eventType,
                    speakerIds = speakerIds,
                    attachment = uploadedAttachment
                )

                eventsViewModel.save(event)
                Snackbar.make(binding.root, "Событие создано", Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                showLoading(false)
                showError(e.message ?: "Неизвестная ошибка при создании события")
            }
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

        coordinates = Event.Coordinates(latitude, longitude)
        updateSelectedLocationText()

        Snackbar.make(binding.root, "Локация выбрана", Snackbar.LENGTH_SHORT).show()
    }

    private fun openLocationPicker() {
        val currentCoords = coordinates?.let {
            MapFragment.newInstance(it.lat, it.long)
        } ?: MapFragment.newInstance()

        currentCoords.show(parentFragmentManager, MapFragment.TAG)
    }

    private suspend fun EventsViewModel.uploadMedia(uri: Uri, type: Event.AttachmentType): Event.Attachment? {
        return try {
            val mediaUrl = eventRepository.uploadMedia(uri, type)
            Event.Attachment(mediaUrl, type)
        } catch (e: Exception) {
            null
        }
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
                "📍 Координаты: ${String.format("%.6f", coords.lat)}, ${String.format("%.6f", coords.long)}"

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
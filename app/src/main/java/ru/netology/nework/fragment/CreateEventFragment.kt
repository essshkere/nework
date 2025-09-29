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
import ru.netology.nework.databinding.FragmentCreateEventBinding
import ru.netology.nework.data.Event
import ru.netology.nework.viewmodel.EventsViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateEventFragment : Fragment(), MenuProvider {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private val eventsViewModel: EventsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()

    private var attachmentUri: Uri? = null
    private var attachmentType: Event.Attachment.AttachmentType? = null
    private var coordinates: Event.Coordinates? = null
    private var speakerIds: List<Long> = emptyList()
    private var eventDateTime: Date? = null
    private var eventType: Event.EventType = Event.EventType.ONLINE

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                attachmentUri = uri
                attachmentType = Event.Attachment.AttachmentType.IMAGE
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
                attachmentType = Event.Attachment.AttachmentType.VIDEO
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
                attachmentType = Event.Attachment.AttachmentType.AUDIO
                showAudioAttachment()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        setupTextWatchers()
        setupClickListeners()
        setupEventType()
        observeEventCreation()
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

            Snackbar.make(binding.root, "Выбор локации будет реализован позже", Snackbar.LENGTH_SHORT).show()
        }

        binding.selectSpeakersButton.setOnClickListener {

            Snackbar.make(binding.root, "Выбор спикеров будет реализован позже", Snackbar.LENGTH_SHORT).show()
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

    private fun setupEventType() {
        binding.eventTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            eventType = when (checkedId) {
                R.id.onlineRadioButton -> Event.EventType.ONLINE
                R.id.offlineRadioButton -> Event.EventType.OFFLINE
                else -> Event.EventType.ONLINE
            }
        }


        binding.onlineRadioButton.isChecked = true
    }

    private fun observeEventCreation() {
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

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        eventDateTime = calendar.time
        updateSelectedDateTimeText()

        Snackbar.make(binding.root, "Выбор даты и времени будет реализован позже", Snackbar.LENGTH_SHORT).show()
    }

    private fun validateContent(content: String): Boolean {
        return if (content.isBlank()) {
            binding.contentEditText.error = "Описание события не может быть пустым"
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
        } else {
            true
        }
    }

    private fun createEvent() {
        val content = binding.contentEditText.text.toString().trim()

        if (!validateContent(content) || !validateDateTime()) {
            return
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
            attachment = attachmentUri?.let { uri ->
                Event.Attachment(
                    url = uri.toString(),
                    type = attachmentType ?: Event.Attachment.AttachmentType.IMAGE
                )
            }
        )

        eventsViewModel.save(event)

        Snackbar.make(binding.root, "Событие создано", Snackbar.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun updateSelectedDateTimeText() {
        eventDateTime?.let { dateTime ->
            binding.selectedDateTimeText.visibility = View.VISIBLE
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            binding.selectedDateTimeText.text = "Дата и время: ${formatter.format(dateTime)}"
        } ?: run {
            binding.selectedDateTimeText.visibility = View.GONE
        }
    }

    private fun updateSelectedLocationText() {
        coordinates?.let { coords ->
            binding.selectedLocationText.visibility = View.VISIBLE
            binding.selectedLocationText.text = "Координаты: ${coords.lat}, ${coords.long}"
        } ?: run {
            binding.selectedLocationText.visibility = View.GONE
        }
    }

    private fun updateSelectedSpeakersText() {
        if (speakerIds.isNotEmpty()) {
            binding.selectedSpeakersText.visibility = View.VISIBLE
            binding.selectedSpeakersText.text = "Спикеров: ${speakerIds.size}"
        } else {
            binding.selectedSpeakersText.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
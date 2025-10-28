package ru.netology.nework.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import ru.netology.nework.databinding.FragmentEditEventBinding
import ru.netology.nework.viewmodel.EventsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditEventFragment : Fragment(), MenuProvider {
    private var _binding: FragmentEditEventBinding? = null
    private val binding get() = _binding!!
    private val eventsViewModel: EventsViewModel by viewModels()
    private var attachmentUri: Uri? = null
    private var attachmentType: Event.AttachmentType? = null
    private var coordinates: Event.Coordinates? = null
    private var speakerIds: List<Long> = emptyList()
    private var eventDateTime: Date? = null
    private var eventType: Event.EventType = Event.EventType.ONLINE
    private var currentEvent: Event? = null
    private val eventId: Long by lazy {
        arguments?.getLong("eventId") ?: throw IllegalArgumentException("eventId is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_edit_event, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save -> {
                updateEvent()
                true
            }
            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        loadEventData()
        setupTextWatchers()
        setupClickListeners()
        setupEventType()
        setupAttachmentRemoval()
        observeEventUpdates()
        setupMapResultListener()
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

    private fun loadEventData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                eventsViewModel.getById(eventId)?.let { event ->
                    currentEvent = event
                    bindEventData(event)
                } ?: run {
                    Snackbar.make(binding.root, "Событие не найдено", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun bindEventData(event: Event) {
        binding.contentEditText.setText(event.content)

        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            eventDateTime = inputFormat.parse(event.datetime)
            updateSelectedDateTimeText()
        } catch (e: Exception) {
            eventDateTime = Date()
        }

        eventType = event.type
        when (event.type) {
            Event.EventType.ONLINE -> binding.onlineRadioButton.isChecked = true
            Event.EventType.OFFLINE -> binding.offlineRadioButton.isChecked = true
        }

        coordinates = event.coords
        updateSelectedLocationText()

        speakerIds = event.speakerIds
        updateSelectedSpeakersText(emptyList())

        event.attachment?.let { attachment ->
            binding.attachmentPreview.visibility = View.VISIBLE
            when (attachment.type) {
                Event.AttachmentType.IMAGE -> {
                    Glide.with(this@EditEventFragment)
                        .load(attachment.url)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image)
                        .into(binding.attachmentPreview)
                }
                Event.AttachmentType.VIDEO -> {
                    binding.attachmentPreview.setImageResource(R.drawable.ic_video)
                }
                Event.AttachmentType.AUDIO -> {
                    binding.attachmentPreview.setImageResource(R.drawable.ic_audio)
                }
            }
            binding.removeAttachmentButton.visibility = View.VISIBLE
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

        }

        binding.attachVideoButton.setOnClickListener {

        }

        binding.attachAudioButton.setOnClickListener {

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
    }

    private fun setupAttachmentRemoval() {
        binding.attachmentPreview.setOnClickListener {
            showAttachmentOptions()
        }
    }

    private fun observeEventUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventsViewModel.eventsState.collect { state ->
                    when (state) {
                        is EventsViewModel.EventsState.Success -> {
                            showLoading(false)
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                            eventsViewModel.clearState()
                        }
                        is EventsViewModel.EventsState.Error -> {
                            showLoading(false)
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            eventsViewModel.clearState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val currentDate = eventDateTime ?: calendar.time
        calendar.time = currentDate

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

    private fun openLocationPicker() {
        val currentCoords = coordinates?.let {
            MapFragment.newInstance(it.lat, it.long)
        } ?: MapFragment.newInstance()

        currentCoords.show(parentFragmentManager, MapFragment.TAG)
    }

    private fun openSpeakersPicker() {
        val dialog = SelectUsersDialog.newInstance(
            initiallySelectedUserIds = speakerIds,
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
        } else {
            binding.selectedSpeakersText.visibility = View.GONE
        }
    }

    private fun updateSelectedLocationText() {
        coordinates?.let { coords ->
            binding.selectedLocationText.visibility = View.VISIBLE
            binding.selectedLocationText.text =
                "📍 Координаты: ${String.format("%.6f", coords.lat)}, ${String.format("%.6f", coords.long)}"
        } ?: run {
            binding.selectedLocationText.visibility = View.GONE
        }
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
        currentEvent?.attachment?.let { attachment ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(attachment.url)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Не найдено приложение для просмотра", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeAttachment() {
        attachmentUri = null
        attachmentType = null
        binding.attachmentPreview.visibility = View.GONE
        binding.removeAttachmentButton.visibility = View.GONE

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

    private fun updateEvent() {
        val content = binding.contentEditText.text.toString().trim()

        if (!validateContent(content) || !validateDateTime()) {
            return
        }

        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uploadedAttachment = if (attachmentUri != null && attachmentType != null) {
                    val url = eventsViewModel.uploadMedia(attachmentUri!!, attachmentType!!)
                    Event.Attachment(url, attachmentType!!)
                } else {
                    currentEvent?.attachment
                }

                val eventDateTimeFormatted = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                    .format(eventDateTime!!)

                val updatedEvent = currentEvent!!.copy(
                    content = content,
                    datetime = eventDateTimeFormatted,
                    coords = coordinates,
                    type = eventType,
                    speakerIds = speakerIds,
                    attachment = uploadedAttachment
                )

                eventsViewModel.save(updatedEvent)
            } catch (e: Exception) {
                showLoading(false)
                Snackbar.make(binding.root, "Ошибка при обновлении: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.contentEditText.isEnabled = !loading
        binding.selectDateTimeButton.isEnabled = !loading
        binding.selectLocationButton.isEnabled = !loading
        binding.selectSpeakersButton.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
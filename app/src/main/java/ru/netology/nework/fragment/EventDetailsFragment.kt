package ru.netology.nework.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.ParticipantAdapter
import ru.netology.nework.databinding.FragmentEventDetailsBinding
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.EventsViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class EventDetailsFragment : Fragment() {
    private var _binding: FragmentEventDetailsBinding? = null
    private val binding get() = _binding!!
    private val eventsViewModel: EventsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    @Inject
    lateinit var speakersAdapter: ParticipantAdapter
    @Inject
    lateinit var participantsAdapter: ParticipantAdapter
    private var eventId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventId = arguments?.getLong("eventId") ?: 0

        setupRecyclerViews()
        observeEvent()
        setupClickListeners()
        setupSpeakersClickListeners()
    }

    private fun setupRecyclerViews() {
        binding.speakersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = speakersAdapter
        }

        binding.participantsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = participantsAdapter
        }

        speakersAdapter.onUserClicked = { userId ->
            val bundle = Bundle().apply {
                putLong("userId", userId)
            }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }

        participantsAdapter.onUserClicked = { userId ->
            val bundle = Bundle().apply {
                putLong("userId", userId)
            }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
    }

    private fun observeEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventsViewModel.getById(eventId)?.let { event ->
                    bindEvent(event)
                    loadAuthorJob(event.authorId)
                    loadSpeakers(event.speakerIds)
                    loadParticipants(event.participantsIds)
                }
            }
        }
    }

    private fun navigateToUserProfile(userId: Long) {
        val bundle = Bundle().apply {
            putLong("userId", userId)
        }
        findNavController().navigate(R.id.userProfileFragment, bundle)
    }

    private fun bindEvent(event: ru.netology.nework.data.Event) {
        binding.apply {
            authorNameTextView.text = event.author
            publishedDateTextView.text = formatDate(event.published)
            eventDateTimeTextView.text = "Когда: ${formatDate(event.datetime)}"
            eventTypeTextView.text = when (event.type) {
                ru.netology.nework.data.Event.EventType.ONLINE -> "Онлайн"
                ru.netology.nework.data.Event.EventType.OFFLINE -> "Офлайн"
            }
            contentTextView.text = event.content
            likesCountTextView.text = event.likeOwnerIds.size.toString()

            event.authorAvatar?.let { avatarUrl ->
                Glide.with(authorAvatarImageView)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(authorAvatarImageView)
            } ?: run {
                authorAvatarImageView.setImageResource(R.drawable.ic_account_circle)
            }

            event.attachment?.let { attachment ->
                when (attachment.type) {
                    ru.netology.nework.data.Event.AttachmentType.IMAGE -> {
                        attachmentImageView.visibility = View.VISIBLE
                        Glide.with(attachmentImageView)
                            .load(attachment.url)
                            .centerCrop()
                            .into(attachmentImageView)
                    }
                    ru.netology.nework.data.Event.AttachmentType.VIDEO -> {
                        attachmentImageView.visibility = View.VISIBLE
                        attachmentImageView.setImageResource(R.drawable.ic_video)
                    }
                    ru.netology.nework.data.Event.AttachmentType.AUDIO -> {
                        attachmentImageView.visibility = View.VISIBLE
                        attachmentImageView.setImageResource(R.drawable.ic_audio)
                    }
                }
            } ?: run {
                attachmentImageView.visibility = View.GONE
            }

            event.link?.let { link ->
                linkTextView.visibility = View.VISIBLE
                linkTextView.text = link
                linkTextView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                }
            } ?: run {
                linkTextView.visibility = View.GONE
            }

            event.coords?.let { coords ->
                locationCardView.visibility = View.VISIBLE
                locationTextView.text = "Координаты: ${coords.lat}, ${coords.long}"
            } ?: run {
                locationCardView.visibility = View.GONE
            }

            likeButton.setImageResource(
                if (event.likedByMe) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            likeButton.setOnClickListener {
                if (authViewModel.isAuthenticated()) {
                    eventsViewModel.likeById(event.id)
                } else {
                    findNavController().navigate(R.id.loginFragment)
                }
            }

            participateButton.text = if (event.participatedByMe) "Отказаться" else "Участвовать"
            participateButton.setOnClickListener {
                if (authViewModel.isAuthenticated()) {
                    if (event.participatedByMe) {
                        eventsViewModel.unparticipate(event.id)
                    } else {
                        eventsViewModel.participate(event.id)
                    }
                } else {
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        }
    }

    private fun loadAuthorJob(authorId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.getJobs(authorId).let { jobs ->
                val currentJob = jobs.firstOrNull { it.finish == null }
                binding.authorJobTextView.text = currentJob?.let {
                    "${it.position} в ${it.name}"
                } ?: "В поиске работы"
            }
        }
    }

    private fun loadSpeakers(speakerIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val speakers = speakerIds.mapNotNull { userId ->
                usersViewModel.getUserById(userId)
            }
            speakersAdapter.submitList(speakers)
            binding.speakersCardView.visibility = if (speakerIds.isNotEmpty()) View.VISIBLE else View.GONE
            if (speakerIds.isNotEmpty()) {
                binding.speakersTitle.text = when (speakerIds.size) {
                    1 -> "Спикер (1)"
                    in 2..4 -> "Спикеры (${speakerIds.size})"
                    else -> "Спикеры (${speakerIds.size})"
                }
                if (speakers.size < speakerIds.size) {
                    binding.speakersTitle.text = "${binding.speakersTitle.text} • загружено ${speakers.size}"
                }
            }
        }
    }

    private fun showAllSpeakersDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val event = eventsViewModel.getById(eventId)
            val speakerIds = event?.speakerIds ?: emptyList()
            if (speakerIds.isEmpty()) return@launch

            val speakers = speakerIds.mapNotNull { userId ->
                usersViewModel.getUserById(userId)
            }

            val speakerNames = speakers.joinToString("\n") { "• ${it.name} (@${it.login})" }

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Спикеры события")
                .setMessage(if (speakerNames.isNotEmpty()) speakerNames else "Загрузка спикеров...")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setupSpeakersClickListeners() {
        binding.speakersCardView.setOnClickListener {
            showAllSpeakersDialog()
        }

        speakersAdapter.onUserClicked = { userId ->
            navigateToUserProfile(userId)
        }
    }

    private fun loadParticipants(participantIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val participants = participantIds.mapNotNull { userId ->
                usersViewModel.getUserById(userId)
            }
            participantsAdapter.submitList(participants)
            binding.participantsCardView.visibility = if (participants.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.authorAvatarImageView.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                eventsViewModel.getById(eventId)?.authorId?.let { authorId ->
                    val bundle = Bundle().apply {
                        putLong("userId", authorId)
                    }
                    findNavController().navigate(R.id.userProfileFragment, bundle)
                }
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
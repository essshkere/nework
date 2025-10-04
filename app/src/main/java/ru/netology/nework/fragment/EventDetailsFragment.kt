package ru.netology.nework.fragment

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
import ru.netology.nework.data.Event
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.EventsViewModel
import ru.netology.nework.viewmodel.UsersViewModel
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

             val action = EventDetailsFragmentDirections.actionEventDetailsFragmentToUserProfileFragment(userId)
             findNavController().navigate(action)
        }

        participantsAdapter.onUserClicked = { userId ->

             val action = EventDetailsFragmentDirections.actionEventDetailsFragmentToUserProfileFragment(userId)
             findNavController().navigate(action)
        }
    }

    private fun observeEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val event = eventsViewModel.getById(eventId)
                event?.let {
                    bindEvent(it)
                    loadAuthorJob(it.authorId)
                    loadSpeakers(it.speakerIds)
                    loadParticipants(it.participantsIds)
                }
            }
        }
    }

    private fun bindEvent(event: Event) {
        binding.apply {
            authorNameTextView.text = event.author
            publishedDateTextView.text = event.published
            eventDateTimeTextView.text = "Дата проведения: ${event.datetime}"
            eventTypeTextView.text = when (event.type) {
                Event.EventType.ONLINE -> "Онлайн"
                Event.EventType.OFFLINE -> "Офлайн"
            }
            contentTextView.text = event.content
            likesCountTextView.text = event.likeOwnerIds.size.toString()

            event.authorAvatar?.let { avatarUrl ->
                Glide.with(authorAvatarImageView)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .into(authorAvatarImageView)
            }

            event.attachment?.let { attachment ->
                when (attachment.type) {
                    Event.AttachmentType.IMAGE -> {
                        attachmentImageView.visibility = View.VISIBLE
                        Glide.with(attachmentImageView)
                            .load(attachment.url)
                            .into(attachmentImageView)
                    }
                    else -> {
                        attachmentImageView.visibility = View.GONE
                    }
                }
            } ?: run {
                attachmentImageView.visibility = View.GONE
            }

            event.link?.let { link ->
                linkTextView.visibility = View.VISIBLE
                linkTextView.text = link
                linkTextView.setOnClickListener {
                }
            } ?: run {
                linkTextView.visibility = View.GONE
            }

            event.coords?.let { coords ->
                locationCardView.visibility = View.VISIBLE
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
            }
        }
    }

    private fun loadSpeakers(speakerIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val speakers = speakerIds.mapNotNull { userId ->
                usersViewModel.getUserById(userId)
            }
            speakersAdapter.submitList(speakers)
            binding.speakersCardView.visibility = if (speakers.isNotEmpty()) View.VISIBLE else View.GONE
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
                    val action = EventDetailsFragmentDirections.actionEventDetailsFragmentToUserProfileFragment(authorId)
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
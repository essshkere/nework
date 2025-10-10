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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.EventsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment() {
    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private val eventsViewModel: EventsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    @Inject
    lateinit var eventAdapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observeEvents()
        observeEventsState()
    }

    private fun setupRecyclerView() {
        binding.eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }

        eventAdapter.onEventClicked = { eventId ->
            navigateToEventDetails(eventId)
        }

        eventAdapter.onLikeClicked = { eventId ->
            eventsViewModel.likeById(eventId)
        }

        eventAdapter.onParticipateClicked = { event ->
            handleParticipation(event)
        }

        eventAdapter.onSpeakerClicked = { userId ->
            navigateToUserProfile(userId)
        }

        eventAdapter.onAuthorClicked = { authorId ->
            navigateToUserProfile(authorId)
        }
    }

    private fun handleParticipation(event: ru.netology.nework.data.Event) {
        if (authViewModel.isAuthenticated()) {
            eventsViewModel.toggleParticipation(event)
        } else {
            showAuthenticationRequired()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            eventsViewModel.refresh()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            if (authViewModel.isAuthenticated()) {
                navigateToCreateEvent()
            } else {
                showAuthenticationRequired()
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventsViewModel.data.collect { pagingData ->
                    eventAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun observeEventsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventsViewModel.eventsState.collect { state ->
                    when (state) {
                        is EventsViewModel.EventsState.Loading -> {
                            showLoading(true)
                        }
                        is EventsViewModel.EventsState.Success -> {
                            showLoading(false)
                            showSuccess(state.message)
                            eventsViewModel.clearState()
                        }
                        is EventsViewModel.EventsState.Error -> {
                            showLoading(false)
                            showError(state.message)
                            eventsViewModel.clearState()
                        }
                        EventsViewModel.EventsState.Idle -> {
                            showLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToEventDetails(eventId: Long) {
        val bundle = Bundle().apply {
            putLong("eventId", eventId)
        }
        findNavController().navigate(R.id.eventDetailsFragment, bundle)
    }

    private fun navigateToUserProfile(userId: Long) {
        val bundle = Bundle().apply {
            putLong("userId", userId)
        }
        findNavController().navigate(R.id.userProfileFragment, bundle)
    }

    private fun navigateToCreateEvent() {
        findNavController().navigate(R.id.createEventFragment)
    }

    private fun showAuthenticationRequired() {
        Snackbar.make(binding.root, "Для этого действия необходимо авторизоваться", Snackbar.LENGTH_LONG)
            .setAction("Войти") {
                findNavController().navigate(R.id.loginFragment)
            }
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
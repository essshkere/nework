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

    private val viewModel: EventsViewModel by viewModels()
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
    }

    private fun setupRecyclerView() {
        binding.eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }

        eventAdapter.onEventClicked = { eventId ->
            val bundle = Bundle().apply {
                putLong("eventId", eventId)
            }
            findNavController().navigate(R.id.eventDetailsFragment, bundle)
        }

        eventAdapter.onLikeClicked = { eventId ->
            viewModel.likeById(eventId)
        }

        eventAdapter.onParticipateClicked = { eventId ->
            if (authViewModel.isAuthenticated()) {
                viewModel.participate(eventId)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }

        eventAdapter.onSpeakerClicked = { userId ->
            val bundle = Bundle().apply {
                putLong("userId", userId)
            }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.data.collect {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            if (authViewModel.isAuthenticated()) {
                findNavController().navigate(R.id.createEventFragment)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collect { pagingData ->
                    eventAdapter.submitData(pagingData)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
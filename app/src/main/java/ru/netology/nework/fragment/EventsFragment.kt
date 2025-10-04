package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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

             val action = EventsFragmentDirections.actionEventsFragmentToEventDetailsFragment(eventId)
             findNavController().navigate(action)


            Toast.makeText(requireContext(), "Event ID: $eventId", Toast.LENGTH_SHORT).show()
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
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {

            binding.swipeRefreshLayout.isRefreshing = false

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.data.collectLatest { pagingData ->

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

                 viewModel.data.collectLatest { pagingData ->
                     eventAdapter.submitData(pagingData)
                 }


                viewModel.data.collectLatest {

                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
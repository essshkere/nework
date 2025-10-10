package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
        setupEmptyState()
        observeEvents()
        observeUiState()
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
        eventAdapter.addLoadStateListener { loadState ->
            val isEmpty = eventAdapter.itemCount == 0 &&
                    loadState.source.refresh is androidx.paging.LoadState.NotLoading
            binding.emptyStateLayout.isVisible = isEmpty && !eventsViewModel.uiState.value.isLoading
            binding.eventsRecyclerView.isVisible = !isEmpty
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.design_default_color_primary,
            R.color.design_default_color_primary_variant,
            R.color.design_default_color_secondary
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        // Показываем прогресс при первом запуске
        binding.swipeRefreshLayout.isRefreshing = true
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

    private fun setupEmptyState() {
        binding.emptyStateTextView.text = "Пока нет событий\nСоздайте первое мероприятие!"
        binding.emptyStateButton.setOnClickListener {
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

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventsViewModel.uiState.collect { uiState ->
                    updateUi(uiState)
                }
            }
        }
    }

    private fun observeEventsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventsViewModel.eventsState.collect { state ->
                    when (state) {
                        is EventsViewModel.EventsState.Success -> {
                            showSuccess(state.message)
                            eventsViewModel.clearState()
                        }
                        is EventsViewModel.EventsState.Error -> {
                            showError(state.message)
                            eventsViewModel.clearState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUi(uiState: EventsViewModel.EventsUiState) {
        with(binding) {
            progressBar.isVisible = uiState.isLoading && !uiState.isRefreshing
            swipeRefreshLayout.isRefreshing = uiState.isRefreshing
            eventsRecyclerView.isEnabled = !uiState.isLoading
            fab.isEnabled = !uiState.isLoading
            if (uiState.showError && uiState.error != null) {
                showError(uiState.error)
                eventsViewModel.clearError()
            }
            updateEmptyState(uiState)
            updateOperationState(uiState.currentOperation)
        }
    }

    private fun updateEmptyState(uiState: EventsViewModel.EventsUiState) {
        val isEmpty = eventAdapter.itemCount == 0 && !uiState.isLoading && !uiState.isRefreshing
        binding.emptyStateLayout.isVisible = isEmpty
        binding.eventsRecyclerView.isVisible = !isEmpty || uiState.isLoading
    }

    private fun updateOperationState(operation: String?) {
        operation?.let {
            when (it) {
                "like" -> showShortMessage("Ставим лайк...")
                "dislike" -> showShortMessage("Убираем лайк...")
                "participate" -> showShortMessage("Присоединяемся к событию...")
                "unparticipate" -> showShortMessage("Отказываемся от участия...")
                "create" -> showShortMessage("Создаем событие...")
                "edit" -> showShortMessage("Редактируем событие...")
                "delete" -> showShortMessage("Удаляем событие...")
            }
        }
    }

    private fun refreshData() {
        eventsViewModel.refresh()
        binding.swipeRefreshLayout.postDelayed({
            binding.swipeRefreshLayout.isRefreshing = false
        }, 3000)
    }

    private fun handleParticipation(event: ru.netology.nework.data.Event) {
        if (authViewModel.isAuthenticated()) {
            eventsViewModel.toggleParticipation(event)
        } else {
            showAuthenticationRequired()
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

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Повторить") {
                refreshData()
            }
            .show()

        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showShortMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (eventAdapter.itemCount == 0) {
            refreshData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
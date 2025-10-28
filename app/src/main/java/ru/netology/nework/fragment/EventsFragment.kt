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

        eventAdapter.onAuthorClicked = { authorId ->
            navigateToUserProfile(authorId)
        }

        eventAdapter.onEditClicked = { eventId ->
            navigateToEditEvent(eventId)
        }

        eventAdapter.onDeleteClicked = { event ->
            confirmDeleteEvent(event)
        }

        observeEmptyState()
    }

    private fun showEventMenuOptions(event: ru.netology.nework.data.Event) {
        val isOwnEvent = authViewModel.getUserId() == event.authorId

        val options = mutableListOf<String>()

        if (isOwnEvent) {
            options.add("Редактировать")
            options.add("Удалить")
        } else {
            options.add("Пожаловаться")
            if (event.participatedByMe) {
                options.add("Отказаться от участия")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Опции события")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Редактировать" -> navigateToEditEvent(event.id)
                    "Удалить" -> confirmDeleteEvent(event)
                    "Пожаловаться" -> showReportEventDialog(event)
                    "Отказаться от участия" -> eventsViewModel.unparticipate(event.id)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDeleteEvent(event: ru.netology.nework.data.Event) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Удаление события")
            .setMessage("Вы уверены, что хотите удалить это событие? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                eventsViewModel.removeById(event.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showReportEventDialog(event: ru.netology.nework.data.Event) {
        val reportOptions = arrayOf("Спам", "Несоответствующее содержание", "Неправильная дата/время", "Другое")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Пожаловаться на событие")
            .setItems(reportOptions) { _, which ->
                Snackbar.make(binding.root, "Жалоба отправлена: ${reportOptions[which]}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun navigateToEditEvent(eventId: Long) {
        val bundle = Bundle().apply {
            putLong("eventId", eventId)
        }
        findNavController().navigate(R.id.editEventFragment, bundle)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.purple_700,
            R.color.teal_200
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

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

    private fun observeEmptyState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventsViewModel.uiState.collect { uiState ->
                    updateEmptyStateBasedOnAdapter()
                }
            }
        }
    }

    private fun updateEmptyStateBasedOnAdapter() {
        val isEmpty = eventAdapter.itemCount == 0
        binding.emptyStateLayout.isVisible = isEmpty
        binding.eventsRecyclerView.isVisible = !isEmpty
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
            updateOperationState(uiState.currentOperation)
        }
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
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
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.PostsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PostsFragment : Fragment() {
    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private val postsViewModel: PostsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    @Inject
    lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        setupEmptyState()
        observePosts()
        observeUiState()
        observePostsState()
    }

    private fun setupRecyclerView() {
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }

        postAdapter.onPostClicked = { postId ->
            val bundle = Bundle().apply {
                putLong("postId", postId)
            }
            findNavController().navigate(R.id.postDetailsFragment, bundle)
        }

        postAdapter.onLikeClicked = { postId ->
            postsViewModel.likeById(postId)
        }

        postAdapter.onMentionClicked = { userId ->
            val bundle = Bundle().apply {
                putLong("userId", userId)
            }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
        postAdapter.addLoadStateListener { loadState ->
            val isEmpty = postAdapter.itemCount == 0 &&
                    loadState.source.refresh is androidx.paging.LoadState.NotLoading
            binding.emptyStateLayout.isVisible = isEmpty && !postsViewModel.uiState.value.isLoading
            binding.postsRecyclerView.isVisible = !isEmpty
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
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            if (authViewModel.isAuthenticated()) {
                findNavController().navigate(R.id.createPostFragment)
            } else {
                showAuthenticationRequired()
            }
        }
    }

    private fun setupEmptyState() {
        binding.emptyStateTextView.text = "Пока нет постов\nБудьте первым, кто поделится новостью!"
        binding.emptyStateButton.setOnClickListener {
            if (authViewModel.isAuthenticated()) {
                findNavController().navigate(R.id.createPostFragment)
            } else {
                showAuthenticationRequired()
            }
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postsViewModel.data.collect { pagingData ->
                    postAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postsViewModel.uiState.collect { uiState ->
                    updateUi(uiState)
                }
            }
        }
    }

    private fun observePostsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postsViewModel.postsState.collect { state ->
                    when (state) {
                        is PostsViewModel.PostsState.Success -> {
                            showSuccess(state.message)
                            postsViewModel.clearState()
                        }
                        is PostsViewModel.PostsState.Error -> {
                            showError(state.message)
                            postsViewModel.clearState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUi(uiState: PostsViewModel.PostsUiState) {
        with(binding) {
            progressBar.isVisible = uiState.isLoading && !uiState.isRefreshing
            swipeRefreshLayout.isRefreshing = uiState.isRefreshing
            postsRecyclerView.isEnabled = !uiState.isLoading
            fab.isEnabled = !uiState.isLoading
            if (uiState.showError && uiState.error != null) {
                showError(uiState.error)
                postsViewModel.clearError()
            }
            updateEmptyState(uiState)
            updateOperationState(uiState.currentOperation)
        }
    }

    private fun updateEmptyState(uiState: PostsViewModel.PostsUiState) {
        val isEmpty = postAdapter.itemCount == 0 && !uiState.isLoading && !uiState.isRefreshing
        binding.emptyStateLayout.isVisible = isEmpty
        binding.postsRecyclerView.isVisible = !isEmpty || uiState.isLoading
    }

    private fun updateOperationState(operation: String?) {
        operation?.let {
            when (it) {
                "like" -> showShortMessage("Ставим лайк...")
                "dislike" -> showShortMessage("Убираем лайк...")
                "create" -> showShortMessage("Создаем пост...")
                "edit" -> showShortMessage("Редактируем пост...")
                "delete" -> showShortMessage("Удаляем пост...")
            }
        }
    }

    private fun refreshData() {
        postsViewModel.refresh()
        binding.swipeRefreshLayout.postDelayed({
            binding.swipeRefreshLayout.isRefreshing = false
        }, 3000)
    }

    private fun showAuthenticationRequired() {
        Snackbar.make(binding.root, "Для создания поста необходимо авторизоваться", Snackbar.LENGTH_LONG)
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
        if (postAdapter.itemCount == 0) {
            refreshData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
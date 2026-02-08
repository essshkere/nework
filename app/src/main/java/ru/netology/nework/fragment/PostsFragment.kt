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
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.PostsViewModel

@AndroidEntryPoint
class PostsFragment : Fragment() {
    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private val postsViewModel: PostsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
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

        if (!authViewModel.isAuthenticated()) {
            navigateToLogin()
            return
        }


        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observePosts()
        observeLoadState()
        observeAuthState()
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.loginFragment)
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            currentUserId = authViewModel.getUserId(),
            onEditPost = { post ->
                val bundle = Bundle().apply {
                    putLong("postId", post.id)
                }
                findNavController().navigate(R.id.editPostFragment, bundle)
            },
            onDeletePost = { post ->
                confirmDeletePost(post)
            },
            onReportPost = { post ->
                showReportPostDialog(post)
            }
        ).apply {
            onPostClicked = { postId ->
                navigateToPostDetails(postId)
            }
            onLikeClicked = { postId ->
                postsViewModel.likeById(postId)
            }
            onAuthorClicked = { authorId ->
                navigateToUserProfile(authorId)
            }
            onMentionClicked = { userId ->
                navigateToUserProfile(userId)
            }
        }

        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
            setHasFixedSize(true)
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postsViewModel.data.collectLatest { pagingData ->
                    postAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun handleError(error: Throwable) {
        val errorMessage = when {
            error.message?.contains("403") == true -> {
                Snackbar.make(binding.root, "Сессия истекла. Войдите снова", Snackbar.LENGTH_LONG)
                    .setAction("Войти") {
                        navigateToLogin()
                    }
                    .show()
                "Требуется авторизация"
            }
            error.message?.contains("network", ignoreCase = true) == true -> {
                "Ошибка сети. Проверьте подключение"
            }
            else -> {
                "Ошибка загрузки: ${error.message}"
            }
        }

        if (errorMessage != "Требуется авторизация") {
            Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun navigateToPostDetails(postId: Long) {
        val bundle = Bundle().apply {
            putLong("postId", postId)
        }
        findNavController().navigate(R.id.postDetailsFragment, bundle)
    }

    private fun navigateToUserProfile(userId: Long) {
        val bundle = Bundle().apply {
            putLong("userId", userId)
        }
        findNavController().navigate(R.id.userProfileFragment, bundle)
    }

    private fun confirmDeletePost(post: ru.netology.nework.data.Post) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Удаление поста")
            .setMessage("Вы уверены, что хотите удалить этот пост?")
            .setPositiveButton("Удалить") { _, _ ->
                postsViewModel.removeById(post.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showReportPostDialog(post: ru.netology.nework.data.Post) {
        Snackbar.make(binding.root, "Жалоба отправлена", Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToEditPost(postId: Long) {
        val bundle = Bundle().apply {
            putLong("postId", postId)
        }
        findNavController().navigate(R.id.editPostFragment, bundle)
        binding?.root?.let {
            Snackbar.make(it, "Редактирование поста будет реализовано в следующем обновлении", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            postsViewModel.refresh()
            binding.swipeRefreshLayout.isRefreshing = false
        }
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

    private fun observeLoadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest { loadState ->
                binding.apply {
                    progressBar.isVisible = loadState.refresh is LoadState.Loading
                    swipeRefreshLayout.isRefreshing = loadState.refresh is LoadState.Loading

                    if (loadState.refresh is LoadState.Error) {
                        val error = (loadState.refresh as LoadState.Error).error
                        showError("Ошибка загрузки: ${error.message}")
                    }

                    val isEmpty = loadState.refresh is LoadState.NotLoading &&
                            postAdapter.itemCount == 0
                    emptyStateLayout.isVisible = isEmpty
                    postsRecyclerView.isVisible = !isEmpty

                    if (isEmpty) {
                        emptyStateTextView.text = "Пока нет постов"
                        emptyStateButton?.let {
                            it.text = "Обновить"
                            it.setOnClickListener {
                                postsViewModel.refresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { uiState ->
                    binding.fab.isEnabled = uiState.isAuthenticated
                    if (!uiState.isAuthenticated) {
                        navigateToLogin()
                    }
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
        binding?.let {
            it.swipeRefreshLayout.isRefreshing = uiState.isLoading || uiState.isRefreshing
            it.postsRecyclerView.isEnabled = !uiState.isLoading
            it.fab.isEnabled = !uiState.isLoading
            if (uiState.showError && uiState.error != null) {
                showError(uiState.error)
                postsViewModel.clearError()
            }
            updateEmptyState()
            updateOperationState(uiState.currentOperation)
        }
    }

    private fun updateEmptyState() {
        val isEmpty = postAdapter.itemCount == 0
        if (isEmpty && postsViewModel.uiState.value.isLoading.not()) {
            binding?.root?.let {
                Snackbar.make(it, "Пока нет постов. Будьте первым!", Snackbar.LENGTH_LONG)
                    .setAction("Создать") {
                        if (authViewModel.isAuthenticated()) {
                            findNavController().navigate(R.id.createPostFragment)
                        } else {
                            showAuthenticationRequired()
                        }
                    }
                    .show()
            }
        }
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
        binding?.swipeRefreshLayout?.postDelayed({
            binding?.swipeRefreshLayout?.isRefreshing = false
        }, 3000)
    }

    private fun showAuthenticationRequired() {
        Snackbar.make(binding.root, "Необходимо авторизоваться", Snackbar.LENGTH_LONG)
            .setAction("Войти") {
                findNavController().navigate(R.id.loginFragment)
            }
            .show()
    }

    private fun showSuccess(message: String) {
        binding?.root?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showShortMessage(message: String) {
        binding?.root?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!authViewModel.isAuthenticated()) {
            navigateToLogin()
        } else {
            postsViewModel.refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

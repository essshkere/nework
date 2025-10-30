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
    private val binding get() = _binding
    private val postsViewModel: PostsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postAdapter = PostAdapter(
            currentUserId = authViewModel.getUserId(),
            onEditPost = { post -> navigateToEditPost(post.id) },
            onDeletePost = { post -> confirmDeletePost(post) },
            onReportPost = { post -> showReportPostDialog(post) }
        )

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observePosts()
        observeUiState()
        observePostsState()
    }

    private fun setupRecyclerView() {
        binding?.postsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }

        postAdapter.onPostClicked = { postId ->
            navigateToPostDetails(postId)
        }

        postAdapter.onLikeClicked = { postId ->
            postsViewModel.likeById(postId)
        }

        postAdapter.onMentionClicked = { userId ->
            navigateToUserProfile(userId)
        }

        postAdapter.onAuthorClicked = { authorId ->
            navigateToUserProfile(authorId)
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
            .setMessage("Вы уверены, что хотите удалить этот пост? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                postsViewModel.removeById(post.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showReportPostDialog(post: ru.netology.nework.data.Post) {
        binding?.root?.let {
            Snackbar.make(it, "Жалоба на пост отправлена", Snackbar.LENGTH_SHORT).show()
        }
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
        binding?.swipeRefreshLayout?.setColorSchemeResources(
            R.color.purple_500,
            R.color.purple_700,
            R.color.teal_200
        )

        binding?.swipeRefreshLayout?.setOnRefreshListener {
            refreshData()
        }
        binding?.swipeRefreshLayout?.isRefreshing = true
    }

    private fun setupFab() {
        binding?.fab?.setOnClickListener {
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
                    updateEmptyState()
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
        binding?.root?.let {
            Snackbar.make(it, "Для создания поста необходимо авторизоваться", Snackbar.LENGTH_LONG)
                .setAction("Войти") {
                    findNavController().navigate(R.id.loginFragment)
                }
                .show()
        }
    }

    private fun showSuccess(message: String) {
        binding?.root?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        binding?.root?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG)
                .setAction("Повторить") {
                    refreshData()
                }
                .show()
        }
        binding?.swipeRefreshLayout?.isRefreshing = false
    }

    private fun showShortMessage(message: String) {
        binding?.root?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
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

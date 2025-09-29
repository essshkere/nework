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
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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

    private val viewModel: PostsViewModel by viewModels()
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
        observePosts()
        observeLoadState()
    }

    private fun setupRecyclerView() {
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }

        postAdapter.onPostClicked = { postId ->
            val action = PostsFragmentDirections.actionPostsFragmentToPostDetailsFragment(postId)
            findNavController().navigate(action)
        }

        postAdapter.onLikeClicked = { postId ->
            viewModel.likeById(postId)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            postAdapter.refresh()
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            if (authViewModel.isAuthenticated()) {
                findNavController().navigate(R.id.action_postsFragment_to_createPostFragment)
            } else {

                findNavController().navigate(R.id.action_postsFragment_to_loginFragment)
            }
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest { pagingData ->
                    postAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun observeLoadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postAdapter.loadStateFlow.collectLatest { loadState ->
                    binding.swipeRefresh.isRefreshing = loadState.refresh is LoadState.Loading

                    when (loadState.refresh) {
                        is LoadState.Error -> {

                            val error = (loadState.refresh as LoadState.Error).error
                            Toast.makeText(requireContext(), "Ошибка загрузки: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                        is LoadState.NotLoading -> {

                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
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
            viewModel.likeById(postId)
        }

        postAdapter.onMentionClicked = { userId ->
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
                findNavController().navigate(R.id.createPostFragment)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collect { pagingData ->
                    postAdapter.submitData(pagingData)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
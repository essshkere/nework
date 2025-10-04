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
             val action = PostsFragmentDirections.actionPostsFragmentToPostDetailsFragment(postId)
             findNavController().navigate(action)


            Toast.makeText(requireContext(), "Post ID: $postId", Toast.LENGTH_SHORT).show()
        }

        postAdapter.onLikeClicked = { postId ->
            viewModel.likeById(postId)
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
                findNavController().navigate(R.id.createPostFragment)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                 viewModel.data.collectLatest { pagingData ->
                     postAdapter.submitData(pagingData)
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
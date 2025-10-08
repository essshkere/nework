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
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.JobAdapter
import ru.netology.nework.databinding.FragmentMyProfileBinding
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.JobsViewModel
import ru.netology.nework.viewmodel.UsersViewModel

@AndroidEntryPoint
class MyProfileFragment : Fragment() {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private val jobsViewModel: JobsViewModel by viewModels()

    private lateinit var jobAdapter: JobAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeUser()
        observeJobs()
    }

    private fun setupRecyclerView() {
        jobAdapter = JobAdapter()

        binding.jobsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = jobAdapter
        }
    }

    private fun setupClickListeners() {
        binding.addJobButton.setOnClickListener {
            findNavController().navigate(R.id.createJobFragment)
        }

        binding.editProfileButton.setOnClickListener {
            // TODO: Implement profile editing
        }
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val userId = authViewModel.getUserId()
                if (userId != 0L) {
                    usersViewModel.getUserById(userId)?.let { user ->
                        bindUser(user)
                    }
                }
            }
        }
    }

    private fun observeJobs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                jobsViewModel.jobs.observe(viewLifecycleOwner) { jobs ->
                    jobAdapter.submitList(jobs)
                    binding.jobsEmptyView.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun bindUser(user: ru.netology.nework.data.User) {
        binding.apply {
            userNameTextView.text = user.name
            userLoginTextView.text = user.login

            user.avatar?.let { avatarUrl ->
                Glide.with(avatarImageView)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .into(avatarImageView)
            } ?: run {
                avatarImageView.setImageResource(R.drawable.ic_account_circle)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        jobsViewModel.loadJobs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
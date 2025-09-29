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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.JobAdapter
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.databinding.FragmentUserProfileBinding
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.PostsViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val args: UserProfileFragmentArgs by navArgs()
    private val usersViewModel: UsersViewModel by viewModels()
    private val postsViewModel: PostsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var postAdapter: PostAdapter

    @Inject
    lateinit var jobAdapter: JobAdapter

    private lateinit var viewPagerAdapter: ProfileViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        observeUser()
        setupCurrentJob()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ProfileViewPagerAdapter(this, args.userId)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Стена"
                1 -> "Работы"
                else -> ""
            }
        }.attach()
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.getUserById(args.userId)?.let { user ->
                    bindUser(user)
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
            }
        }
    }

    private fun setupCurrentJob() {
        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.getJobs(args.userId).let { jobs ->
                val currentJob = jobs.firstOrNull { it.finish == null }
                binding.currentJobTextView.text = currentJob?.let {
                    "${it.position} в ${it.name}"
                } ?: "В поиске работы"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class ProfileViewPagerAdapter(
    fragment: Fragment,
    private val userId: Long
) : androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserWallFragment.newInstance(userId)
            1 -> UserJobsFragment.newInstance(userId)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}


class UserWallFragment : Fragment() {

    private var _binding: ru.netology.nework.databinding.FragmentPostsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()

    private var userId: Long = 0

    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: Long): UserWallFragment {
            return UserWallFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getLong(ARG_USER_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ru.netology.nework.databinding.FragmentPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUserWall()
        binding.fab.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())

        }
    }

    private fun observeUserWall() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.getUserWall(userId).let { posts ->

                     postAdapter.submitList(posts)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class UserJobsFragment : Fragment() {

    private var _binding: ru.netology.nework.databinding.FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val usersViewModel: UsersViewModel by viewModels()

    private var userId: Long = 0

    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: Long): UserJobsFragment {
            return UserJobsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getLong(ARG_USER_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ru.netology.nework.databinding.FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUserJobs()
    }

    private fun setupRecyclerView() {
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())

        }
    }

    private fun observeUserJobs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.getJobs(userId).let { jobs ->

                     jobAdapter.submitList(jobs)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
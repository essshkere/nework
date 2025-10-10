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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.JobAdapter
import ru.netology.nework.databinding.FragmentMyProfileBinding
import ru.netology.nework.data.Job
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.JobsViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.Locale

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
        observeJobsState()
    }
    private fun setupRecyclerView() {
        jobAdapter = JobAdapter()

        binding.jobsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = jobAdapter
        }
        jobAdapter.onJobClicked = { job ->
            showJobOptions(job)
        }
    }

    private fun setupClickListeners() {
        binding.addJobButton.setOnClickListener {
            navigateToCreateJob()
        }

        binding.editProfileButton.setOnClickListener {
            showEditProfileOptions()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            jobsViewModel.loadJobs()
            binding.swipeRefreshLayout.isRefreshing = false
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
                    updateJobsEmptyState(jobs)
                    updateCurrentJob(jobs)
                }
            }
        }
    }

    private fun observeJobsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                jobsViewModel.state.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        JobsViewModel.JobsState.Loading -> {
                            showLoading(true)
                        }
                        JobsViewModel.JobsState.Success -> {
                            showLoading(false)
                        }
                        is JobsViewModel.JobsState.Error -> {
                            showLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun bindUser(user: ru.netology.nework.data.User) {
        binding.apply {
            userNameTextView.text = user.name
            userLoginTextView.text = "@${user.login}"

            user.avatar?.let { avatarUrl ->
                com.bumptech.glide.Glide.with(avatarImageView)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .into(avatarImageView)
            } ?: run {
                avatarImageView.setImageResource(R.drawable.ic_account_circle)
            }
        }
    }

    private fun updateJobsEmptyState(jobs: List<Job>) {
        binding.jobsEmptyView.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
        binding.jobsRecyclerView.visibility = if (jobs.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateCurrentJob(jobs: List<Job>) {
        val currentJob = jobs.find { it.finish == null }

        binding.currentJobTextView.text = currentJob?.let { job ->
            formatCurrentJobText(job)
        } ?: "В поиске работы"
    }

    private fun formatCurrentJobText(job: Job): String {
        val startDate = formatDate(job.start)
        return "${job.position} в ${job.name}\nс $startDate"
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun showJobOptions(job: Job) {
        val options = arrayOf("Редактировать", "Удалить")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Работа: ${job.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToEditJob(job.id)
                    1 -> confirmDeleteJob(job)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDeleteJob(job: Job) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Удаление работы")
            .setMessage("Вы уверены, что хотите удалить место работы \"${job.name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteJob(job.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteJob(jobId: Long) {
        jobsViewModel.removeJob(jobId)
    }

    private fun showEditProfileOptions() {
        val options = arrayOf("Изменить аватар", "Изменить имя", "Сменить пароль")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Редактирование профиля")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAvatarChangeOptions()
                    1 -> showNameChangeDialog()
                    2 -> showPasswordChangeDialog()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAvatarChangeOptions() {
        Snackbar.make(binding.root, "Смена аватара будет реализована в следующем обновлении", Snackbar.LENGTH_SHORT).show()
    }

    private fun showNameChangeDialog() {
        Snackbar.make(binding.root, "Смена имени будет реализована в следующем обновлении", Snackbar.LENGTH_SHORT).show()
    }

    private fun showPasswordChangeDialog() {
        Snackbar.make(binding.root, "Смена пароля будет реализована в следующем обновлении", Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToCreateJob() {
        findNavController().navigate(R.id.createJobFragment)
    }

    private fun navigateToEditJob(jobId: Long) {
        val bundle = Bundle().apply {
            putLong("jobId", jobId)
        }
        findNavController().navigate(R.id.editJobFragment, bundle)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.addJobButton.isEnabled = !isLoading
        binding.editProfileButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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
package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentCreateJobBinding
import ru.netology.nework.data.Job
import ru.netology.nework.viewmodel.JobsViewModel

@AndroidEntryPoint
class EditJobFragment : Fragment(), MenuProvider {

    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!

    private val jobsViewModel: JobsViewModel by viewModels()

    private var jobId: Long = 0
    private var currentJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateJobBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        jobId = arguments?.getLong("jobId") ?: 0

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        setupClickListeners()
        observeJob()
        observeJobUpdate()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_edit_job, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save -> {
                updateJob()
                true
            }
            R.id.action_delete -> {
                deleteJob()
                true
            }
            else -> false
        }
    }

    private fun setupClickListeners() {
        binding.startDateEditText.setOnClickListener {
            showDatePicker(true)
        }

        binding.finishDateEditText.setOnClickListener {
            showDatePicker(false)
        }
    }

    private fun observeJob() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                jobsViewModel.jobs.observe(viewLifecycleOwner) { jobs ->
                    currentJob = jobs.find { it.id == jobId }
                    currentJob?.let { job ->
                        bindJob(job)
                    }
                }
            }
        }
    }

    private fun observeJobUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                jobsViewModel.state.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        JobsViewModel.JobsState.Loading -> {
                            showLoading(true)
                        }
                        JobsViewModel.JobsState.Success -> {
                            showLoading(false)
                            Snackbar.make(binding.root, "Работа обновлена", Snackbar.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is JobsViewModel.JobsState.Error -> {
                            showLoading(false)
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindJob(job: Job) {
        binding.apply {
            companyNameEditText.setText(job.name)
            positionEditText.setText(job.position)
            startDateEditText.setText(job.start)
            finishDateEditText.setText(job.finish ?: "")
            linkEditText.setText(job.link ?: "")
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        // TODO: Implement date picker dialog
        Snackbar.make(binding.root, "Выбор даты будет реализован позже", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateJob() {
        val companyName = binding.companyNameEditText.text.toString().trim()
        val position = binding.positionEditText.text.toString().trim()
        val startDate = binding.startDateEditText.text.toString().trim()
        val finishDate = binding.finishDateEditText.text.toString().trim().takeIf { it.isNotBlank() }
        val link = binding.linkEditText.text.toString().trim().takeIf { it.isNotBlank() }

        if (companyName.isBlank() || position.isBlank() || startDate.isBlank()) {
            Snackbar.make(binding.root, "Заполните обязательные поля", Snackbar.LENGTH_SHORT).show()
            return
        }

        val updatedJob = currentJob?.copy(
            name = companyName,
            position = position,
            start = startDate,
            finish = finishDate,
            link = link
        ) ?: return

        jobsViewModel.saveJob(updatedJob)
    }

    private fun deleteJob() {
        currentJob?.let { job ->
            jobsViewModel.removeJob(job.id)
            Snackbar.make(binding.root, "Работа удалена", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
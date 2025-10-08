package ru.netology.nework.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateJobFragment : Fragment(), MenuProvider {

    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!

    private val jobsViewModel: JobsViewModel by viewModels()

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

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        setupTextWatchers()
        setupClickListeners()
        observeJobCreation()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_create_job, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save -> {
                createJob()
                true
            }
            else -> false
        }
    }

    private fun setupTextWatchers() {
        binding.companyNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateCompanyName(s.toString())
            }
        })

        binding.positionEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePosition(s.toString())
            }
        })

        binding.startDateEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateStartDate(s.toString())
            }
        })
    }

    private fun setupClickListeners() {
        binding.startDateEditText.setOnClickListener {
            showDatePicker(true)
        }

        binding.finishDateEditText.setOnClickListener {
            showDatePicker(false)
        }
    }

    private fun observeJobCreation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                jobsViewModel.state.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        JobsViewModel.JobsState.Loading -> {
                            showLoading(true)
                        }
                        JobsViewModel.JobsState.Success -> {
                            showLoading(false)
                            Snackbar.make(binding.root, "Работа добавлена", Snackbar.LENGTH_SHORT).show()
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

    private fun showDatePicker(isStartDate: Boolean) {
        // TODO: Implement date picker dialog
        Snackbar.make(binding.root, "Выбор даты будет реализован позже", Snackbar.LENGTH_SHORT).show()
    }

    private fun validateCompanyName(companyName: String): Boolean {
        return if (companyName.isBlank()) {
            binding.companyNameEditText.error = "Название компании не может быть пустым"
            false
        } else {
            binding.companyNameEditText.error = null
            true
        }
    }

    private fun validatePosition(position: String): Boolean {
        return if (position.isBlank()) {
            binding.positionEditText.error = "Должность не может быть пустой"
            false
        } else {
            binding.positionEditText.error = null
            true
        }
    }

    private fun validateStartDate(startDate: String): Boolean {
        return if (startDate.isBlank()) {
            binding.startDateEditText.error = "Дата начала не может быть пустой"
            false
        } else {
            binding.startDateEditText.error = null
            true
        }
    }

    private fun createJob() {
        val companyName = binding.companyNameEditText.text.toString().trim()
        val position = binding.positionEditText.text.toString().trim()
        val startDate = binding.startDateEditText.text.toString().trim()
        val finishDate = binding.finishDateEditText.text.toString().trim().takeIf { it.isNotBlank() }
        val link = binding.linkEditText.text.toString().trim().takeIf { it.isNotBlank() }

        if (!validateCompanyName(companyName) || !validatePosition(position) || !validateStartDate(startDate)) {
            Snackbar.make(binding.root, "Заполните обязательные поля", Snackbar.LENGTH_SHORT).show()
            return
        }

        val job = Job(
            id = 0,
            name = companyName,
            position = position,
            start = startDate,
            finish = finishDate,
            link = link
        )

        jobsViewModel.saveJob(job)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
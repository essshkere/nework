package ru.netology.nework.fragment

import android.app.DatePickerDialog
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditJobFragment : Fragment(), MenuProvider {
    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!
    private val jobsViewModel: JobsViewModel by viewModels()
    private val calendar = Calendar.getInstance()
    private var startDate: Date? = null
    private var finishDate: Date? = null
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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

        setupTextWatchers()
        setupClickListeners()
        setupDatePickers()
        observeJob()
        observeJobUpdate()
        setupUI()

        if (jobId == 0L) {
            Snackbar.make(binding.root, "Ошибка: работа не найдена", Snackbar.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
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
                confirmDeleteJob()
                true
            }
            else -> false
        }
    }

    private fun setupUI() {
        requireActivity().title = "Редактирование работы"
        binding.progressBar.visibility = View.GONE
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

        binding.linkEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateLink(s.toString())
            }
        })
    }

    private fun setupClickListeners() {
        binding.startDateEditText.setOnClickListener {
            showStartDatePicker()
        }

        binding.startDateLayout.setEndIconOnClickListener {
            showStartDatePicker()
        }

        binding.finishDateEditText.setOnClickListener {
            showFinishDatePicker()
        }

        binding.finishDateLayout.setEndIconOnClickListener {
            showFinishDatePicker()
        }

        binding.currentJobCheckbox.setOnCheckedChangeListener { _, isChecked ->
            handleCurrentJobToggle(isChecked)
        }
    }

    private fun setupDatePickers() {
        calendar.add(Calendar.YEAR, -50)
    }

    private fun observeJob() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                jobsViewModel.jobs.observe(viewLifecycleOwner) { jobs ->
                    currentJob = jobs.find { it.id == jobId }
                    currentJob?.let { job ->
                        bindJob(job)
                    } ?: run {
                        Snackbar.make(binding.root, "Работа не найдена", Snackbar.LENGTH_LONG).show()
                        findNavController().navigateUp()
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
                            Snackbar.make(binding.root, "Работа успешно обновлена", Snackbar.LENGTH_SHORT).show()
                            findNavController().navigateUp()
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

    private fun bindJob(job: Job) {
        binding.apply {
            companyNameEditText.setText(job.name)
            positionEditText.setText(job.position)
            linkEditText.setText(job.link ?: "")

            try {
                startDate = inputDateFormat.parse(job.start)
                startDateEditText.setText(displayDateFormat.format(startDate!!))
            } catch (e: Exception) {
                startDateEditText.setText(job.start)
            }

            if (job.finish != null) {
                try {
                    finishDate = inputDateFormat.parse(job.finish)
                    finishDateEditText.setText(displayDateFormat.format(finishDate!!))
                    currentJobCheckbox.isChecked = false
                } catch (e: Exception) {
                    finishDateEditText.setText(job.finish)
                    currentJobCheckbox.isChecked = false
                }
            } else {
                currentJobCheckbox.isChecked = true
                finishDateEditText.text?.clear()
                binding.finishDateLayout.isEnabled = false
                binding.finishDateEditText.isEnabled = false
            }

            validateCompanyName(job.name)
            validatePosition(job.position)
            validateStartDate()
            validateFinishDate()
            validateLink(job.link ?: "")
        }
    }

    private fun updateJob() {
        if (!validateForm()) {
            return
        }

        val companyName = binding.companyNameEditText.text.toString().trim()
        val position = binding.positionEditText.text.toString().trim()
        val link = binding.linkEditText.text.toString().trim().takeIf { it.isNotBlank() }

        val startDateFormatted = serverDateFormat.format(startDate!!)
        val finishDateFormatted = if (!binding.currentJobCheckbox.isChecked) {
            serverDateFormat.format(finishDate!!)
        } else {
            null
        }

        val updatedJob = currentJob?.copy(
            name = companyName,
            position = position,
            start = startDateFormatted,
            finish = finishDateFormatted,
            link = link
        ) ?: return

        jobsViewModel.saveJob(updatedJob)
    }

    private fun confirmDeleteJob() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Удаление работы")
            .setMessage("Вы уверены, что хотите удалить место работы \"${currentJob?.name}\"? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteJob()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteJob() {
        jobsViewModel.removeJob(jobId)
        Snackbar.make(binding.root, "Работа удалена", Snackbar.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun showStartDatePicker() {
        val currentDate = startDate ?: Date()
        val calendar = Calendar.getInstance().apply { time = currentDate }

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                startDate = selectedDate.time
                binding.startDateEditText.setText(displayDateFormat.format(selectedDate.time))
                validateStartDate()

                finishDate?.let { finish ->
                    if (finish.before(selectedDate.time)) {
                        finishDate = null
                        binding.finishDateEditText.text?.clear()
                        binding.currentJobCheckbox.isChecked = true
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val minCalendar = Calendar.getInstance().apply { add(Calendar.YEAR, -50) }
        val maxCalendar = Calendar.getInstance()

        datePicker.datePicker.minDate = minCalendar.timeInMillis
        datePicker.datePicker.maxDate = maxCalendar.timeInMillis

        datePicker.show()
    }

    private fun showFinishDatePicker() {
        val currentDate = finishDate ?: Date()
        val calendar = Calendar.getInstance().apply { time = currentDate }

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                finishDate = selectedDate.time
                binding.finishDateEditText.setText(displayDateFormat.format(selectedDate.time))
                validateFinishDate()
                binding.currentJobCheckbox.isChecked = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val minCalendar = startDate?.let {
            Calendar.getInstance().apply { time = it }
        } ?: Calendar.getInstance().apply { add(Calendar.YEAR, -50) }

        val maxCalendar = Calendar.getInstance()

        datePicker.datePicker.minDate = minCalendar.timeInMillis
        datePicker.datePicker.maxDate = maxCalendar.timeInMillis

        datePicker.show()
    }

    private fun handleCurrentJobToggle(isChecked: Boolean) {
        if (isChecked) {
            finishDate = null
            binding.finishDateEditText.text?.clear()
            binding.finishDateLayout.isEnabled = false
            binding.finishDateEditText.isEnabled = false
        } else {
            binding.finishDateLayout.isEnabled = true
            binding.finishDateEditText.isEnabled = true
        }
    }

    private fun validateCompanyName(companyName: String): Boolean {
        return when {
            companyName.isBlank() -> {
                binding.companyNameLayout.error = "Название компании не может быть пустым"
                false
            }
            companyName.length < 2 -> {
                binding.companyNameLayout.error = "Название компании должно содержать минимум 2 символа"
                false
            }
            else -> {
                binding.companyNameLayout.error = null
                true
            }
        }
    }

    private fun validatePosition(position: String): Boolean {
        return when {
            position.isBlank() -> {
                binding.positionLayout.error = "Должность не может быть пустой"
                false
            }
            position.length < 2 -> {
                binding.positionLayout.error = "Должность должна содержать минимум 2 символа"
                false
            }
            else -> {
                binding.positionLayout.error = null
                true
            }
        }
    }

    private fun validateStartDate(): Boolean {
        return if (startDate == null) {
            binding.startDateLayout.error = "Укажите дату начала работы"
            false
        } else {
            binding.startDateLayout.error = null
            true
        }
    }

    private fun validateFinishDate(): Boolean {
        if (binding.currentJobCheckbox.isChecked) {
            binding.finishDateLayout.error = null
            return true
        }

        return when {
            finishDate == null -> {
                binding.finishDateLayout.error = "Укажите дату окончания работы"
                false
            }
            finishDate!!.before(startDate) -> {
                binding.finishDateLayout.error = "Дата окончания не может быть раньше даты начала"
                false
            }
            finishDate!!.after(Date()) -> {
                binding.finishDateLayout.error = "Дата окончания не может быть в будущем"
                false
            }
            else -> {
                binding.finishDateLayout.error = null
                true
            }
        }
    }

    private fun validateLink(link: String): Boolean {
        if (link.isBlank()) {
            binding.linkLayout.error = null
            return true
        }

        val urlRegex = Regex("^https?://(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$")

        return when {
            !urlRegex.matches(link) -> {
                binding.linkLayout.error = "Введите корректный URL"
                false
            }
            else -> {
                binding.linkLayout.error = null
                true
            }
        }
    }

    private fun validateForm(): Boolean {
        val companyName = binding.companyNameEditText.text.toString().trim()
        val position = binding.positionEditText.text.toString().trim()
        val link = binding.linkEditText.text.toString().trim()

        val isCompanyNameValid = validateCompanyName(companyName)
        val isPositionValid = validatePosition(position)
        val isStartDateValid = validateStartDate()
        val isFinishDateValid = validateFinishDate()
        val isLinkValid = validateLink(link)

        return isCompanyNameValid && isPositionValid && isStartDateValid && isFinishDateValid && isLinkValid
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.companyNameEditText.isEnabled = !isLoading
        binding.positionEditText.isEnabled = !isLoading
        binding.startDateEditText.isEnabled = !isLoading
        binding.finishDateEditText.isEnabled = !isLoading
        binding.linkEditText.isEnabled = !isLoading
        binding.currentJobCheckbox.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        val errorMessage = when {
            message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение"
            message.contains("403", ignoreCase = true) -> "Необходимо авторизоваться"
            else -> "Ошибка: $message"
        }

        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
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
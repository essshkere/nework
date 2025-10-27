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
class CreateJobFragment : Fragment(), MenuProvider {
    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!
    private val jobsViewModel: JobsViewModel by viewModels()
    private val calendar = Calendar.getInstance()
    private var startDate: Date? = null
    private var finishDate: Date? = null
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        setupDatePickers()
        observeJobCreation()
        setupLinkValidation()
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
        val minDate = calendar.timeInMillis
        calendar.time = Date()
        val maxDate = calendar.timeInMillis
    }

    private fun setupLinkValidation() {
        binding.linkEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateLink(binding.linkEditText.text.toString())
            }
        }
    }

    private fun observeJobCreation() {
        viewLifecycleOwner.lifecycleScope.launch {
            jobsViewModel.state.collect { state ->
                when (state) {
                    JobsViewModel.JobsState.Loading -> showLoading(true)
                    JobsViewModel.JobsState.Success -> {
                        showLoading(false)
                        Snackbar.make(binding.root, "Работа успешно добавлена", Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    is JobsViewModel.JobsState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                    JobsViewModel.JobsState.Idle -> {
                    }
                }
            }
        }
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

                // Если дата окончания раньше даты начала, сбрасываем её
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
            companyName.length > 100 -> {
                binding.companyNameLayout.error = "Название компании не должно превышать 100 символов"
                false
            }
            !companyName.matches(Regex("^[\\p{L}0-9\\s\\-\\.&]+$")) -> {
                binding.companyNameLayout.error = "Название компании содержит недопустимые символы"
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
            position.length > 50 -> {
                binding.positionLayout.error = "Должность не должна превышать 50 символов"
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
            link.length > 200 -> {
                binding.linkLayout.error = "Ссылка не должна превышать 200 символов"
                false
            }
            !urlRegex.matches(link) -> {
                binding.linkLayout.error = "Введите корректный URL (начинается с http:// или https://)"
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
        val isValid = isCompanyNameValid && isPositionValid && isStartDateValid && isFinishDateValid && isLinkValid
        if (!isValid) {
            Snackbar.make(binding.root, "Заполните все обязательные поля правильно", Snackbar.LENGTH_LONG).show()
        }

        return isValid
    }

    private fun createJob() {
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

        val job = Job(
            id = 0,
            name = companyName,
            position = position,
            start = startDateFormatted,
            finish = finishDateFormatted,
            link = link
        )

        jobsViewModel.saveJob(job)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
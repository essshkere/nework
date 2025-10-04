package ru.netology.nework.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import ru.netology.nework.databinding.DialogDatetimePickerBinding
import java.util.Calendar
import java.util.Date

class DateTimePickerDialog : DialogFragment() {

    private var _binding: DialogDatetimePickerBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: Date? = null
    private var minDate: Date? = null

    var onDateTimeSelected: ((Date) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDatetimePickerBinding.inflate(LayoutInflater.from(requireContext()))

        val calendar = Calendar.getInstance()
        selectedDate?.let { date ->
            calendar.time = date
        }


        minDate?.let { date ->
            binding.datePicker.minDate = date.time
        }


        binding.datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            null
        )

        binding.timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
        binding.timePicker.minute = calendar.get(Calendar.MINUTE)

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.confirmButton.setOnClickListener {
            val selectedCalendar = Calendar.getInstance().apply {
                set(
                    binding.datePicker.year,
                    binding.datePicker.month,
                    binding.datePicker.dayOfMonth,
                    binding.timePicker.hour,
                    binding.timePicker.minute,
                    0
                )
            }
            selectedDate = selectedCalendar.time
            onDateTimeSelected?.invoke(selectedCalendar.time)
            dismiss()
        }
    }

    fun setInitialDate(date: Date) {
        selectedDate = date
    }

    fun setMinDate(date: Date) {
        minDate = date
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DateTimePickerDialog"

        fun newInstance(initialDate: Date? = null, minDate: Date? = null): DateTimePickerDialog {
            return DateTimePickerDialog().apply {
                initialDate?.let { setInitialDate(it) }
                minDate?.let { setMinDate(it) }
            }
        }
    }
}
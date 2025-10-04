package ru.netology.nework.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.databinding.DialogConfirmLogoutBinding
import ru.netology.nework.viewmodel.AuthViewModel

@AndroidEntryPoint
class ConfirmLogoutDialog : DialogFragment() {

    private var _binding: DialogConfirmLogoutBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    var onLogoutConfirmed: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogConfirmLogoutBinding.inflate(LayoutInflater.from(requireContext()))

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
            authViewModel.logout()
            onLogoutConfirmed?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ConfirmLogoutDialog"

        fun newInstance(): ConfirmLogoutDialog {
            return ConfirmLogoutDialog()
        }
    }
}
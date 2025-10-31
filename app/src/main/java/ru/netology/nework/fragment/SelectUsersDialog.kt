package ru.netology.nework.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.adapter.ParticipantAdapter
import ru.netology.nework.databinding.DialogSelectUsersBinding
import ru.netology.nework.viewmodel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class SelectUsersDialog : DialogFragment() {
    private var _binding: DialogSelectUsersBinding? = null
    private val binding get() = _binding!!
    private val usersViewModel: UsersViewModel by viewModels()

    @Inject
    lateinit var participantAdapter: ParticipantAdapter

    private var initiallySelectedUserIds: Set<Long> = emptySet()
    private var multiSelect: Boolean = true
    var onUsersSelected: ((List<ru.netology.nework.data.User>) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSelectUsersBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle(getDialogTitle())
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getConfirmButtonText()) { _, _ -> }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                confirmSelection()
            }

            negativeButton.setOnClickListener {
                dismiss()
            }

            updateConfirmButtonState(positiveButton)
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observeUsers()
        updateSelectionInfo()
    }

    private fun setupRecyclerView() {
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = participantAdapter
        }

        participantAdapter.showCheckbox = multiSelect
        participantAdapter.setInitiallySelectedUsers(initiallySelectedUserIds)
        participantAdapter.onUserClicked = { userId ->
            if (!multiSelect) {
                val selectedUser = participantAdapter.currentList.find { it.id == userId }
                selectedUser?.let { user ->
                    onUsersSelected?.invoke(listOf(user))
                    dismiss()
                }
            }
        }
        participantAdapter.onSelectionChanged = { selectedCount ->
            updateSelectionInfo()
            updateConfirmButtonState()
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText ?: "")
                return true
            }
        })
    }

    private fun filterUsers(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.usersFlow.collect { users ->
                val filteredList = if (query.isBlank()) {
                    users
                } else {
                    users.filter { user ->
                        user.name.contains(query, ignoreCase = true) ||
                                user.login.contains(query, ignoreCase = true)
                    }
                }
                participantAdapter.submitList(filteredList)
            }
        }
    }

    private fun observeUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usersViewModel.usersFlow.collect { users ->
                    participantAdapter.submitList(users)
                    updateSelectionInfo()
                    updateConfirmButtonState()
                }
            }
        }
    }

    private fun confirmSelection() {
        val selectedUsers = if (multiSelect) {
            participantAdapter.getSelectedUsers()
        } else {
            val selectedIds = participantAdapter.getSelectedUserIds()
            if (selectedIds.isNotEmpty()) {
                listOf(participantAdapter.currentList.find { it.id == selectedIds.first() }!!)
            } else {
                emptyList()
            }
        }

        if (selectedUsers.isNotEmpty() || multiSelect) {
            onUsersSelected?.invoke(selectedUsers)
            dismiss()
        } else {
            android.widget.Toast.makeText(requireContext(), "Выберите пользователя", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectionInfo() {
        val selectedCount = participantAdapter.getSelectedUserIds().size

        if (multiSelect) {
             binding.selectionInfoTextView.visibility = View.VISIBLE
             binding.selectionInfoTextView.text = when (selectedCount) {
                 0 -> "Пользователи не выбраны"
                 1 -> "Выбран 1 пользователь"
                 else -> "Выбрано пользователей: $selectedCount"
             }
        } else {
             binding.selectionInfoTextView.visibility = View.GONE
        }
    }

    private fun updateConfirmButtonState(button: Button? = null) {
        val selectedCount = participantAdapter.getSelectedUserIds().size
        val confirmButton = button ?: (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)

        confirmButton?.isEnabled = if (multiSelect) {
            true
        } else {
            selectedCount > 0
        }
    }

    private fun getDialogTitle(): String {
        return if (multiSelect) "Выберите пользователей" else "Выберите пользователя"
    }

    private fun getConfirmButtonText(): String {
        return if (multiSelect) "Выбрать" else "Выбрать пользователя"
    }

    fun setInitiallySelectedUserIds(userIds: List<Long>) {
        initiallySelectedUserIds = userIds.toSet()
    }

    fun setMultiSelect(enabled: Boolean) {
        multiSelect = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectUsersDialog"

        fun newInstance(
            initiallySelectedUserIds: List<Long> = emptyList(),
            multiSelect: Boolean = true
        ): SelectUsersDialog {
            return SelectUsersDialog().apply {
                setInitiallySelectedUserIds(initiallySelectedUserIds)
                setMultiSelect(multiSelect)
            }
        }
    }
}
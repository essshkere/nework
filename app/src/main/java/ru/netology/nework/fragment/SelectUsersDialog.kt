package ru.netology.nework.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.adapter.ParticipantAdapter
import ru.netology.nework.databinding.DialogSelectUsersBinding
import ru.netology.nework.data.User
import ru.netology.nework.viewmodel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class SelectUsersDialog : DialogFragment() {

    private var _binding: DialogSelectUsersBinding? = null
    private val binding get() = _binding!!

    private val usersViewModel: UsersViewModel by viewModels()

    @Inject
    lateinit var participantAdapter: ParticipantAdapter

    private var initiallySelectedUserIds: List<Long> = emptyList()
    private var multiSelect: Boolean = true

    var onUsersSelected: ((List<User>) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSelectUsersBinding.inflate(LayoutInflater.from(requireContext()))

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupClickListeners()
        observeUsers()
    }

    private fun setupRecyclerView() {
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = participantAdapter
        }

        participantAdapter.showCheckbox = multiSelect
        initiallySelectedUserIds.forEach { userId ->
            participantAdapter.selectedUserIds.add(userId)
        }

        if (!multiSelect) {
            participantAdapter.onUserClicked = { userId ->
                val selectedUser = participantAdapter.currentList.find { it.id == userId }
                selectedUser?.let {
                    onUsersSelected?.invoke(listOf(it))
                    dismiss()
                }
            }
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { query ->
                    filterUsers(query)
                }
                return true
            }
        })
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.confirmButton.setOnClickListener {
            if (multiSelect) {
                val selectedUsers = participantAdapter.currentList.filter { user ->
                    participantAdapter.selectedUserIds.contains(user.id)
                }
                onUsersSelected?.invoke(selectedUsers)
            }
            dismiss()
        }
    }

    private fun observeUsers() {
        usersViewModel.users.observe(viewLifecycleOwner) { users ->
            participantAdapter.submitList(users)
        }
    }

    private fun filterUsers(query: String) {
        val filteredList = if (query.isBlank()) {
            usersViewModel.users.value ?: emptyList()
        } else {
            (usersViewModel.users.value ?: emptyList()).filter { user ->
                user.name.contains(query, ignoreCase = true) ||
                        user.login.contains(query, ignoreCase = true)
            }
        }
        participantAdapter.submitList(filteredList)
    }

    fun setInitiallySelectedUserIds(userIds: List<Long>) {
        initiallySelectedUserIds = userIds
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
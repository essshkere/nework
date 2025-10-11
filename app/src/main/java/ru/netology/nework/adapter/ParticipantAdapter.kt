package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.databinding.ItemParticipantBinding
import ru.netology.nework.data.User

class ParticipantAdapter : ListAdapter<User, ParticipantAdapter.ViewHolder>(DiffCallback) {

    var onUserClicked: ((Long) -> Unit)? = null
    var onSelectionChanged: ((Int) -> Unit)? = null
    var showCheckbox: Boolean = false
    private val selectedUserIds = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                userNameTextView.text = user.name
                userLoginTextView.text = "@${user.login}"

                user.avatar?.let { avatarUrl ->
                    Glide.with(avatarImageView)
                        .load(avatarUrl)
                        .placeholder(ru.netology.nework.R.drawable.ic_account_circle)
                        .circleCrop()
                        .into(avatarImageView)
                } ?: run {
                    avatarImageView.setImageResource(ru.netology.nework.R.drawable.ic_account_circle)
                }

                if (showCheckbox) {
                    checkbox.visibility = View.VISIBLE
                    checkbox.isChecked = selectedUserIds.contains(user.id)

                    checkbox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedUserIds.add(user.id)
                        } else {
                            selectedUserIds.remove(user.id)
                        }
                        onSelectionChanged?.invoke(selectedUserIds.size)
                    }

                    root.setOnClickListener {
                        checkbox.isChecked = !checkbox.isChecked
                    }
                } else {
                    checkbox.visibility = View.GONE
                    root.setOnClickListener {
                        onUserClicked?.invoke(user.id)
                    }
                }
            }
        }
    }

    fun getSelectedUsers(): List<User> {
        return currentList.filter { user ->
            selectedUserIds.contains(user.id)
        }
    }

    fun getSelectedUserIds(): List<Long> {
        return selectedUserIds.toList()
    }

    fun setInitiallySelectedUsers(userIds: Set<Long>) {
        selectedUserIds.clear()
        selectedUserIds.addAll(userIds)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedUserIds.clear()
        onSelectionChanged?.invoke(0)
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedUserIds.clear()
        selectedUserIds.addAll(currentList.map { it.id })
        onSelectionChanged?.invoke(selectedUserIds.size)
        notifyDataSetChanged()
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }
}
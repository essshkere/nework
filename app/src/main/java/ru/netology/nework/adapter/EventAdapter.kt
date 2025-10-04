package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.databinding.ItemEventBinding
import ru.netology.nework.data.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter : ListAdapter<Event, EventAdapter.ViewHolder>(DiffCallback) {

    var onEventClicked: ((Long) -> Unit)? = null
    var onLikeClicked: ((Long) -> Unit)? = null
    var onParticipateClicked: ((Long) -> Unit)? = null
    var onSpeakerClicked: ((Long) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                authorNameTextView.text = event.author
                publishedDateTextView.text = formatDate(event.published)
                eventDateTimeTextView.text = "Когда: ${formatDate(event.datetime)}"
                eventTypeTextView.text = when (event.type) {
                    Event.EventType.ONLINE -> "Онлайн"
                    Event.EventType.OFFLINE -> "Офлайн"
                }
                contentTextView.text = event.content
                likesCountTextView.text = event.likeOwnerIds.size.toString()
                participantsCountTextView.text = "Участников: ${event.participantsIds.size}"

                event.authorAvatar?.let { avatarUrl ->
                    Glide.with(authorAvatarImageView)
                        .load(avatarUrl)
                        .placeholder(ru.netology.nework.R.drawable.ic_account_circle)
                        .circleCrop()
                        .into(authorAvatarImageView)
                } ?: run {
                    authorAvatarImageView.setImageResource(ru.netology.nework.R.drawable.ic_account_circle)
                }

                event.attachment?.let { attachment ->
                    when (attachment.type) {
                        Event.AttachmentType.IMAGE -> {
                            attachmentImageView.visibility = android.view.View.VISIBLE
                            Glide.with(attachmentImageView)
                                .load(attachment.url)
                                .centerCrop()
                                .into(attachmentImageView)
                        }
                        Event.AttachmentType.VIDEO -> {
                            attachmentImageView.visibility = android.view.View.VISIBLE
                            attachmentImageView.setImageResource(ru.netology.nework.R.drawable.ic_video)
                        }
                        Event.AttachmentType.AUDIO -> {
                            attachmentImageView.visibility = android.view.View.VISIBLE
                            attachmentImageView.setImageResource(ru.netology.nework.R.drawable.ic_audio)
                        }
                    }
                } ?: run {
                    attachmentImageView.visibility = android.view.View.GONE
                }

                val likeIcon = if (event.likedByMe) {
                    ru.netology.nework.R.drawable.ic_favorite
                } else {
                    ru.netology.nework.R.drawable.ic_favorite_border
                }
                likeButton.setImageResource(likeIcon)

                participateButton.text = if (event.participatedByMe) "Отказаться" else "Участвовать"

                root.setOnClickListener {
                    onEventClicked?.invoke(event.id)
                }

                likeButton.setOnClickListener {
                    onLikeClicked?.invoke(event.id)
                }

                participateButton.setOnClickListener {
                    onParticipateClicked?.invoke(event.id)
                }

                authorAvatarImageView.setOnClickListener {
                    // TODO: Navigate to user profile
                }

                menuButton.visibility = android.view.View.GONE
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString) ?: Date()
                outputFormat.format(date)
            } catch (e: Exception) {
                dateString
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: Event, newItem: Event): Any? {
                return when {
                    oldItem.likedByMe != newItem.likedByMe ||
                            oldItem.likeOwnerIds.size != newItem.likeOwnerIds.size -> "likes_changed"
                    oldItem.participatedByMe != newItem.participatedByMe ||
                            oldItem.participantsIds.size != newItem.participantsIds.size -> "participation_changed"
                    else -> null
                }
            }
        }
    }
}
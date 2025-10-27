package ru.netology.nework.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.data.Event
import ru.netology.nework.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter : ListAdapter<Event, EventAdapter.ViewHolder>(DiffCallback) {

    var onEventClicked: ((Long) -> Unit)? = null
    var onLikeClicked: ((Long) -> Unit)? = null
    var onParticipateClicked: ((Event) -> Unit)? = null
    var onAuthorClicked: ((Long) -> Unit)? = null
    var onEditClicked: ((Long) -> Unit)? = null
    var onDeleteClicked: ((Event) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) = with(binding) {
            authorNameTextView.text = event.author
            publishedDateTextView.text = formatDate(event.published)
            eventDateTimeTextView.text = "📅 ${formatDate(event.datetime)}"
            eventTypeTextView.text =
                if (event.type == Event.EventType.ONLINE) "🌐 Онлайн" else "📍 Офлайн"
            contentTextView.text = event.content
            likesCountTextView.text = event.likeOwnerIds.size.toString()
            participantsCountTextView.text = "👥 ${event.participantsIds.size}"

            if (event.authorAvatar != null) {
                Glide.with(authorAvatarImageView)
                    .load(event.authorAvatar)
                    .placeholder(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(authorAvatarImageView)
            } else {
                authorAvatarImageView.setImageResource(R.drawable.ic_account_circle)
            }

            // Вложение (картинка, видео, аудио)
            when (event.attachment?.type) {
                Event.AttachmentType.IMAGE -> {
                    attachmentImageView.visibility = View.VISIBLE
                    Glide.with(attachmentImageView)
                        .load(event.attachment.url)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image)
                        .into(attachmentImageView)
                }
                Event.AttachmentType.VIDEO -> {
                    attachmentImageView.visibility = View.VISIBLE
                    attachmentImageView.setImageResource(R.drawable.ic_video)
                }
                Event.AttachmentType.AUDIO -> {
                    attachmentImageView.visibility = View.VISIBLE
                    attachmentImageView.setImageResource(R.drawable.ic_audio)
                }
                else -> attachmentImageView.visibility = View.GONE
            }

            if (event.link != null) {
                linkTextView.visibility = View.VISIBLE
                linkTextView.text = "🔗 ${event.link}"
            } else {
                linkTextView.visibility = View.GONE
            }

            val likeIcon = if (event.likedByMe) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            likeButton.setImageResource(likeIcon)

            participateButton.text = if (event.participatedByMe) "❌ Отказаться" else "✅ Участвовать"
            val participateColor =
                if (event.participatedByMe) R.color.participation_active else R.color.participation_inactive
            participateButton.setBackgroundColor(participateButton.context.getColor(participateColor))

            root.setOnClickListener { onEventClicked?.invoke(event.id) }
            authorAvatarImageView.setOnClickListener { onAuthorClicked?.invoke(event.authorId) }
            likeButton.setOnClickListener { onLikeClicked?.invoke(event.id) }
            participateButton.setOnClickListener { onParticipateClicked?.invoke(event) }

            menuButton.setOnClickListener { showMenu(event) }
        }

        private fun showMenu(event: Event) {
            val options = mutableListOf<String>().apply {
                add("Редактировать")
                add("Удалить")
                add("Поделиться")
                add("Добавить в календарь")
            }

            AlertDialog.Builder(binding.root.context)
                .setTitle("Опции события")
                .setItems(options.toTypedArray()) { _, which ->
                    when (options[which]) {
                        "Редактировать" -> onEditClicked?.invoke(event.id)
                        "Удалить" -> onDeleteClicked?.invoke(event)
                        "Поделиться" -> shareEvent(event)
                        "Добавить в календарь" -> addToCalendar(event)
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        private fun shareEvent(event: Event) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, "Событие: ${event.content.take(100)}...")
                type = "text/plain"
            }
            binding.root.context.startActivity(
                Intent.createChooser(shareIntent, "Поделиться событием")
            )
        }

        private fun addToCalendar(event: Event) {
            Toast.makeText(binding.root.context, "Добавление в календарь в разработке", Toast.LENGTH_SHORT).show()
        }

        private fun formatDate(date: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val output = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val parsed = input.parse(date)
                output.format(parsed ?: Date())
            } catch (e: Exception) {
                date
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
        }
    }
}

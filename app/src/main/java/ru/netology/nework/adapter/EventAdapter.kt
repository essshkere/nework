package ru.netology.nework.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.data.Event
import ru.netology.nework.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter : ListAdapter<Event, EventAdapter.ViewHolder>(DiffCallback) {

    var onEventClicked: ((Long) -> Unit)? = null
    var onLikeClicked: ((Long) -> Unit)? = null
    var onParticipateClicked: ((Event) -> Unit)? = null
    var onSpeakerClicked: ((Long) -> Unit)? = null
    var onAuthorClicked: ((Long) -> Unit)? = null
    var onParticipantClicked: ((Long) -> Unit)? = null
    var onMenuClicked: ((Event) -> Unit)? = null

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
                eventDateTimeTextView.text = "📅 ${formatDate(event.datetime)}"
                eventTypeTextView.text = when (event.type) {
                    Event.EventType.ONLINE -> "🌐 Онлайн"
                    Event.EventType.OFFLINE -> "📍 Офлайн"
                }
                contentTextView.text = event.content
                likesCountTextView.text = event.likeOwnerIds.size.toString()
                participantsCountTextView.text = "👥 ${event.participantsIds.size}"

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
                                .placeholder(ru.netology.nework.R.drawable.ic_image)
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

                event.link?.let { link ->
                    linkTextView.visibility = android.view.View.VISIBLE
                    linkTextView.text = "🔗 $link"
                } ?: run {
                    linkTextView.visibility = android.view.View.GONE
                }

                val likeIcon = if (event.likedByMe) {
                    ru.netology.nework.R.drawable.ic_favorite_filled
                } else {
                    ru.netology.nework.R.drawable.ic_favorite_border
                }
                likeButton.setImageResource(likeIcon)

                participateButton.text = if (event.participatedByMe) {
                    "❌ Отказаться"
                } else {
                    "✅ Участвовать"
                }

                participateButton.setBackgroundColor(
                    if (event.participatedByMe) {
                        participateButton.context.getColor(ru.netology.nework.R.color.participation_active)
                    } else {
                        participateButton.context.getColor(ru.netology.nework.R.color.participation_inactive)
                    }
                )

                setupClickListeners(event)
                menuButton.visibility = android.view.View.VISIBLE
                menuButton.setOnClickListener {
                    onMenuClicked?.invoke(event)
                }
                private fun showEventMenuOptions(event: Event) {
                    val isOwnEvent = authViewModel.getUserId() == event.authorId

                    val options = mutableListOf<String>()

                    if (isOwnEvent) {
                        options.add("Редактировать")
                        options.add("Удалить")
                        options.add("Управление участниками")
                    } else {
                        options.add("Пожаловаться")
                        if (event.participatedByMe) {
                            options.add("Отказаться от участия")
                        } else {
                            options.add("Участвовать")
                        }
                    }

                    options.add("Поделиться")
                    options.add("Добавить в календарь")

                    androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                        .setTitle("Опции события")
                        .setItems(options.toTypedArray()) { _, which ->
                            when (options[which]) {
                                "Редактировать" -> navigateToEditEvent(event.id)
                                "Удалить" -> confirmDeleteEvent(event)
                                "Управление участниками" -> manageEventParticipants(event)
                                "Пожаловаться" -> showReportEventDialog(event)
                                "Отказаться от участия" -> eventsViewModel.unparticipate(event.id)
                                "Участвовать" -> eventsViewModel.participate(event.id)
                                "Поделиться" -> shareEvent(event)
                                "Добавить в календарь" -> addToCalendar(event)
                            }
                        }
                        .setNegativeButton("Отмена", null)
                        .show()

                    private fun navigateToEditEvent(eventId: Long) {
                        onEditEventClicked?.invoke(eventId)
                    }

                    private fun manageEventParticipants(event: Event) {
                        androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                            .setTitle("Участники события")
                            .setMessage("Участников: ${event.participantsIds.size}\nСпикеров: ${event.speakerIds.size}")
                            .setPositiveButton("Подробнее") { dialog, _ ->
                                onEventClicked?.invoke(event.id)
                                dialog.dismiss()
                            }
                            .setNegativeButton("OK", null)
                            .show()
                    }

                    private fun shareEvent(event: Event) {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Присоединяйтесь к событию: ${event.content.take(100)}...")
                            type = "text/plain"
                        }
                        binding.root.context.startActivity(Intent.createChooser(shareIntent, "Поделиться событием"))
                    }

                    private fun addToCalendar(event: Event) {
                        Toast.makeText(binding.root.context, "Функция добавления в календарь в разработке", Toast.LENGTH_SHORT).show()
                    }

                    var onEditEventClicked: ((Long) -> Unit)? = null

                }
            }
        }

        private fun setupClickListeners(event: Event) {
            binding.apply {
                root.setOnClickListener {
                    onEventClicked?.invoke(event.id)
                }

                likeButton.setOnClickListener {
                    onLikeClicked?.invoke(event.id)
                }

                participateButton.setOnClickListener {
                    onParticipateClicked?.invoke(event)
                }

                authorAvatarImageView.setOnClickListener {
                    onAuthorClicked?.invoke(event.authorId)
                }

                if (event.speakerIds.isNotEmpty()) {
                    speakersTextView.visibility = android.view.View.VISIBLE
                    speakersTextView.text = "🎤 Спикеров: ${event.speakerIds.size}"
                    speakersTextView.setOnClickListener {
                        event.speakerIds.firstOrNull()?.let { speakerId ->
                            onSpeakerClicked?.invoke(speakerId)
                        }
                    }
                } else {
                    speakersTextView.visibility = android.view.View.GONE
                }

                if (event.participantsIds.isNotEmpty()) {
                    participantsTextView.visibility = android.view.View.VISIBLE
                    participantsTextView.text = "👥 Участников: ${event.participantsIds.size}"
                    participantsTextView.setOnClickListener {
                        event.participantsIds.firstOrNull()?.let { participantId ->
                            onParticipantClicked?.invoke(participantId)
                        }
                    }
                } else {
                    participantsTextView.visibility = android.view.View.GONE
                }
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
                            oldItem.likeOwnerIds.size != newItem.likeOwnerIds.size ->
                        "likes_changed"

                    oldItem.participatedByMe != newItem.participatedByMe ||
                            oldItem.participantsIds.size != newItem.participantsIds.size ->
                        "participation_changed"

                    oldItem.speakerIds != newItem.speakerIds ->
                        "speakers_changed"

                    else -> null
                }
            }
        }
    }
}
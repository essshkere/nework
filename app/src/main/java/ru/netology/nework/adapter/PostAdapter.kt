package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.databinding.ItemPostBinding
import ru.netology.nework.data.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter : ListAdapter<Post, PostAdapter.ViewHolder>(DiffCallback) {

    var onPostClicked: ((Long) -> Unit)? = null
    var onLikeClicked: ((Long) -> Unit)? = null
    var onMentionClicked: ((Long) -> Unit)? = null
    var onAuthorClicked: ((Long) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.apply {
                authorNameTextView.text = post.author
                publishedDateTextView.text = formatDate(post.published)
                contentTextView.text = post.content
                likesCountTextView.text = post.likeOwnerIds.size.toString()

                post.authorAvatar?.let { avatarUrl ->
                    Glide.with(authorAvatarImageView)
                        .load(avatarUrl)
                        .placeholder(ru.netology.nework.R.drawable.ic_account_circle)
                        .circleCrop()
                        .into(authorAvatarImageView)
                } ?: run {
                    authorAvatarImageView.setImageResource(ru.netology.nework.R.drawable.ic_account_circle)
                }

                post.attachment?.let { attachment ->
                    when (attachment.type) {
                        Post.AttachmentType.IMAGE -> {
                            attachmentImageView.visibility = android.view.View.VISIBLE
                            Glide.with(attachmentImageView)
                                .load(attachment.url)
                                .centerCrop()
                                .into(attachmentImageView)
                        }
                        Post.AttachmentType.VIDEO -> {
                            attachmentImageView.visibility = android.view.View.VISIBLE
                            attachmentImageView.setImageResource(ru.netology.nework.R.drawable.ic_video)
                        }
                        Post.AttachmentType.AUDIO -> {
                            attachmentImageView.visibility = android.view.View.VISIBLE
                            attachmentImageView.setImageResource(ru.netology.nework.R.drawable.ic_audio)
                        }
                    }
                } ?: run {
                    attachmentImageView.visibility = android.view.View.GONE
                }

                post.link?.let { link ->
                    linkTextView.visibility = android.view.View.VISIBLE
                    linkTextView.text = link
                } ?: run {
                    linkTextView.visibility = android.view.View.GONE
                }

                val mentionIds = post.mentionIds
                if (mentionIds.isNotEmpty()) {
                    mentionedUsersTextView.visibility = android.view.View.VISIBLE
                    val mentionedUsersText = if (mentionIds.size == 1) {
                        "Упомянут 1 пользователь"
                    } else {
                        "Упомянуто ${mentionIds.size} пользователей"
                    }
                    mentionedUsersTextView.text = mentionedUsersText
                } else {
                    mentionedUsersTextView.visibility = android.view.View.GONE
                }

                val likeIcon = if (post.likedByMe) {
                    ru.netology.nework.R.drawable.ic_favorite
                } else {
                    ru.netology.nework.R.drawable.ic_favorite_border
                }
                likeButton.setImageResource(likeIcon)

                root.setOnClickListener {
                    onPostClicked?.invoke(post.id)
                }

                likeButton.setOnClickListener {
                    onLikeClicked?.invoke(post.id)
                }

                mentionedUsersTextView.setOnClickListener {
                    if (mentionIds.isNotEmpty()) {
                        onMentionClicked?.invoke(mentionIds.first())
                    }
                }

                authorAvatarImageView.setOnClickListener {
                    onAuthorClicked?.invoke(post.authorId)
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
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
                return if (oldItem.likedByMe != newItem.likedByMe ||
                    oldItem.likeOwnerIds.size != newItem.likeOwnerIds.size) {
                    "likes_changed"
                } else {
                    null
                }
            }
        }
    }
}
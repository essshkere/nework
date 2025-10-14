package ru.netology.nework.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    var onMenuClicked: ((Post) -> Unit)? = null

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
                    mentionedUsersTextView.visibility = View.VISIBLE
                    val mentionedUsersText = when (mentionIds.size) {
                        1 -> "Упомянут 1 пользователь"
                        in 2..4 -> "Упомянуто ${mentionIds.size} пользователя"
                        else -> "Упомянуто ${mentionIds.size} пользователей"
                    }
                    mentionedUsersTextView.text = mentionedUsersText

                    mentionedUsersTextView.setOnClickListener {
                        showMentionedUsersPreview(post.mentionIds)
                    }
                } else {
                    mentionedUsersTextView.visibility = View.GONE
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

                menuButton.visibility = android.view.View.VISIBLE
                menuButton.setOnClickListener {
                    onMenuClicked?.invoke(post)
                }
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString) ?: Date()
                outputFormat.format(date)
            } catch (e: Exception) {
                dateString
            }
        }

        private fun showMentionedUsersPreview(mentionIds: List<Long>) {
            val context = binding.root.context
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Упомянутые пользователи")
                .setMessage("Упомянуто пользователей: ${mentionIds.size}\nID: ${mentionIds.joinToString()}")
                .setPositiveButton("Подробнее") { dialog, _ ->
                    onMentionClicked?.invoke(mentionIds.firstOrNull() ?: 0)
                    dialog.dismiss()
                }
                .setNegativeButton("OK", null)
                .show()
        }


        private fun showPostMenuOptions(post: Post) {
            val isOwnPost = authViewModel.getUserId() == post.authorId

            val options = mutableListOf<String>()

            if (isOwnPost) {
                options.add("Редактировать")
                options.add("Удалить")
            } else {
                options.add("Пожаловаться")
            }

            options.add("Поделиться")
            options.add("Скопировать ссылку")

            androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                .setTitle("Опции поста")
                .setItems(options.toTypedArray()) { _, which ->
                    when (options[which]) {
                        "Редактировать" -> navigateToEditPost(post.id)
                        "Удалить" -> confirmDeletePost(post)
                        "Пожаловаться" -> showReportPostDialog(post)
                        "Поделиться" -> sharePost(post)
                        "Скопировать ссылку" -> copyPostLink(post)
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        private fun navigateToEditPost(postId: Long) {

            onEditPostClicked?.invoke(postId)
        }

        private fun sharePost(post: Post) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Посмотрите этот пост: ${post.content.take(100)}...")
                type = "text/plain"
            }
            binding.root.context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Поделиться постом"
                )
            )
        }

        private fun copyPostLink(post: Post) {
            val clipboard =
                binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("post_link", "https://nework/post/${post.id}")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(binding.root.context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        }

        var onEditPostClicked: ((Long) -> Unit)? = null
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
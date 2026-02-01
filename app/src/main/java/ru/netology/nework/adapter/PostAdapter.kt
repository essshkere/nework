package ru.netology.nework.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.data.Post
import ru.netology.nework.databinding.ItemPostBinding
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    private val currentUserId: Long,
    private val onEditPost: (Post) -> Unit,
    private val onDeletePost: (Post) -> Unit,
    private val onReportPost: (Post) -> Unit
) : PagingDataAdapter<Post, PostAdapter.ViewHolder>(DiffCallback) {

    var onPostClicked: ((Long) -> Unit)? = null
    var onLikeClicked: ((Long) -> Unit)? = null
    var onMentionClicked: ((Long) -> Unit)? = null
    var onAuthorClicked: ((Long) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = getItem(position)
        post?.let {
            holder.bind(it)
        } ?: run {
            holder.bindEmpty()
        }
    }


    inner class ViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) = with(binding) {
            authorNameTextView.text = post.author
            publishedDateTextView.text = formatDate(post.published)
            contentTextView.text = post.content
            likesCountTextView.text = post.likeOwnerIds.size.toString()

            if (post.authorAvatar != null) {
                Glide.with(authorAvatarImageView)
                    .load(post.authorAvatar)
                    .placeholder(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(authorAvatarImageView)
            } else {
                authorAvatarImageView.setImageResource(R.drawable.ic_account_circle)
            }

            when (post.attachment?.type) {
                Post.AttachmentType.IMAGE -> {
                    attachmentImageView.visibility = View.VISIBLE
                    Glide.with(attachmentImageView)
                        .load(post.attachment.url)
                        .centerCrop()
                        .into(attachmentImageView)
                }
                Post.AttachmentType.VIDEO -> {
                    attachmentImageView.visibility = View.VISIBLE
                    attachmentImageView.setImageResource(R.drawable.ic_video)
                }
                Post.AttachmentType.AUDIO -> {
                    attachmentImageView.visibility = View.VISIBLE
                    attachmentImageView.setImageResource(R.drawable.ic_audio)
                }
                else -> attachmentImageView.visibility = View.GONE
            }

            if (post.link != null) {
                linkTextView.visibility = View.VISIBLE
                linkTextView.text = "🔗 ${post.link}"
            } else {
                linkTextView.visibility = View.GONE
            }

            val mentionIds = post.mentionIds
            if (mentionIds.isNotEmpty()) {
                mentionedUsersTextView.visibility = View.VISIBLE
                mentionedUsersTextView.text = when (mentionIds.size) {
                    1 -> "Упомянут 1 пользователь"
                    in 2..4 -> "Упомянуто ${mentionIds.size} пользователя"
                    else -> "Упомянуто ${mentionIds.size} пользователей"
                }
                mentionedUsersTextView.setOnClickListener {
                    onMentionClicked?.invoke(mentionIds.first())
                }
            } else {
                mentionedUsersTextView.visibility = View.GONE
            }

            val likeIcon = if (post.likedByMe) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            likeButton.setImageResource(likeIcon)

            root.setOnClickListener { onPostClicked?.invoke(post.id) }
            likeButton.setOnClickListener { onLikeClicked?.invoke(post.id) }
            authorAvatarImageView.setOnClickListener { onAuthorClicked?.invoke(post.authorId) }

            menuButton.visibility = View.VISIBLE
            menuButton.setOnClickListener { showPostMenuOptions(post) }
        }
        fun bindEmpty() {
            with(binding) {
                authorNameTextView.text = ""
                publishedDateTextView.text = ""
                contentTextView.text = "Загрузка..."
                likesCountTextView.text = ""
                authorAvatarImageView.setImageResource(R.drawable.ic_account_circle)
                attachmentImageView.visibility = View.GONE
                linkTextView.visibility = View.GONE
                mentionedUsersTextView.visibility = View.GONE
                menuButton.visibility = View.GONE
                likeButton.setOnClickListener(null)
                root.setOnClickListener(null)
            }
        }

        private fun showPostMenuOptions(post: Post) {
            val isOwnPost = currentUserId == post.authorId
            val options = mutableListOf<String>()

            if (isOwnPost) {
                options.add("Редактировать")
                options.add("Удалить")
            } else {
                options.add("Пожаловаться")
            }

            options.add("Поделиться")
            options.add("Скопировать ссылку")

            AlertDialog.Builder(binding.root.context)
                .setTitle("Опции поста")
                .setItems(options.toTypedArray()) { _, which ->
                    when (options[which]) {
                        "Редактировать" -> onEditPost(post)
                        "Удалить" -> onDeletePost(post)
                        "Пожаловаться" -> onReportPost(post)
                        "Поделиться" -> sharePost(post)
                        "Скопировать ссылку" -> copyPostLink(post)
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        private fun copyPostLink(post: Post) {
            val clipboard =
                binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("post_link", "https://nework/post/${post.id}")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(binding.root.context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        }

        private fun sharePost(post: Post) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Посмотрите этот пост: ${post.content.take(100)}...")
                type = "text/plain"
            }
            binding.root.context.startActivity(
                Intent.createChooser(shareIntent, "Поделиться постом")
            )
        }

        private fun formatDate(dateString: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val output = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = input.parse(dateString)
                output.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
        }
    }
}

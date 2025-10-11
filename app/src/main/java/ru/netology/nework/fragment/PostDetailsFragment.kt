package ru.netology.nework.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.ParticipantAdapter
import ru.netology.nework.databinding.FragmentPostDetailsBinding
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.PostsViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    private val postsViewModel: PostsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var participantAdapter: ParticipantAdapter

    private var postId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getLong("postId") ?: 0

        setupRecyclerView()
        observePost()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.mentionedUsersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = participantAdapter
        }

        participantAdapter.onUserClicked = { userId ->
            val bundle = Bundle().apply {
                putLong("userId", userId)
            }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
    }

    private fun observePost() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postsViewModel.getPostById(postId)?.let { post ->
                    bindPost(post)
                    loadAuthorJob(post.authorId)
                    loadMentionedUsers(post.mentionIds)
                }
            }
        }
    }

    private fun bindPost(post: ru.netology.nework.data.Post) {
        binding.apply {
            authorNameTextView.text = post.author
            publishedDateTextView.text = formatDate(post.published)
            contentTextView.text = post.content
            likesCountTextView.text = post.likeOwnerIds.size.toString()

            post.authorAvatar?.let { avatarUrl ->
                Glide.with(authorAvatarImageView)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .into(authorAvatarImageView)
            } ?: run {
                authorAvatarImageView.setImageResource(R.drawable.ic_account_circle)
            }

            post.attachment?.let { attachment ->
                when (attachment.type) {
                    ru.netology.nework.data.Post.AttachmentType.IMAGE -> {
                        attachmentImageView.visibility = View.VISIBLE
                        Glide.with(attachmentImageView)
                            .load(attachment.url)
                            .centerCrop()
                            .into(attachmentImageView)
                    }
                    ru.netology.nework.data.Post.AttachmentType.VIDEO -> {
                        attachmentImageView.visibility = View.VISIBLE
                        attachmentImageView.setImageResource(R.drawable.ic_video)
                    }
                    ru.netology.nework.data.Post.AttachmentType.AUDIO -> {
                        attachmentImageView.visibility = View.VISIBLE
                        attachmentImageView.setImageResource(R.drawable.ic_audio)
                    }
                }
            } ?: run {
                attachmentImageView.visibility = View.GONE
            }

            post.link?.let { link ->
                linkTextView.visibility = View.VISIBLE
                linkTextView.text = link
                linkTextView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                }
            } ?: run {
                linkTextView.visibility = View.GONE
            }

            post.coords?.let { coords ->
                locationCardView.visibility = View.VISIBLE
                locationTextView.text = "Координаты: ${coords.lat}, ${coords.long}"
            } ?: run {
                locationCardView.visibility = View.GONE
            }

            likeButton.setImageResource(
                if (post.likedByMe) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            likeButton.setOnClickListener {
                if (authViewModel.isAuthenticated()) {
                    postsViewModel.likeById(post.id)
                } else {
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        }
    }

    private fun loadAuthorJob(authorId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.getJobs(authorId).let { jobs ->
                val currentJob = jobs.firstOrNull { it.finish == null }
                binding.apply {
                    authorJobCardView.visibility = if (currentJob != null) View.VISIBLE else View.GONE
                    authorJobTextView.text = currentJob?.let {
                        "${it.position} в ${it.name}"
                    } ?: "В поиске работы"
                }
            }
        }
    }

    private fun loadMentionedUsers(mentionIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val mentionedUsers = mentionIds.mapNotNull { userId ->
                usersViewModel.getUserById(userId)
            }
            participantAdapter.submitList(mentionedUsers)
            binding.mentionedUsersCardView.visibility = if (mentionedUsers.isNotEmpty()) View.VISIBLE else View.GONE

            if (mentionedUsers.isNotEmpty()) {
                binding.mentionedUsersTitle.text = when (mentionedUsers.size) {
                    1 -> "Упомянут 1 пользователь"
                    else -> "Упомянуто ${mentionedUsers.size} пользователей"
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.authorAvatarImageView.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                postsViewModel.getPostById(postId)?.authorId?.let { authorId ->
                    val bundle = Bundle().apply {
                        putLong("userId", authorId)
                    }
                    findNavController().navigate(R.id.userProfileFragment, bundle)
                }
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
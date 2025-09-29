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
import androidx.navigation.fragment.navArgs
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
import javax.inject.Inject

@AndroidEntryPoint
class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: PostDetailsFragmentArgs by navArgs()
    private val postsViewModel: PostsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var participantAdapter: ParticipantAdapter

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
            val action = PostDetailsFragmentDirections.actionPostDetailsFragmentToUserProfileFragment(userId)
            findNavController().navigate(action)
        }
    }

    private fun observePost() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postsViewModel.getPostById(args.postId)?.let { post ->
                    bindPost(post)
                    loadAuthorJob(post.authorId)
                    loadMentionedUsers(post.mentionIds ?: emptyList())
                }
            }
        }
    }

    private fun bindPost(post: ru.netology.nework.data.Post) {
        binding.apply {
            authorNameTextView.text = post.author
            publishedDateTextView.text = post.published
            contentTextView.text = post.content
            likesCountTextView.text = post.likeOwnerIds.size.toString()


            post.authorAvatar?.let { avatarUrl ->
                Glide.with(authorAvatarImageView)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .into(authorAvatarImageView)
            }


            post.attachment?.let { attachment ->
                when (attachment.type) {
                    ru.netology.nework.data.Post.Attachment.AttachmentType.IMAGE -> {
                        attachmentImageView.visibility = View.VISIBLE
                        Glide.with(attachmentImageView)
                            .load(attachment.url)
                            .into(attachmentImageView)
                    }
                    else -> {
                        attachmentImageView.visibility = View.GONE
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
                    findNavController().navigate(R.id.action_postDetailsFragment_to_loginFragment)
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
        }
    }

    private fun setupClickListeners() {
        binding.authorAvatarImageView.setOnClickListener {
            args.postId.let { postId ->
                postsViewModel.getPostById(postId)?.authorId?.let { authorId ->
                    val action = PostDetailsFragmentDirections.actionPostDetailsFragmentToUserProfileFragment(authorId)
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
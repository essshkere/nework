package ru.netology.nework.adapter

import androidx.recyclerview.widget.ListAdapter
import ru.netology.nework.dto.PostDto

class PostAdapter(
    private val onLikeClicked: (Long) -> Unit,
    private val onPostClicked: (Long) -> Unit
) : ListAdapter<PostDto, PostAdapter.ViewHolder>(DiffCallback) {

}
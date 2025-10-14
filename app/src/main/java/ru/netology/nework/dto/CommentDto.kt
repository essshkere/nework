package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class CommentDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("postId")
    val postId: Long,

    @SerializedName("authorId")
    val authorId: Long,

    @SerializedName("author")
    val author: String,

    @SerializedName("authorAvatar")
    val authorAvatar: String? = null,

    @SerializedName("content")
    val content: String,

    @SerializedName("published")
    val published: String,

    @SerializedName("likeOwnerIds")
    val likeOwnerIds: List<Long> = emptyList(),

    @SerializedName("likedByMe")
    val likedByMe: Boolean = false
)
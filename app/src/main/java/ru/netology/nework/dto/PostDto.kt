package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class PostDto(
    @SerializedName("id") val id: Long,
    @SerializedName("authorId") val authorId: Long,
    @SerializedName("author") val author: String,
    @SerializedName("authorJob") val authorJob: String? = null,
    @SerializedName("authorAvatar") val authorAvatar: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("published") val published: String,
    @SerializedName("coords") val coords: CoordinatesDto? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("mentionIds") val mentionIds: List<Long> = emptyList(),
    @SerializedName("mentionedMe") val mentionedMe: Boolean = false,
    @SerializedName("likeOwnerIds") val likeOwnerIds: List<Long> = emptyList(),
    @SerializedName("likedByMe") val likedByMe: Boolean = false,
    @SerializedName("attachment") val attachment: AttachmentDto? = null,
    @SerializedName("users") val users: Map<Long, UserPreviewDto> = emptyMap()
)

data class CoordinatesDto(
    @SerializedName("lat") val lat: String,
    @SerializedName("long") val long: String
)
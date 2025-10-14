package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class EventDto(
    @SerializedName("id") val id: Long,
    @SerializedName("authorId") val authorId: Long,
    @SerializedName("author") val author: String,
    @SerializedName("authorJob") val authorJob: String? = null,
    @SerializedName("authorAvatar") val authorAvatar: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("published") val published: String,
    @SerializedName("coords") val coords: CoordinatesDto? = null,
    @SerializedName("type") val type: EventTypeDto,
    @SerializedName("likeOwnerIds") val likeOwnerIds: List<Long> = emptyList(),
    @SerializedName("likedByMe") val likedByMe: Boolean = false,
    @SerializedName("speakerIds") val speakerIds: List<Long> = emptyList(),
    @SerializedName("participantsIds") val participantsIds: List<Long> = emptyList(),
    @SerializedName("participatedByMe") val participatedByMe: Boolean = false,
    @SerializedName("attachment") val attachment: AttachmentDto? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("users") val users: Map<Long, UserPreviewDto> = emptyMap()
)

enum class EventTypeDto {
    @SerializedName("ONLINE") ONLINE,
    @SerializedName("OFFLINE") OFFLINE
}
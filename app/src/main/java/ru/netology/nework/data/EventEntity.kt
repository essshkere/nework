package ru.netology.nework.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val authorJob: String?,
    val content: String,
    val datetime: String,
    val published: String,
    val coords: String?,
    val type: String,
    val likeOwnerIds: String,
    val likedByMe: Boolean,
    val speakerIds: String,
    val participantsIds: String,
    val participatedByMe: Boolean,
    val attachmentUrl: String?,
    val attachmentType: String?,
    val link: String?
)
package ru.netology.nework.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val authorJob: String?,
    val content: String,
    val published: String,
    val coords: String?,
    val link: String?,
    val likeOwnerIds: String,
    val likedByMe: Boolean,
    val attachmentUrl: String?,
    val attachmentType: String?,
    val mentionIds: String
)
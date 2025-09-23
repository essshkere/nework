package ru.netology.nework.mapper

import com.google.gson.Gson
import ru.netology.nework.data.Post
import ru.netology.nework.data.PostEntity
import ru.netology.nework.dto.PostDto

fun PostDto.toEntity(): PostEntity = PostEntity(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    published = published,
    coords = coords?.let { Gson().toJson(it) },
    link = link,
    likeOwnerIds = Gson().toJson(likeOwnerIds),
    likedByMe = likedByMe,
    attachmentUrl = attachment?.url,
    attachmentType = attachment?.type?.name,
    mentionIds = Gson().toJson(mentionIds)
)

fun PostEntity.toModel(): Post = Post(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    published = published,
    coords = coords?.split(",")?.let {
        Post.Coordinates(it[0].toDouble(), it[1].toDouble())
    },
    link = link,
    likeOwnerIds = likeOwnerIds.split(",").map { it.toLong() },
    likedByMe = likedByMe,
    attachment = attachmentUrl?.let {
        Post.Attachment(it, Post.AttachmentType.valueOf(attachmentType ?: "IMAGE"))
    }
)

fun Post.toDto(): PostDto = PostDto(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    published = published,
    coords = coords?.let {
        PostDto.CoordinatesDto(it.lat.toString(), it.long.toString())
    },
    link = link,
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    attachment = attachment?.let {
        PostDto.AttachmentDto(it.url, PostDto.AttachmentTypeDto.valueOf(it.type.name))
    }
)
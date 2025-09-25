package ru.netology.nework.mapper

import com.google.gson.Gson
import ru.netology.nework.data.Post
import ru.netology.nework.data.PostEntity
import ru.netology.nework.dto.AttachmentDto
import ru.netology.nework.dto.AttachmentTypeDto
import ru.netology.nework.dto.CoordinatesDto
import ru.netology.nework.dto.PostDto

private val gson = Gson()

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
    coords = coords?.let {
        try {
            val coordsDto = gson.fromJson(it, CoordinatesDto::class.java)
            Post.Coordinates(coordsDto.lat.toDouble(), coordsDto.long.toDouble())
        } catch (e: Exception) {
            null
        }
    },
    link = link,
    likeOwnerIds = try {
        gson.fromJson(likeOwnerIds, Array<Long>::class.java).toList()
    } catch (e: Exception) {
        emptyList()
    },
    likedByMe = likedByMe,
    attachment = attachmentUrl?.let {
        Post.Attachment(it, Post.AttachmentType.valueOf(attachmentType ?: "IMAGE"))
    }
)

fun PostEntity.toDto(): PostDto = PostDto(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    published = published,
    coords = try {
        gson.fromJson(coords, CoordinatesDto::class.java)
    } catch (e: Exception) { null },
    link = link,
    mentionIds = try {
        gson.fromJson(mentionIds, Array<Long>::class.java).toList()
    } catch (e: Exception) { emptyList() },
    mentionedMe = false,
    likeOwnerIds = try {
        gson.fromJson(likeOwnerIds, Array<Long>::class.java).toList()
    } catch (e: Exception) { emptyList() },
    likedByMe = likedByMe,
    attachment = attachmentUrl?.let { url ->
        AttachmentDto(
            url = url,
            type = AttachmentTypeDto.valueOf(attachmentType ?: "IMAGE")
        )
    },
    users = emptyMap()
)
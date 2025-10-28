package ru.netology.nework.mapper

import com.google.gson.Gson
import ru.netology.nework.data.Post
import ru.netology.nework.data.PostEntity
import ru.netology.nework.dto.AttachmentDto
import ru.netology.nework.dto.AttachmentTypeDto
import ru.netology.nework.dto.CoordinatesDto
import ru.netology.nework.dto.PostDto
import ru.netology.nework.dto.UserPreviewDto

private val gson = Gson()

fun PostDto.toEntity(): PostEntity = PostEntity(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    published = published,
    coords = coords?.let { gson.toJson(it) },
    link = link,
    likeOwnerIds = gson.toJson(likeOwnerIds),
    likedByMe = likedByMe,
    attachmentUrl = attachment?.url,
    attachmentType = attachment?.type?.name,
    mentionIds = gson.toJson(mentionIds),
    users = gson.toJson(users)
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
        val coordsDto = gson.fromJson(it, CoordinatesDto::class.java)
        Post.Coordinates(coordsDto.lat.toDouble(), coordsDto.long.toDouble())
    },
    link = link,
    likeOwnerIds = try {
        gson.fromJson(likeOwnerIds, Array<Long>::class.java).toList()
    } catch (e: Exception) {
        emptyList()
    },
    likedByMe = likedByMe,
    attachment = attachmentUrl?.let { url ->
        Post.Attachment(
            url = url,
            type = Post.AttachmentType.valueOf(attachmentType ?: "IMAGE")
        )
    },
    mentionIds = try {
        gson.fromJson(mentionIds, Array<Long>::class.java).toList()
    } catch (e: Exception) {
        emptyList()
    },
    mentionedMe = false,
    users = try {
        gson.fromJson(users, Map::class.java) as? Map<Long, Post.UserPreview> ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
)
fun PostDto.toModel(): Post = Post(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    published = published,
    coords = coords?.let { Post.Coordinates(it.lat, it.long) },
    link = link,
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    attachment = attachment?.let {
        Post.Attachment(
            url = it.url,
            type = when (it.type) {
                AttachmentTypeDto.IMAGE -> Post.AttachmentType.IMAGE
                AttachmentTypeDto.VIDEO -> Post.AttachmentType.VIDEO
                AttachmentTypeDto.AUDIO -> Post.AttachmentType.AUDIO
            }
        )
    },
    mentionIds = mentionIds,
    mentionedMe = mentionedMe,
    users = users.mapValues { (_, userPreviewDto) ->
        Post.UserPreview(
            name = userPreviewDto.name,
            avatar = userPreviewDto.avatar
        )
    }
)

fun Post.toDto(): PostDto {
    return PostDto(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        authorJob = authorJob,
        content = content,
        published = published,
        coords = coords?.let {
            CoordinatesDto(
                lat = it.lat,
                long = it.long
            )
        },
        link = link,
        mentionIds = mentionIds,
        mentionedMe = mentionedMe,
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        attachment = attachment?.let {
            AttachmentDto(
                it.url,
                when (it.type) {
                    Post.AttachmentType.IMAGE -> AttachmentTypeDto.IMAGE
                    Post.AttachmentType.VIDEO -> AttachmentTypeDto.VIDEO
                    Post.AttachmentType.AUDIO -> AttachmentTypeDto.AUDIO
                }
            )
        },
        users = users.mapValues { (_, userPreview) ->
            UserPreviewDto(
                name = userPreview.name,
                avatar = userPreview.avatar
            )
        }
    )
}
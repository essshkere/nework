package ru.netology.nework.mapper

import com.google.gson.Gson
import ru.netology.nework.data.Event
import ru.netology.nework.data.EventEntity
import ru.netology.nework.dto.AttachmentDto
import ru.netology.nework.dto.AttachmentTypeDto
import ru.netology.nework.dto.CoordinatesDto
import ru.netology.nework.dto.EventDto
import ru.netology.nework.dto.EventTypeDto

private val gson = Gson()

fun EventDto.toEntity(): EventEntity = EventEntity(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    datetime = datetime,
    published = published,
    coords = coords?.let { gson.toJson(it) },
    type = type.name,
    likeOwnerIds = gson.toJson(likeOwnerIds),
    likedByMe = likedByMe,
    speakerIds = gson.toJson(speakerIds),
    participantsIds = gson.toJson(participantsIds),
    participatedByMe = participatedByMe,
    attachmentUrl = attachment?.url,
    attachmentType = attachment?.type?.name,
    link = link,
    users = gson.toJson(users)
)

fun EventEntity.toModel(): Event = Event(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    datetime = datetime,
    published = published,
    coords = coords?.let {
        val coordsDto = gson.fromJson(it, CoordinatesDto::class.java)
        Event.Coordinates(coordsDto.lat.toDouble(), coordsDto.long.toDouble())
    },
    type = Event.EventType.valueOf(type),
    likeOwnerIds = try {
        gson.fromJson(likeOwnerIds, Array<Long>::class.java).toList()
    } catch (e: Exception) { emptyList() },
    likedByMe = likedByMe,
    speakerIds = try {
        gson.fromJson(speakerIds, Array<Long>::class.java).toList()
    } catch (e: Exception) { emptyList() },
    participantsIds = try {
        gson.fromJson(participantsIds, Array<Long>::class.java).toList()
    } catch (e: Exception) { emptyList() },
    participatedByMe = participatedByMe,
    attachment = attachmentUrl?.let { url ->
        Event.Attachment(
            url = url,
            type = Event.AttachmentType.valueOf(attachmentType ?: "IMAGE")
        )
    },
    link = link,
    users = try {
        gson.fromJson(users, Map::class.java) as? Map<Long, Event.UserPreview> ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
)

fun Event.toDto(): EventDto = EventDto(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    datetime = datetime,
    published = published,
    coords = coords?.let { CoordinatesDto(it.lat.toString(), it.long.toString()) },
    type = EventTypeDto.valueOf(type.name),
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    speakerIds = speakerIds,
    participantsIds = participantsIds,
    participatedByMe = participatedByMe,
    attachment = attachment?.let {
        AttachmentDto(it.url, AttachmentTypeDto.valueOf(it.type.name))
    },
    link = link,
    users = users.mapValues { (_, userPreview) ->
        ru.netology.nework.dto.UserPreviewDto(
            name = userPreview.name,
            avatar = userPreview.avatar
        )
    }
)
package ru.netology.nework.mapper

import com.google.gson.Gson
import ru.netology.nework.data.Event
import ru.netology.nework.data.EventEntity
import ru.netology.nework.dto.AttachmentDto
import ru.netology.nework.dto.AttachmentTypeDto
import ru.netology.nework.dto.CoordinatesDto
import ru.netology.nework.dto.EventDto
import ru.netology.nework.dto.EventTypeDto
import ru.netology.nework.dto.UserPreviewDto

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
    link = link
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
    link = link
)

fun EventDto.toModel(): Event = Event(
    id = id,
    authorId = authorId,
    author = author,
    authorAvatar = authorAvatar,
    authorJob = authorJob,
    content = content,
    datetime = datetime,
    published = published,
    coords = coords?.let { Event.Coordinates(it.lat, it.long) },
    type = when (type) {
        EventTypeDto.ONLINE -> Event.EventType.ONLINE
        EventTypeDto.OFFLINE -> Event.EventType.OFFLINE
    },
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    speakerIds = speakerIds,
    participantsIds = participantsIds,
    participatedByMe = participatedByMe,
    attachment = attachment?.let {
        Event.Attachment(
            url = it.url,
            type = when (it.type) {
                AttachmentTypeDto.IMAGE -> Event.AttachmentType.IMAGE
                AttachmentTypeDto.VIDEO -> Event.AttachmentType.VIDEO
                AttachmentTypeDto.AUDIO -> Event.AttachmentType.AUDIO
            }
        )
    },
    link = link,
    users = users.mapValues { (_, userPreviewDto) ->
        Event.UserPreview(
            name = userPreviewDto.name,
            avatar = userPreviewDto.avatar
        )
    }
)
fun Event.toDto(): EventDto {
    return EventDto(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        authorJob = authorJob,
        content = content,
        datetime = datetime,
        published = published,
        coords = coords?.let {
            CoordinatesDto(
                lat = it.lat,
                long = it.long
            )
        },
        type = when (type) {
            Event.EventType.ONLINE -> EventTypeDto.ONLINE
            Event.EventType.OFFLINE -> EventTypeDto.OFFLINE
        },
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        speakerIds = speakerIds,
        participantsIds = participantsIds,
        participatedByMe = participatedByMe,
        attachment = attachment?.let {
            AttachmentDto(
                it.url,
                when (it.type) {
                    Event.AttachmentType.IMAGE -> AttachmentTypeDto.IMAGE
                    Event.AttachmentType.VIDEO -> AttachmentTypeDto.VIDEO
                    Event.AttachmentType.AUDIO -> AttachmentTypeDto.AUDIO
                }
            )
        },
        link = link,
        users = emptyMap()
    )
}
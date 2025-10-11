package ru.netology.nework.data

data class Event(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val datetime: String,
    val published: String,
    val coords: Coordinates? = null,
    val type: EventType,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean = false,
    val speakerIds: List<Long> = emptyList(),
    val participantsIds: List<Long> = emptyList(),
    val participatedByMe: Boolean = false,
    val attachment: Attachment? = null,
    val link: String? = null,
    val users: Map<Long, UserPreview> = emptyMap()
) {
    data class Coordinates(
        val lat: Double,
        val long: Double
    )

    data class Attachment(
        val url: String,
        val type: AttachmentType
    )

    data class UserPreview(
        val name: String,
        val avatar: String?
    )

    enum class AttachmentType {
        IMAGE, VIDEO, AUDIO
    }

    enum class EventType {
        ONLINE, OFFLINE
    }
}
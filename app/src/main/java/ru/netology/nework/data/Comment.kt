package ru.netology.nework.data

data class Comment(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: String,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean = false
) {
    fun toDto(): ru.netology.nework.dto.CommentDto {
        return ru.netology.nework.dto.CommentDto(
            id = id,
            postId = postId,
            authorId = authorId,
            author = author,
            authorAvatar = authorAvatar,
            content = content,
            published = published,
            likeOwnerIds = likeOwnerIds,
            likedByMe = likedByMe
        )
    }
}

fun ru.netology.nework.dto.CommentDto.toModel(): Comment {
    return Comment(
        id = id,
        postId = postId,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe
    )
}
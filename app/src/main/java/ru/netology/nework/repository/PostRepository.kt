// PostRepository.kt
package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.data.Post
import android.net.Uri

interface PostRepository {
    fun getPagingData(): Flow<PagingData<Post>>
    suspend fun getAll()
    suspend fun likeById(id: Long)
    suspend fun dislikeById(id: Long)
    suspend fun removeById(id: Long)
    suspend fun save(post: Post)
    suspend fun getById(id: Long): Post?
    suspend fun getUserWall(userId: Long): List<Post>
    suspend fun getMyWall(): List<Post>
    suspend fun uploadMedia(uri: Uri, type: Post.AttachmentType): String
    suspend fun getComments(postId: Long): List<ru.netology.nework.data.Comment>
    suspend fun saveComment(comment: ru.netology.nework.data.Comment): ru.netology.nework.data.Comment
    suspend fun likeComment(postId: Long, commentId: Long)
    suspend fun dislikeComment(postId: Long, commentId: Long)
    suspend fun removeComment(postId: Long, commentId: Long)
    fun getUserWallPaging(userId: Long): Flow<PagingData<Post>>
}
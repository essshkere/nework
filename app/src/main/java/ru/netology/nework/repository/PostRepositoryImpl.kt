package ru.netology.nework.repository

import android.net.Uri
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.MediaApi
import ru.netology.nework.api.PostApi
import ru.netology.nework.api.UserApi
import ru.netology.nework.dao.PostDao
import ru.netology.nework.data.Post
import ru.netology.nework.data.toModel
import ru.netology.nework.dto.AttachmentDto
import ru.netology.nework.dto.AttachmentTypeDto
import ru.netology.nework.dto.CoordinatesDto
import ru.netology.nework.dto.PostDto
import ru.netology.nework.dto.UserPreviewDto
import ru.netology.nework.mapper.toEntity
import ru.netology.nework.mapper.toModel
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postApi: PostApi,
    private val userApi: UserApi,
    private val postDao: PostDao,
    private val mediaApi: MediaApi
) : PostRepository {

    override fun getPagingData(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                println("DEBUG: Creating PostPagingSource")
                PostPagingSource(postApi)
            }
        ).flow
    }

    fun getPagingDataFromDb(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                postDao.pagingSource()
            }
        ).flow.map { pagingData ->
            pagingData.map { postEntity ->
                postEntity.toModel()
            }
        }
    }

    override fun getUserWallPaging(userId: Long): Flow<PagingData<Post>> {

        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                UserWallPagingSource(postApi, userId)
            }
        ).flow
    }

    override suspend fun getAll() {
        try {
            println("DEBUG: Запрашиваем посты с сервера...")
            val response = postApi.getAll()
            println("DEBUG: Ответ от сервера: код ${response.code()}, успех: ${response.isSuccessful}")
            if (response.isSuccessful) {
                val posts = response.body()
                println("DEBUG: Получено постов: ${posts?.size ?: 0}")
                posts?.let {
                    postDao.insert(posts.map { it.toEntity() })
                }
            } else {
                println("DEBUG: Ошибка при запросе: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("DEBUG: Исключение при загрузке постов: ${e.message}")
            e.printStackTrace()
            throw Exception("Ошибка загрузки постов: ${e.message}")
        }
    }

    override suspend fun likeById(id: Long) {
        try {
            val response = postApi.likeById(id)
            if (response.isSuccessful) {
                response.body()?.let { postDto ->
                    postDao.insert(postDto.toEntity())
                }
            } else {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для лайка")
                    404 -> Exception("Пост не найден")
                    else -> Exception("Ошибка лайка: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка лайка поста: ${e.message}")
        }
    }

    override suspend fun dislikeById(id: Long) {
        try {
            val response = postApi.dislikeById(id)
            if (response.isSuccessful) {
                response.body()?.let { postDto ->
                    postDao.insert(postDto.toEntity())
                }
            } else {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для дизлайка")
                    404 -> Exception("Пост не найден")
                    else -> Exception("Ошибка дизлайка: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка дизлайка поста: ${e.message}")
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = postApi.removeById(id)
            if (response.isSuccessful) {
                postDao.removeById(id)
            } else {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться или удалять свой пост")
                    else -> Exception("Ошибка удаления поста: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка удаления поста: ${e.message}")
        }
    }

    override suspend fun save(post: Post) {
        try {
            val uploadedAttachment = post.attachment?.let { attachment ->
                if (attachment.url.startsWith("content://") || attachment.url.startsWith("file://")) {
                    uploadMediaFile(Uri.parse(attachment.url), attachment.type)
                } else {
                    attachment
                }
            }

            val postDto = createPostDto(post, uploadedAttachment)
            val response = postApi.save(postDto)
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для создания поста")
                    else -> Exception("Ошибка сохранения поста: ${response.code()}")
                }
            }

            response.body()?.let { postDto ->
                postDao.insert(postDto.toEntity())
            }
        } catch (e: Exception) {
            throw when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Ошибка сети при сохранении поста")
                e.message?.contains("15", ignoreCase = true) == true ->
                    Exception("Размер файла превышает 15 МБ")
                else -> Exception("Ошибка сохранения поста: ${e.message}")
            }
        }
    }

    override suspend fun getById(id: Long): Post? {
        return try {
            val cachedPost = postDao.getById(id)?.toModel()
            if (cachedPost != null) {
                return cachedPost
            }

            val response = postApi.getById(id)
            if (response.isSuccessful) {
                response.body()?.let { postDto ->
                    val post = postDto.toModel()
                    postDao.insert(postDto.toEntity())
                    post
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserWall(userId: Long): List<Post> {
        return try {
            val allPosts = postApi.getAll()
            if (allPosts.isSuccessful) {
                allPosts.body()?.filter { it.authorId == userId }?.map { it.toModel() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMyWall(): List<Post> {
        return try {
            val response = postApi.getAll()
            if (response.isSuccessful) {
                response.body()?.map { it.toModel() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun uploadMedia(uri: Uri, type: Post.AttachmentType): String {
        return withContext(Dispatchers.IO) {
            try {
                val file = createTempFileFromUri(uri)
                if (file.length() > 15 * 1024 * 1024) {
                    throw Exception("Размер файла превышает 15 МБ")
                }

                val mimeType = when (type) {
                    Post.AttachmentType.IMAGE -> "image/*"
                    Post.AttachmentType.VIDEO -> "video/*"
                    Post.AttachmentType.AUDIO -> "audio/*"
                }

                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = mediaApi.uploadMedia(filePart)
                if (!response.isSuccessful) {
                    throw when (response.code()) {
                        403 -> Exception("Нужно авторизоваться для загрузки медиа")
                        415 -> Exception("Неподдерживаемый формат файла")
                        else -> Exception("Ошибка загрузки медиа: ${response.code()}")
                    }
                }

                val mediaDto = response.body() ?: throw Exception("Пустой ответ при загрузке медиа")
                file.delete()
                return@withContext mediaDto.url
            } catch (e: Exception) {
                throw when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        Exception("Ошибка сети при загрузке медиа")
                    else -> Exception("Ошибка загрузки файла: ${e.message}")
                }
            }
        }
    }

    private suspend fun uploadMediaFile(
        fileUri: Uri,
        attachmentType: Post.AttachmentType
    ): Post.Attachment = withContext(Dispatchers.IO) {
        try {
            val file = createTempFileFromUri(fileUri)
            if (file.length() > 15 * 1024 * 1024) {
                throw Exception("Размер файла превышает 15 МБ")
            }

            val mimeType = when (attachmentType) {
                Post.AttachmentType.IMAGE -> "image/*"
                Post.AttachmentType.VIDEO -> "video/*"
                Post.AttachmentType.AUDIO -> "audio/*"
            }

            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = mediaApi.uploadMedia(filePart)
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    403 -> Exception("Нужно авторизоваться для загрузки медиа")
                    415 -> Exception("Неподдерживаемый формат файла")
                    else -> Exception("Ошибка загрузки медиа: ${response.code()}")
                }
            }

            val mediaDto = response.body() ?: throw Exception("Пустой ответ при загрузке медиа")
            file.delete()
            return@withContext Post.Attachment(mediaDto.url, attachmentType)
        } catch (e: Exception) {
            throw when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    Exception("Ошибка сети при загрузке медиа")
                else -> Exception("Ошибка загрузки файла: ${e.message}")
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val context = ru.netology.nework.App.applicationContext()
        val file = File.createTempFile("media_upload", ".tmp", context.cacheDir)
        file.deleteOnExit()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw Exception("Не удалось открыть файл")

        return file
    }

    private fun createPostDto(post: Post, uploadedAttachment: Post.Attachment?): PostDto {
        return PostDto(
            id = post.id,
            authorId = post.authorId,
            author = post.author,
            authorAvatar = post.authorAvatar,
            authorJob = post.authorJob,
            content = post.content,
            published = post.published,
            coords = post.coords?.let { coords ->
                CoordinatesDto(
                    lat = coords.lat,
                    long = coords.long
                )
            },
            link = post.link,
            mentionIds = post.mentionIds,
            mentionedMe = post.mentionedMe,
            likeOwnerIds = post.likeOwnerIds,
            likedByMe = post.likedByMe,
            attachment = uploadedAttachment?.let { attachment ->
                AttachmentDto(
                    url = attachment.url,
                    type = when (attachment.type) {
                        Post.AttachmentType.IMAGE -> AttachmentTypeDto.IMAGE
                        Post.AttachmentType.VIDEO -> AttachmentTypeDto.VIDEO
                        Post.AttachmentType.AUDIO -> AttachmentTypeDto.AUDIO
                    }
                )
            },
            users = post.users.mapValues { (_, userPreview) ->
                UserPreviewDto(
                    name = userPreview.name,
                    avatar = userPreview.avatar
                )
            }
        )
    }

    override suspend fun getComments(postId: Long): List<ru.netology.nework.data.Comment> {
        return try {
            val response = postApi.getComments(postId)
            if (response.isSuccessful) {
                response.body()?.map { it.toModel() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveComment(comment: ru.netology.nework.data.Comment): ru.netology.nework.data.Comment {
        val response = postApi.saveComment(comment.postId, comment.toDto())
        if (!response.isSuccessful) {
            throw Exception("Ошибка сохранения комментария: ${response.code()}")
        }
        return response.body()!!.toModel()
    }

    override suspend fun likeComment(postId: Long, commentId: Long) {
        val response = postApi.likeComment(postId, commentId)
        if (!response.isSuccessful) {
            throw Exception("Ошибка лайка комментария: ${response.code()}")
        }
    }

    override suspend fun dislikeComment(postId: Long, commentId: Long) {
        val response = postApi.dislikeComment(postId, commentId)
        if (!response.isSuccessful) {
            throw Exception("Ошибка дизлайка комментария: ${response.code()}")
        }
    }

    override suspend fun removeComment(postId: Long, commentId: Long) {
        val response = postApi.removeComment(postId, commentId)
        if (!response.isSuccessful) {
            throw Exception("Ошибка удаления комментария: ${response.code()}")
        }
    }

}
package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.PostApi
import ru.netology.nework.dao.PostDao
import ru.netology.nework.data.Post
import ru.netology.nework.dto.PostDto
import ru.netology.nework.mapper.toEntity
import ru.netology.nework.mapper.toModel
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postApi: PostApi,
    private val postDao: PostDao
) : PostRepository {

    override fun getPagingData(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = { postDao.pagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toModel() }
        }
    }

    override suspend fun getAll() {
        try {
            val response = postApi.getAll()
            if (response.isSuccessful) {
                response.body()?.let { posts ->
                    postDao.insert(posts.map { it.toEntity() })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun likeById(id: Long) {
        try {
            val response = postApi.likeById(id)
            if (response.isSuccessful) {
                response.body()?.let { postDto ->
                    postDao.insert(postDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun dislikeById(id: Long) {
        try {
            val response = postApi.dislikeById(id)
            if (response.isSuccessful) {
                response.body()?.let { postDto ->
                    postDao.insert(postDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            postApi.removeById(id)
            postDao.removeById(id)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun save(post: Post) {
        try {
            val postDto = PostDto(
                id = post.id,
                authorId = post.authorId,
                author = post.author,
                authorAvatar = post.authorAvatar,
                authorJob = post.authorJob,
                content = post.content,
                published = post.published,
                coords = post.coords?.let { coords ->
                    ru.netology.nework.dto.CoordinatesDto(
                        lat = coords.lat.toString(),
                        long = coords.long.toString()
                    )
                },
                link = post.link,
                mentionIds = post.mentionIds,
                mentionedMe = false,
                likeOwnerIds = post.likeOwnerIds,
                likedByMe = post.likedByMe,
                attachment = post.attachment?.let { attachment ->
                    ru.netology.nework.dto.AttachmentDto(
                        url = attachment.url,
                        type = when (attachment.type) {
                            Post.AttachmentType.IMAGE -> ru.netology.nework.dto.AttachmentTypeDto.IMAGE
                            Post.AttachmentType.VIDEO -> ru.netology.nework.dto.AttachmentTypeDto.VIDEO
                            Post.AttachmentType.AUDIO -> ru.netology.nework.dto.AttachmentTypeDto.AUDIO
                        }
                    )
                },
                users = emptyMap()
            )

            val response = postApi.save(postDto)
            if (response.isSuccessful) {
                response.body()?.let { postDto ->
                    postDao.insert(postDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
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
                    val post = postDto.toEntity().toModel()
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
}
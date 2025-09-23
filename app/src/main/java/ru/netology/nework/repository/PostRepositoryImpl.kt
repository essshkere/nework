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
import ru.netology.nework.mapper.toDto
import ru.netology.nework.mapper.toEntity
import ru.netology.nework.mapper.toModel
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
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
            val response = postApi.save(post.toDto())
            if (response.isSuccessful) {
                response.body()?.let { postDto ->
                    postDao.insert(postDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
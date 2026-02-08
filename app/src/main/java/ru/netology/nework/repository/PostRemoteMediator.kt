package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nework.api.PostApi
import ru.netology.nework.data.AppDatabase
import ru.netology.nework.data.PostEntity
import ru.netology.nework.mapper.toEntity

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val postApi: PostApi,
    private val db: AppDatabase
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        return try {
            val response = when (loadType) {
                LoadType.REFRESH -> postApi.getAll()
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        postApi.getAll()
                    } else {
                        postApi.getBefore(lastItem.id, state.config.pageSize)
                    }
                }
            }

            if (!response.isSuccessful) {
                return MediatorResult.Error(Exception("Failed to load posts: ${response.code()}"))
            }

            val posts = response.body() ?: emptyList()

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    db.postDao().clear()
                }
                db.postDao().insert(posts.map { it.toEntity() })
            }

            MediatorResult.Success(
                endOfPaginationReached = posts.isEmpty()
            )
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
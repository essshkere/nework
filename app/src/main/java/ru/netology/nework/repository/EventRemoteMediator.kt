package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nework.api.EventApi
import ru.netology.nework.data.AppDatabase
import ru.netology.nework.data.EventEntity
import ru.netology.nework.mapper.toEntity

@OptIn(ExperimentalPagingApi::class)
class EventRemoteMediator(
    private val eventApi: EventApi,
    private val db: AppDatabase
) : RemoteMediator<Long, EventEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Long, EventEntity>
    ): MediatorResult {
        return try {
            val response = when (loadType) {
                LoadType.REFRESH -> eventApi.getAll()
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        eventApi.getAll()
                    } else {
                        eventApi.getBefore(lastItem.id, state.config.pageSize)
                    }
                }
            }

            if (!response.isSuccessful) {
                return MediatorResult.Error(Exception("Failed to load events: ${response.code()}"))
            }

            val events = response.body() ?: emptyList()

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    db.eventDao().clear()
                }
                db.eventDao().insert(events.map { it.toEntity() })
            }

            MediatorResult.Success(
                endOfPaginationReached = events.isEmpty()
            )
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
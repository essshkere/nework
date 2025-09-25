package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.EventApi
import ru.netology.nework.dao.EventDao
import ru.netology.nework.data.Event
import ru.netology.nework.mapper.toDto
import ru.netology.nework.mapper.toEntity
import ru.netology.nework.mapper.toModel
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventApi: EventApi,
    private val eventDao: EventDao
) : EventRepository {

    override fun getPagingData(): Flow<PagingData<Event>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = { eventDao.pagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toModel() }
        }
    }

    override suspend fun getAll() {
        try {
            val response = eventApi.getAll()
            if (response.isSuccessful) {
                response.body()?.let { events ->
                    eventDao.insert(events.map { it.toEntity() })
                }
            }
        } catch (e: Exception) {
            // Handle error
            e.printStackTrace()
        }
    }

    override suspend fun likeById(id: Long) {
        try {
            val response = eventApi.likeById(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun dislikeById(id: Long) {
        try {
            val response = eventApi.dislikeById(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            eventApi.removeById(id)
            eventDao.removeById(id)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun save(event: Event) {
        try {
            val response = eventApi.save(event.toDto())
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun participate(id: Long) {
        try {
            val response = eventApi.participate(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun unparticipate(id: Long) {
        try {
            val response = eventApi.unparticipate(id)
            if (response.isSuccessful) {
                response.body()?.let { eventDto ->
                    eventDao.insert(eventDto.toEntity())
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getById(id: Long): Event? {
        return try {
            val response = eventApi.getById(id)
            if (response.isSuccessful) {
                response.body()?.toEntity()?.toModel()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
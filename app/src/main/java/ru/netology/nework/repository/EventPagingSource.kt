package ru.netology.nework.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nework.api.EventApi
import ru.netology.nework.data.Event
import ru.netology.nework.dto.EventDto
import ru.netology.nework.mapper.toModel

class EventPagingSource(
    private val eventApi: EventApi
) : PagingSource<Long, Event>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Event> {
        return try {
            val response = eventApi.getAll()
            if (!response.isSuccessful) {
                throw Exception("Failed to load events: ${response.code()}")
            }

            val events = response.body()?.map { it.toModel() } ?: emptyList()

            LoadResult.Page(
                data = events,
                prevKey = null,
                nextKey = if (events.isNotEmpty()) events.last().id else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Event>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
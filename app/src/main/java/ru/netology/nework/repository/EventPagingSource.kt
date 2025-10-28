package ru.netology.nework.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nework.api.EventApi
import ru.netology.nework.data.Event
import ru.netology.nework.mapper.toModel

class EventPagingSource(
    private val eventApi: EventApi
) : PagingSource<Long, Event>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Event> {
        return try {
            val response = when (params) {
                is LoadParams.Refresh -> {
                    eventApi.getLatest(params.loadSize)
                }
                is LoadParams.Append -> {
                    val key = params.key ?: return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                    eventApi.getBefore(key, params.loadSize)
                }
                is LoadParams.Prepend -> {
                    val key = params.key ?: return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                    eventApi.getAfter(key, params.loadSize)
                }
            }

            if (!response.isSuccessful) {
                throw Exception("Failed to load events: ${response.code()}")
            }

            val events = response.body()?.map { it.toModel() } ?: emptyList()

            LoadResult.Page(
                data = events,
                prevKey = events.firstOrNull()?.id,
                nextKey = events.lastOrNull()?.id
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
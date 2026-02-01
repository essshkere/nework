// PostPagingSource.kt
package ru.netology.nework.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nework.api.PostApi
import ru.netology.nework.data.Post
import ru.netology.nework.mapper.toModel

class PostPagingSource(
    private val postApi: PostApi
) : PagingSource<Long, Post>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        return try {
            val response = when (params) {
                is LoadParams.Refresh -> {
                    postApi.getLatest(count = params.loadSize)
                }
                is LoadParams.Append -> {
                    val key = params.key ?: return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                    postApi.getBefore(id = key, count = params.loadSize)
                }
                is LoadParams.Prepend -> {
                    val key = params.key ?: return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                    postApi.getAfter(id = key, count = params.loadSize)
                }
            }

            if (!response.isSuccessful) {
                throw Exception("Failed to load posts: ${response.code()}")
            }

            val posts = response.body()?.map { it.toModel() } ?: emptyList()

            LoadResult.Page(
                data = posts,
                prevKey = if (posts.isEmpty()) null else posts.first().id,
                nextKey = if (posts.isEmpty()) null else posts.last().id
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
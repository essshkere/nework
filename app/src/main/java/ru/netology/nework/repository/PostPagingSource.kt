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
            val pageSize = params.loadSize
            val key = params.key

            val response = if (key == null) {
                postApi.getLatest(count = pageSize)
            } else {
                postApi.getBefore(id = key, count = pageSize)
            }

            if (!response.isSuccessful) {
                throw Exception("Failed to load posts: ${response.code()}")
            }

            val posts = response.body()?.map { it.toModel() } ?: emptyList()

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = posts.lastOrNull()?.id
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
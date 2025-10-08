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
            val response = postApi.getAll()
            if (!response.isSuccessful) {
                throw Exception("Failed to load posts: ${response.code()}")
            }

            val posts = response.body()?.map { it.toModel() } ?: emptyList()

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = if (posts.isNotEmpty()) posts.last().id else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
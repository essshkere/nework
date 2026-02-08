package ru.netology.nework.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.netology.nework.api.PostApi
import ru.netology.nework.data.Post
import ru.netology.nework.mapper.toModel

class PostPagingSource(
    private val postApi: PostApi
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize

            val response = postApi.getLatest(count = pageSize)

            if (!response.isSuccessful) {
                throw Exception("Failed to load posts: ${response.code()}")
            }

            val posts = response.body()?.map { it.toModel() } ?: emptyList()

            LoadResult.Page(
                data = posts,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (posts.isNotEmpty()) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
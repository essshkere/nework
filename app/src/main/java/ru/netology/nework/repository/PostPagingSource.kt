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

            println("DEBUG: Загрузка постов, страница: $page, размер: $pageSize")

            val response = postApi.getAll()

            if (!response.isSuccessful) {
                println("DEBUG: Ошибка при запросе: ${response.code()}")
                when (response.code()) {
                    403 -> {
                        throw AuthException("403: Требуется авторизация")
                    }
                    401 -> throw Exception("401: Не авторизован")
                    else -> throw Exception("Ошибка сервера: ${response.code()}")
                }
            }

            val posts = response.body()?.map { it.toModel() } ?: emptyList()
            println("DEBUG: Получено постов: ${posts.size}")

            LoadResult.Page(
                data = posts,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (posts.isNotEmpty()) page + 1 else null
            )
        } catch (e: Exception) {
            println("DEBUG: Исключение в PostPagingSource: ${e.message}")
            e.printStackTrace()
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

class AuthException(message: String) : Exception(message)

package ru.netology.nework.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.netology.nework.dto.PostDto
import retrofit2.Response


interface PostApi {
    @GET("api/posts")
    suspend fun getAll(): Response<List<PostDto>>

    @GET("api/posts/{id}")
    suspend fun getById(@Path("id") id: Long): Response<PostDto>

    @POST("api/posts")
    suspend fun save(@Body post: PostDto): Response<PostDto>

    @DELETE("api/posts/{id}")
    suspend fun removeById(@Path("id") id: Long): Response<Unit>

    @POST("api/posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Response<PostDto>

    @DELETE("api/posts/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Long): Response<PostDto>
}
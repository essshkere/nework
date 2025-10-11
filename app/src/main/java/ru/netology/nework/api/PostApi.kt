package ru.netology.nework.api

import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.dto.PostDto

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

    @GET("api/posts/latest")
    suspend fun getLatest(@Query("count") count: Int): Response<List<PostDto>>

    @GET("api/posts/{id}/newer")
    suspend fun getNewer(@Path("id") id: Long): Response<List<PostDto>>
    @GET("api/posts/{id}/before")
    suspend fun getBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<PostDto>>

    @GET("api/posts/{id}/after")
    suspend fun getAfter(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<PostDto>>
}
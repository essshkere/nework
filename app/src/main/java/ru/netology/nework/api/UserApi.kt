package ru.netology.nework.api

import retrofit2.http.GET
import retrofit2.http.Path
import ru.netology.nework.dto.PostDto
import ru.netology.nework.dto.UserDto
import retrofit2.Response
import ru.netology.nework.dto.JobDto


interface UserApi {
    @GET("api/users")
    suspend fun getAll(): Response<List<UserDto>>

    @GET("api/users/{id}")
    suspend fun getById(@Path("id") id: Long): Response<UserDto>

    @GET("api/users/{id}/jobs")
    suspend fun getJobs(@Path("id") id: Long): Response<List<JobDto>>

    @GET("api/users/{id}/wall")
    suspend fun getWall(@Path("id") id: Long): Response<List<PostDto>>

    @GET("api/my/wall")
    suspend fun getMyWall(): Response<List<PostDto>>
}
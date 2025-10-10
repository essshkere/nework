package ru.netology.nework.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import ru.netology.nework.dto.JobDto
import ru.netology.nework.dto.PostDto
import ru.netology.nework.dto.UserDto

interface UserApi {

    @GET("api/users")
    suspend fun getAll(): Response<List<UserDto>>

    @GET("api/users/{id}")
    suspend fun getById(@Path("id") id: Long): Response<UserDto>

    @GET("api/{userId}/jobs")
    suspend fun getJobs(@Path("userId") userId: Long): Response<List<JobDto>>

    @GET("api/{authorId}/wall")
    suspend fun getWall(@Path("authorId") authorId: Long): Response<List<PostDto>>

    @GET("api/my/wall")
    suspend fun getMyWall(): Response<List<PostDto>>
}
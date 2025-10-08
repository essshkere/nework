package ru.netology.nework.api

import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.dto.JobDto

interface JobApi {
    @GET("api/my/jobs")
    suspend fun getMyJobs(): Response<List<JobDto>>

    @POST("api/my/jobs")
    suspend fun save(@Body job: JobDto): Response<JobDto>

    @DELETE("api/my/jobs/{id}")
    suspend fun removeById(@Path("id") id: Long): Response<Unit>
}
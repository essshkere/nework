package ru.netology.nework.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.netology.nework.dto.EventDto
import retrofit2.Response


interface EventApi {
    @GET("api/events")
    suspend fun getAll(): Response<List<EventDto>>

    @GET("api/events/{id}")
    suspend fun getById(@Path("id") id: Long): Response<EventDto>

    @POST("api/events")
    suspend fun save(@Body event: EventDto): Response<EventDto>

    @DELETE("api/events/{id}")
    suspend fun removeById(@Path("id") id: Long): Response<Unit>

    @POST("api/events/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Response<EventDto>

    @DELETE("api/events/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Long): Response<EventDto>

    @POST("api/events/{id}/participants")
    suspend fun participate(@Path("id") id: Long): Response<EventDto>

    @DELETE("api/events/{id}/participants")
    suspend fun unparticipate(@Path("id") id: Long): Response<EventDto>
}
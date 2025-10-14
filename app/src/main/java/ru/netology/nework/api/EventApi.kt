package ru.netology.nework.api

import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.dto.EventDto

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

    @GET("api/events/latest")
    suspend fun getLatest(@Query("count") count: Int): Response<List<EventDto>>

    @GET("api/events/{id}/newer")
    suspend fun getNewer(@Path("id") id: Long): Response<List<EventDto>>

    @GET("api/events/{id}/before")
    suspend fun getBefore(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<EventDto>>

    @GET("api/events/{id}/after")
    suspend fun getAfter(
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<EventDto>>
}
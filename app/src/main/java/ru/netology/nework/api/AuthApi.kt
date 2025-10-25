package ru.netology.nework.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.dto.LoginResponseDto

interface AuthApi {
    @POST("api/users/authentication")
    suspend fun signIn(
        @Query("login") login: String,
        @Query("pass") password: String
    ): Response<LoginResponseDto>

    @Multipart
    @POST("api/users/registration")
    suspend fun signUp(
        @Query("login") login: String,
        @Query("pass") password: String,
        @Query("name") name: String,
        @Part file: MultipartBody.Part?
    ): Response<LoginResponseDto>
}
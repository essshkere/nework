package ru.netology.nework.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
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
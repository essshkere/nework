package ru.netology.nework.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.dto.LoginResponseDto

interface AuthApi {
    @FormUrlEncoded
    @POST("api/users/authentication")
    suspend fun signIn(
        @Field("login") login: String,
        @Field("pass") password: String
    ): Response<LoginResponseDto>

    @Multipart
    @POST("api/users/registration")
    suspend fun signUp(
        @Part("login") login: RequestBody,
        @Part("pass") password: RequestBody,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part?
    ): Response<LoginResponseDto>
}
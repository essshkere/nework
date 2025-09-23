package ru.netology.nework.api

import retrofit2.http.Body
import retrofit2.http.POST
import ru.netology.nework.dto.LoginRequestDto
import ru.netology.nework.dto.LoginResponseDto
import retrofit2.Response
import ru.netology.nework.dto.RegisterRequestDto

interface AuthApi {
    @POST("api/users/authentication")
    suspend fun signIn(@Body loginRequest: LoginRequestDto): Response<LoginResponseDto>

    @POST("api/users/registration")
    suspend fun signUp(@Body registerRequest: RegisterRequestDto): Response<LoginResponseDto>
}
package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String
)

data class LoginResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("token") val token: String
)

data class RegisterRequestDto(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String
)
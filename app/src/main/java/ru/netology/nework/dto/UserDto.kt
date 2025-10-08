package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("login") val login: String,
    @SerializedName("name") val name: String,
    @SerializedName("avatar") val avatar: String? = null
)

data class UserPreviewDto(
    @SerializedName("name") val name: String,
    @SerializedName("avatar") val avatar: String?
)
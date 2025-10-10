package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class MediaDto(
    @SerializedName("url")
    val url: String
)
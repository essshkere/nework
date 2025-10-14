package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class JobDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("position") val position: String,
    @SerializedName("start") val start: String,
    @SerializedName("finish") val finish: String?,
    @SerializedName("link") val link: String?
)
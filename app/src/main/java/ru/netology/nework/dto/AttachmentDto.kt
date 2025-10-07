package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class AttachmentDto(
    @SerializedName("url") val url: String,
    @SerializedName("type") val type: AttachmentTypeDto
)

enum class AttachmentTypeDto {
    @SerializedName("IMAGE") IMAGE,
    @SerializedName("VIDEO") VIDEO,
    @SerializedName("AUDIO") AUDIO
}
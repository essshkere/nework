package ru.netology.nework.data

import com.google.android.gms.fido.fido2.api.common.Attachment

data class Job(
    val id: Long,
    val name: String,
    val position: String,
    val start: String,
    val finish: String? = null,
    val link: String? = null
)
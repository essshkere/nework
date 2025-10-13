package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.data.Event

interface EventRepository {
    fun getPagingData(): Flow<PagingData<Event>>
    suspend fun getAll()
    suspend fun likeById(id: Long)
    suspend fun dislikeById(id: Long)
    suspend fun removeById(id: Long)
    suspend fun save(event: Event)
    suspend fun participate(id: Long)
    suspend fun unparticipate(id: Long)
    suspend fun getById(id: Long): Event?
    suspend fun uploadMedia(uri: Uri, type: Event.AttachmentType): String
}
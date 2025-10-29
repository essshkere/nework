package ru.netology.nework.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nework.data.Event
import ru.netology.nework.repository.EventRepository
import javax.inject.Inject
import android.net.Uri

@HiltViewModel
class EditEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventRepository
) : ViewModel() {
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: 0L

    suspend fun getEventById(id: Long): Event? = repository.getById(id)

    suspend fun save(event: Event) = repository.save(event)

    suspend fun uploadMedia(uri: Uri, type: Event.AttachmentType): String =
        repository.uploadMedia(uri, type)
}
package ru.netology.nework.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nework.data.Event
import javax.inject.Inject

@HiltViewModel
class EditEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsViewModel: EventsViewModel
) : ViewModel() {
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: 0L

    fun getEventById(id: Long) = eventsViewModel.getById(id)
    fun save(event: Event) = eventsViewModel.save(event)
    fun uploadMedia(uri: android.net.Uri, type: Event.AttachmentType) = eventsViewModel.uploadMedia(uri, type)
}
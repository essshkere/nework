package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.netology.nework.data.Event
import ru.netology.nework.repository.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {
    val data: Flow<PagingData<Event>> = repository.getPagingData().cachedIn(viewModelScope)

    fun likeById(id: Long) = viewModelScope.launch {
        repository.likeById(id)
    }

    fun dislikeById(id: Long) = viewModelScope.launch {
        repository.dislikeById(id)
    }

    fun removeById(id: Long) = viewModelScope.launch {
        repository.removeById(id)
    }

    fun save(event: Event) = viewModelScope.launch {
        repository.save(event)
    }

    fun participate(id: Long) = viewModelScope.launch {
        repository.participate(id)
    }

    fun unparticipate(id: Long) = viewModelScope.launch {
        repository.unparticipate(id)
    }
}
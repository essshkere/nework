package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nework.data.Event
import ru.netology.nework.repository.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {
    val data: Flow<PagingData<Event>> = repository.getPagingData().cachedIn(viewModelScope)

    private val _eventsState = MutableStateFlow<EventsState>(EventsState.Idle)
    val eventsState = _eventsState.asStateFlow()

    fun likeById(id: Long) = viewModelScope.launch {
        _eventsState.value = EventsState.Loading
        try {
            repository.likeById(id)
            _eventsState.value = EventsState.Success("Лайк поставлен")
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error(
                when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для лайка"
                    e.message?.contains("404") == true -> "Событие не найдено"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при лайке: ${e.message}"
                }
            )
        }
    }

    fun dislikeById(id: Long) = viewModelScope.launch {
        _eventsState.value = EventsState.Loading
        try {
            repository.dislikeById(id)
            _eventsState.value = EventsState.Success("Лайк убран")
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error(
                when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для дизлайка"
                    e.message?.contains("404") == true -> "Событие не найдено"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при дизлайке: ${e.message}"
                }
            )
        }
    }

    fun toggleParticipation(event: Event) = viewModelScope.launch {
        _eventsState.value = EventsState.Loading
        try {
            if (event.participatedByMe) {
                // Если уже участвует - отказываемся
                unparticipate(event.id)
            } else {
                // Если не участвует - присоединяемся
                participate(event.id)
            }
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error(
                when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для участия в событии"
                    e.message?.contains("404") == true -> "Событие не найдено"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при участии в событии: ${e.message}"
                }
            )
        }
    }


    fun participate(id: Long) = viewModelScope.launch {
        _eventsState.value = EventsState.Loading
        try {
            repository.participate(id)
            _eventsState.value = EventsState.Success("Вы участвуете в событии")
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error(
                when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для участия"
                    e.message?.contains("404") == true -> "Событие не найдено"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при присоединении к событию: ${e.message}"
                }
            )
        }
    }


    fun unparticipate(id: Long) = viewModelScope.launch {
        _eventsState.value = EventsState.Loading
        try {
            repository.unparticipate(id)
            _eventsState.value = EventsState.Success("Вы отказались от участия")
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error(
                when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для отказа от участия"
                    e.message?.contains("404") == true -> "Событие не найдено"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при отказе от участия: ${e.message}"
                }
            )
        }
    }


    fun removeById(id: Long) = viewModelScope.launch {
        _eventsState.value = EventsState.Loading
        try {
            repository.removeById(id)
            _eventsState.value = EventsState.Success("Событие удалено")
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error(
                when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для удаления события"
                    e.message?.contains("404") == true -> "Событие не найдено"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при удалении события: ${e.message}"
                }
            )
        }
    }


    fun save(event: Event) = viewModelScope.launch {
        _eventsState.value = EventsState.Loading
        try {
            repository.save(event)
            _eventsState.value = EventsState.Success(
                if (event.id == 0L) "Событие создано" else "Событие обновлено"
            )
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error(
                when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для создания события"
                    e.message?.contains("415") == true -> "Неподдерживаемый формат файла"
                    e.message?.contains("15", ignoreCase = true) == true ->
                        "Размер файла превышает 15 МБ"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при сохранении события: ${e.message}"
                }
            )
        }
    }


    suspend fun getById(id: Long): Event? {
        return try {
            repository.getById(id)
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error("Ошибка при загрузке события: ${e.message}")
            null
        }
    }


    fun refresh() = viewModelScope.launch {
        try {
            repository.getAll()
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error("Ошибка при обновлении событий: ${e.message}")
        }
    }


    fun clearState() {
        _eventsState.value = EventsState.Idle
    }


    sealed class EventsState {
        object Idle : EventsState()
        object Loading : EventsState()
        data class Success(val message: String) : EventsState()
        data class Error(val message: String) : EventsState()
    }
}
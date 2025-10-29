package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.data.Event
import ru.netology.nework.repository.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {
    val data: Flow<PagingData<Event>> = repository.getPagingData().cachedIn(viewModelScope)
    private val _eventsState = MutableStateFlow<EventsState>(EventsState.Idle)
    val eventsState: StateFlow<EventsState> = _eventsState.asStateFlow()
    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    suspend fun uploadMedia(uri: Uri, type: Event.AttachmentType): String {
        return withContext(Dispatchers.IO) {
            try {
                repository.uploadMedia(uri, type)
            } catch (e: Exception) {
                throw Exception("Ошибка загрузки медиа: ${e.message}")
            }
        }
    }

    fun likeById(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = "like"
        )

        try {
            repository.likeById(id)
            _eventsState.value = EventsState.Success("Лайк поставлен")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для лайка"
                e.message?.contains("404") == true -> "Событие не найдено"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при лайке: ${e.message}"
            }
            _eventsState.value = EventsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    fun dislikeById(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = "dislike"
        )

        try {
            repository.dislikeById(id)
            _eventsState.value = EventsState.Success("Лайк убран")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для дизлайка"
                e.message?.contains("404") == true -> "Событие не найдено"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при дизлайке: ${e.message}"
            }
            _eventsState.value = EventsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    fun toggleParticipation(event: Event) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = "participation"
        )

        try {
            if (event.participatedByMe) {
                unparticipate(event.id)
            } else {
                participate(event.id)
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для участия в событии"
                e.message?.contains("404") == true -> "Событие не найдено"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при участии в событии: ${e.message}"
            }
            _eventsState.value = EventsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    fun participate(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = "participate"
        )

        try {
            repository.participate(id)
            _eventsState.value = EventsState.Success("Вы участвуете в событии")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для участия"
                e.message?.contains("404") == true -> "Событие не найдено"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при присоединении к событию: ${e.message}"
            }
            _eventsState.value = EventsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    fun unparticipate(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = "unparticipate"
        )

        try {
            repository.unparticipate(id)
            _eventsState.value = EventsState.Success("Вы отказались от участия")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для отказа от участия"
                e.message?.contains("404") == true -> "Событие не найдено"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при отказе от участия: ${e.message}"
            }
            _eventsState.value = EventsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = "delete"
        )

        try {
            repository.removeById(id)
            _eventsState.value = EventsState.Success("Событие удалено")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для удаления события"
                e.message?.contains("404") == true -> "Событие не найдено"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при удалении события: ${e.message}"
            }
            _eventsState.value = EventsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    fun save(event: Event) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = if (event.id == 0L) "create" else "edit"
        )

        try {
            repository.save(event)
            val successMessage = if (event.id == 0L) "Событие создано" else "Событие обновлено"
            _eventsState.value = EventsState.Success(successMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для создания события"
                e.message?.contains("415") == true -> "Неподдерживаемый формат файла"
                e.message?.contains("15", ignoreCase = true) == true ->
                    "Размер файла превышает 15 МБ"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при сохранении события: ${e.message}"
            }
            _eventsState.value = EventsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    suspend fun getById(id: Long): Event? {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val event = repository.getById(id)
            _uiState.value = _uiState.value.copy(isLoading = false)
            event
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error("Ошибка при загрузке события: ${e.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Ошибка при загрузке события",
                showError = true
            )
            null
        }
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isRefreshing = true
        )

        try {
            repository.getAll()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false
            )
        } catch (e: Exception) {
            _eventsState.value = EventsState.Error("Ошибка при обновлении событий: ${e.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Ошибка при обновлении",
                showError = true,
                isRefreshing = false
            )
        }
    }

    fun clearState() {
        _eventsState.value = EventsState.Idle
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null,
            showError = false
        )
        _eventsState.value = EventsState.Idle
    }

    fun clearLoading() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            currentOperation = null
        )
    }

    sealed class EventsState {
        object Idle : EventsState()
        data class Success(val message: String) : EventsState()
        data class Error(val message: String) : EventsState()
    }

    data class EventsUiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val showError: Boolean = false,
        val currentOperation: String? = null
    )
}
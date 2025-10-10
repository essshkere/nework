package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nework.data.Job
import ru.netology.nework.repository.JobRepository
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _state = MutableStateFlow<JobsState>(JobsState.Idle)
    val state: StateFlow<JobsState> = _state.asStateFlow()

    private val _uiState = MutableStateFlow<JobsUiState>(JobsUiState())
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            _state.value = JobsState.Loading
            _uiState.value = JobsUiState(isLoading = true)

            try {
                val jobsList = repository.getMyJobs()
                _jobs.value = jobsList
                _state.value = JobsState.Success
                _uiState.value = JobsUiState(
                    isLoading = false,
                    isEmpty = jobsList.isEmpty()
                )
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для просмотра работ"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при загрузке работ: ${e.message}"
                }
                _state.value = JobsState.Error(errorMessage)
                _uiState.value = JobsUiState(
                    isLoading = false,
                    error = errorMessage,
                    showError = true
                )
            }
        }
    }

    fun saveJob(job: Job) {
        viewModelScope.launch {
            _state.value = JobsState.Loading
            _uiState.value = JobsUiState(
                isLoading = true,
                currentOperation = if (job.id == 0L) "create" else "edit"
            )

            try {
                repository.save(job)
                _state.value = JobsState.Success
                _uiState.value = JobsUiState(
                    isLoading = false,
                    currentOperation = null
                )
                loadJobs() // Reload jobs after save
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для сохранения работы"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при сохранении работы: ${e.message}"
                }
                _state.value = JobsState.Error(errorMessage)
                _uiState.value = JobsUiState(
                    isLoading = false,
                    error = errorMessage,
                    showError = true,
                    currentOperation = null
                )
            }
        }
    }

    fun removeJob(id: Long) {
        viewModelScope.launch {
            _state.value = JobsState.Loading
            _uiState.value = JobsUiState(
                isLoading = true,
                currentOperation = "delete"
            )

            try {
                repository.removeById(id)
                _state.value = JobsState.Success
                _uiState.value = JobsUiState(
                    isLoading = false,
                    currentOperation = null
                )
                loadJobs() // Reload jobs after delete
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("403") == true -> "Необходимо авторизоваться для удаления работы"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка при удалении работы: ${e.message}"
                }
                _state.value = JobsState.Error(errorMessage)
                _uiState.value = JobsUiState(
                    isLoading = false,
                    error = errorMessage,
                    showError = true,
                    currentOperation = null
                )
            }
        }
    }

    fun clearState() {
        _state.value = JobsState.Idle
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null,
            showError = false
        )
        _state.value = JobsState.Idle
    }

    fun clearLoading() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentOperation = null
        )
    }

    sealed class JobsState {
        object Idle : JobsState()
        object Loading : JobsState()
        object Success : JobsState()
        data class Error(val message: String) : JobsState()
    }

    data class JobsUiState(
        val isLoading: Boolean = false,
        val isEmpty: Boolean = false,
        val error: String? = null,
        val showError: Boolean = false,
        val currentOperation: String? = null
    )
}
package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nework.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun signIn(login: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _uiState.value = AuthUiState(isLoading = true)

            try {
                val response = authRepository.signIn(login, password)
                _authState.value = AuthState.Success
                _uiState.value = AuthUiState(
                    isAuthenticated = true,
                    userId = response.id
                )
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("400") == true -> "Неверный логин или пароль"
                    e.message?.contains("404") == true -> "Пользователь не найден"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение к интернету"
                    else -> "Ошибка аутентификации: ${e.message}"
                }
                _authState.value = AuthState.Error(errorMessage)
                _uiState.value = AuthUiState(
                    error = errorMessage,
                    showError = true
                )
            }
        }
    }

    fun signUp(login: String, password: String, name: String, avatarUri: String? = null) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _uiState.value = AuthUiState(isLoading = true)

            try {
                val response = authRepository.signUp(login, password, name, avatarUri)
                _authState.value = AuthState.Success
                _uiState.value = AuthUiState(
                    isAuthenticated = true,
                    userId = response.id
                )
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("403") == true -> "Пользователь с таким логином уже зарегистрирован"
                    e.message?.contains("415") == true -> "Неправильный формат изображения. Используйте JPG или PNG"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение к интернету"
                    else -> "Ошибка регистрации: ${e.message}"
                }
                _authState.value = AuthState.Error(errorMessage)
                _uiState.value = AuthUiState(
                    error = errorMessage,
                    showError = true
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                authRepository.logout()
                _authState.value = AuthState.Idle
                _uiState.value = AuthUiState(
                    isAuthenticated = false,
                    userId = 0L
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    error = "Ошибка при выходе: ${e.message}",
                    showError = true
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null,
            showError = false
        )
        _authState.value = AuthState.Idle
    }

    fun clearLoading() {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    private fun checkAuthStatus() {
        val isAuthenticated = authRepository.getToken() != null
        val userId = authRepository.getUserId()

        _uiState.value = AuthUiState(
            isAuthenticated = isAuthenticated,
            userId = userId
        )
    }

    fun isAuthenticated(): Boolean {
        return authRepository.getToken() != null
    }

    fun getUserId(): Long {
        return authRepository.getUserId()
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    data class AuthUiState(
        val isAuthenticated: Boolean = false,
        val userId: Long = 0L,
        val isLoading: Boolean = false,
        val error: String? = null,
        val showError: Boolean = false
    )
}
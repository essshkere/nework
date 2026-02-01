package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nework.data.Post
import ru.netology.nework.repository.PostRepository
import javax.inject.Inject
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {
    val data: Flow<PagingData<Post>> = repository.getPagingData().cachedIn(viewModelScope)
    private val _postsState = MutableStateFlow<PostsState>(PostsState.Idle)
    val postsState: StateFlow<PostsState> = _postsState.asStateFlow()
    private val _uiState = MutableStateFlow<PostsUiState>(PostsUiState())
    val uiState: StateFlow<PostsUiState> = _uiState.asStateFlow()
    private val _postsList = MutableStateFlow<List<Post>>(emptyList())
    val postsList: StateFlow<List<Post>> = _postsList.asStateFlow()

    init {
        refresh()
    }

    suspend fun uploadMedia(uri: Uri, type: Post.AttachmentType): String {
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
            _postsState.value = PostsState.Success("Лайк поставлен")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для лайка"
                e.message?.contains("404") == true -> "Пост не найден"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при лайке: ${e.message}"
            }
            _postsState.value = PostsState.Error(errorMessage)
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
            _postsState.value = PostsState.Success("Лайк убран")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для дизлайка"
                e.message?.contains("404") == true -> "Пост не найден"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при дизлайке: ${e.message}"
            }
            _postsState.value = PostsState.Error(errorMessage)
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
            _postsState.value = PostsState.Success("Пост удален")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться или удалять свой пост"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при удалении поста: ${e.message}"
            }
            _postsState.value = PostsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    fun save(post: Post) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentOperation = if (post.id == 0L) "create" else "edit"
        )

        try {
            repository.save(post)
            val successMessage = if (post.id == 0L) "Пост создан" else "Пост обновлен"
            _postsState.value = PostsState.Success(successMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentOperation = null
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("403") == true -> "Необходимо авторизоваться для создания поста"
                e.message?.contains("415") == true -> "Неподдерживаемый формат файла"
                e.message?.contains("15", ignoreCase = true) == true ->
                    "Размер файла превышает 15 МБ"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение"
                else -> "Ошибка при сохранении поста: ${e.message}"
            }
            _postsState.value = PostsState.Error(errorMessage)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage,
                showError = true,
                currentOperation = null
            )
        }
    }

    suspend fun getPostById(id: Long): Post? {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val post = repository.getById(id)
            _uiState.value = _uiState.value.copy(isLoading = false)
            post
        } catch (e: Exception) {
            _postsState.value = PostsState.Error("Ошибка при загрузке поста: ${e.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Ошибка при загрузке поста",
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
            _postsState.value = PostsState.Error("Ошибка при обновлении постов: ${e.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Ошибка при обновлении",
                showError = true,
                isRefreshing = false
            )
        }
    }

    fun clearState() {
        _postsState.value = PostsState.Idle
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null,
            showError = false
        )
        _postsState.value = PostsState.Idle
    }

    fun clearLoading() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            currentOperation = null
        )
    }

    sealed class PostsState {
        object Idle : PostsState()
        data class Success(val message: String) : PostsState()
        data class Error(val message: String) : PostsState()
    }

    data class PostsUiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val showError: Boolean = false,
        val currentOperation: String? = null
    )
}
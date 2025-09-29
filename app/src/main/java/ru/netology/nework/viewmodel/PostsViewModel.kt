package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.netology.nework.data.Post
import ru.netology.nework.repository.PostRepository
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    val data: Flow<PagingData<Post>> = repository.getPagingData().cachedIn(viewModelScope)

    fun likeById(id: Long) = viewModelScope.launch {
        try {
            repository.likeById(id)
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    fun dislikeById(id: Long) = viewModelScope.launch {
        try {
            repository.dislikeById(id)
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    fun save(post: Post) = viewModelScope.launch {
        try {
            repository.save(post)
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    suspend fun getPostById(id: Long): Post? {
        return repository.getById(id)
    }
}
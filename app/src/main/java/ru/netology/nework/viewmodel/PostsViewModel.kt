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
        repository.likeById(id)
    }

    fun dislikeById(id: Long) = viewModelScope.launch {

        repository.dislikeById(id)
    }

    fun removeById(id: Long) = viewModelScope.launch {
        repository.removeById(id)
    }

    fun save(post: Post) = viewModelScope.launch {
        repository.save(post)
    }
}
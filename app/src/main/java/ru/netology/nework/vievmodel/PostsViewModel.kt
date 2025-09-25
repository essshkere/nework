package ru.netology.nework.vievmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.netology.nework.dto.PostDto
import ru.netology.nework.repository.AuthRepository
import ru.netology.nework.repository.PostRepository

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {
    val data: Flow<PagingData<PostDto>> = repository.getPagingData().cachedIn(viewModelScope)

    fun likeById(id: Long) = viewModelScope.launch {
        repository.likeById(id)
    }
}
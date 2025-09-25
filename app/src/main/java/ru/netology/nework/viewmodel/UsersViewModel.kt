package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.netology.nework.data.User
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    val users: Flow<List<User>> = repository.getUsers()

    fun getUserById(id: Long) = viewModelScope.launch {
        repository.getUserById(id)
    }

    fun getJobs(userId: Long) = viewModelScope.launch {
        repository.getJobs(userId)
    }

    fun getUserWall(userId: Long) = viewModelScope.launch {
        repository.getUserWall(userId)
    }

    fun getMyWall() = viewModelScope.launch {
        repository.getMyWall()
    }
}
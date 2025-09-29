package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.netology.nework.data.Job
import ru.netology.nework.data.Post
import ru.netology.nework.data.User
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    init {
        loadUsers()
    }

    val usersFlow: Flow<List<User>> = repository.getUsers()

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                repository.getUsers().collect { userList ->
                    _users.value = userList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getUserById(id: Long): User? {
        return try {
            repository.getUserById(id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getJobs(userId: Long): List<Job> {
        return try {
            repository.getJobs(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getUserWall(userId: Long): List<Post> {
        return try {
            repository.getUserWall(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMyWall(): List<Post> {
        return try {
            repository.getMyWall()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
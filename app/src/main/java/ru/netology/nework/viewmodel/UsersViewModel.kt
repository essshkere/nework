package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
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

    val usersFlow: Flow<List<User>> = repository.getUsers()

    init {
        loadUsers()
    }

    fun getUserWallPaging(userId: Long): Flow<PagingData<Post>> {
        return repository.getUserWallPaging(userId)
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                repository.getUsers().collect {}
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
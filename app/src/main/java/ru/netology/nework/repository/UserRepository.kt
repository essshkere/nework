package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.data.Job
import ru.netology.nework.data.Post
import ru.netology.nework.data.User

interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun getUserById(id: Long): User?
    suspend fun getJobs(userId: Long): List<Job>
    suspend fun getUserWall(userId: Long): List<Post>
    suspend fun getMyWall(): List<Post>
    suspend fun saveUser(user: User)
}
package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.UserApi
import ru.netology.nework.dao.UserDao
import ru.netology.nework.data.Job
import ru.netology.nework.data.Post
import ru.netology.nework.data.User
import ru.netology.nework.mapper.toDto
import ru.netology.nework.mapper.toEntity
import ru.netology.nework.mapper.toModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao
) : UserRepository {

    override fun getUsers(): Flow<List<User>> {
        return userDao.getAll().map { users ->
            users.map { it.toModel() }
        }
    }

    override suspend fun getUserById(id: Long): User? {
        val cachedUser = userDao.getById(id)?.toModel()
        if (cachedUser != null) {
            return cachedUser
        }

        return try {
            val response = userApi.getById(id)
            if (response.isSuccessful) {
                response.body()?.let { userDto ->
                    val user = userDto.toEntity().toModel()
                    userDao.insert(userDto.toEntity())
                    user
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getJobs(userId: Long): List<Job> {
        return try {
            val response = userApi.getJobs(userId)
            if (response.isSuccessful) {
                response.body()?.map { it.toModel() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getUserWall(userId: Long): List<Post> {
        return try {
            val response = userApi.getWall(userId)
            if (response.isSuccessful) {
                response.body()?.map { it.toEntity().toModel() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMyWall(): List<Post> {
        return try {
            val response = userApi.getMyWall()
            if (response.isSuccessful) {
                response.body()?.map { it.toEntity().toModel() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveUser(user: User) {
        userDao.insert(user.toDto().toEntity())
    }
}
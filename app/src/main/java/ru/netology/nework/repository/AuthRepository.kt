package ru.netology.nework.repository

import ru.netology.nework.dto.LoginResponseDto

interface AuthRepository {
    suspend fun signIn(login: String, password: String): LoginResponseDto
    suspend fun signUp(login: String, password: String, name: String, avatarUri: String?): LoginResponseDto
    suspend fun logout()
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
    fun getUserId(): Long
    fun clearUserId()
    fun saveUserId(userId: Long)
}
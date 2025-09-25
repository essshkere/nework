package ru.netology.nework.repository

import ru.netology.nework.dto.LoginRequestDto
import ru.netology.nework.dto.LoginResponseDto
import ru.netology.nework.dto.RegisterRequestDto

interface AuthRepository {
    suspend fun signIn(login: String, password: String): LoginResponseDto
    suspend fun signUp(login: String, password: String, name: String): LoginResponseDto
    suspend fun logout()
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}
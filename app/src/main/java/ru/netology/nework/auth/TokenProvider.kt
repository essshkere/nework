package ru.netology.nework.auth

interface TokenProvider {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
    fun getUserId(): Long
    fun saveUserId(id: Long)
    fun clearUserId()
}

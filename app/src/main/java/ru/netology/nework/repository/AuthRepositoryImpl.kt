package ru.netology.nework.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Response
import ru.netology.nework.api.AuthApi
import ru.netology.nework.dto.LoginRequestDto
import ru.netology.nework.dto.LoginResponseDto
import ru.netology.nework.dto.RegisterRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }

    override suspend fun signIn(login: String, password: String): LoginResponseDto {
        val response = authApi.signIn(LoginRequestDto(login, password))
        if (!response.isSuccessful) {
            throw Exception("Authentication failed: ${response.code()}")
        }
        return response.body() ?: throw Exception("Empty response body")
    }

    override suspend fun signUp(login: String, password: String, name: String): LoginResponseDto {
        val response = authApi.signUp(RegisterRequestDto(login, password, name))
        if (!response.isSuccessful) {
            throw Exception("Registration failed: ${response.code()}")
        }
        return response.body() ?: throw Exception("Empty response body")
    }

    override suspend fun logout() {
        clearToken()
    }

    override fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    override fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
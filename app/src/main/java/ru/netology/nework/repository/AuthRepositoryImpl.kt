package ru.netology.nework.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import ru.netology.nework.api.AuthApi
import ru.netology.nework.dto.LoginResponseDto
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
        const val KEY_USER_ID = "user_id"
    }

    override suspend fun signIn(login: String, password: String): LoginResponseDto {
        val response = authApi.signIn(login, password)
        if (!response.isSuccessful) {
            throw Exception("Authentication failed: ${response.code()}")
        }
        val body = response.body() ?: throw Exception("Empty response body")
        saveToken(body.token)
        saveUserId(body.id)
        return body
    }

    override suspend fun signUp(
        login: String,
        password: String,
        name: String,
        avatarUri: String?
    ): LoginResponseDto {
        val loginPart = login.toRequestBody()
        val passwordPart = password.toRequestBody()
        val namePart = name.toRequestBody()
        val filePart = avatarUri?.let { uri ->
            // TODO: Implement file upload logic
            null
        }

        val response = authApi.signUp(loginPart, passwordPart, namePart, filePart)
        if (!response.isSuccessful) {
            throw Exception("Registration failed: ${response.code()}")
        }
        val body = response.body() ?: throw Exception("Empty response body")
        saveToken(body.token)
        saveUserId(body.id)
        return body
    }

    override suspend fun logout() {
        clearToken()
        clearUserId()
    }

    override fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    override fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 0L)
    }

    override fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    override fun clearUserId() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }
}
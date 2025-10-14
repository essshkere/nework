package ru.netology.nework.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.api.AuthApi
import ru.netology.nework.dto.LoginResponseDto
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
        const val MAX_AVATAR_SIZE = 5 * 1024 * 1024
    }

    override suspend fun signIn(login: String, password: String): LoginResponseDto {
        val response = authApi.signIn(login, password)
        if (!response.isSuccessful) {
            throw when (response.code()) {
                400 -> Exception("Неверный логин или пароль")
                404 -> Exception("Пользователь не найден")
                else -> Exception("Ошибка аутентификации: ${response.code()}")
            }
        }
        val body = response.body() ?: throw Exception("Пустой ответ от сервера")
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
        return withContext(Dispatchers.IO) {
            try {
                val loginPart = login.toRequestBody(MultipartBody.FORM)
                val passwordPart = password.toRequestBody(MultipartBody.FORM)
                val namePart = name.toRequestBody(MultipartBody.FORM)

                val filePart = avatarUri?.let { uriString ->
                    createAvatarPart(Uri.parse(uriString))
                }

                val response = authApi.signUp(loginPart, passwordPart, namePart, filePart)

                if (!response.isSuccessful) {
                    throw when (response.code()) {
                        403 -> Exception("Пользователь с таким логином уже зарегистрирован")
                        415 -> Exception("Неправильный формат изображения")
                        else -> Exception("Ошибка регистрации: ${response.code()}")
                    }
                }

                val body = response.body() ?: throw Exception("Пустой ответ от сервера")
                saveToken(body.token)
                saveUserId(body.id)
                body
            } catch (e: Exception) {
                throw when {
                    e.message?.contains("Unable to resolve host") == true ->
                        Exception("Ошибка сети. Проверьте подключение к интернету")
                    e.message?.contains("Размер файла") == true ->
                        Exception("Размер файла превышает 5 МБ")
                    else -> e
                }
            }
        }
    }

    private fun createAvatarPart(avatarUri: Uri): MultipartBody.Part {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(avatarUri)
            val file = createTempFileFromUri(avatarUri, inputStream)
            if (file.length() > MAX_AVATAR_SIZE) {
                file.delete()
                throw Exception("Размер файла превышает 5 МБ")
            }

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("file", file.name, requestFile)
        } catch (e: Exception) {
            throw Exception("Ошибка обработки изображения: ${e.message}")
        }
    }

    private fun createTempFileFromUri(uri: Uri, inputStream: InputStream?): File {
        val file = File.createTempFile("avatar", ".jpg", context.cacheDir)
        file.deleteOnExit()

        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Не удалось открыть файл")

        return file
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
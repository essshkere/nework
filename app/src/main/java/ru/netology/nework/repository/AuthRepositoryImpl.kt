package ru.netology.nework.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.AuthApi
import ru.netology.nework.auth.TokenProvider
import ru.netology.nework.dto.LoginResponseDto
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenProvider: TokenProvider,
    @ApplicationContext private val context: Context
) : AuthRepository {

    companion object {
        private const val MAX_AVATAR_SIZE = 5 * 1024 * 1024
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
        println("DEBUG: Получен токен: ${body.token.take(20)}...")
        println("DEBUG: Получен ID пользователя: ${body.id}")
        saveToken(body.token)
        saveUserId(body.id)
        println("DEBUG: Токен сохранен в SharedPreferences")
        println("DEBUG: Проверка сохраненного токена: ${getToken()?.take(20)}...")
        return body
    }

    override suspend fun signUp(
        login: String,
        password: String,
        name: String,
        avatarUri: String?
    ): LoginResponseDto = withContext(Dispatchers.IO) {
        val filePart = avatarUri?.let { createAvatarPart(Uri.parse(it)) }
        val response = authApi.signUp(login, password, name, filePart)
        if (!response.isSuccessful) {
            throw when (response.code()) {
                403 -> Exception("Пользователь с таким логином уже зарегистрирован")
                415 -> Exception("Неправильный формат изображения")
                else -> Exception("Ошибка регистрации: ${response.code()}")
            }
        }
        val body = response.body() ?: throw Exception("Пустой ответ от сервера")
        tokenProvider.saveToken(body.token)
        tokenProvider.saveUserId(body.id)
        body
    }

    private fun createAvatarPart(avatarUri: Uri): MultipartBody.Part {
        val inputStream: InputStream? = context.contentResolver.openInputStream(avatarUri)
        val file = createTempFileFromUri(avatarUri, inputStream)
        if (file.length() > MAX_AVATAR_SIZE) {
            file.delete()
            throw Exception("Размер файла превышает 5 МБ")
        }
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", file.name, requestFile)
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
        tokenProvider.clearToken()
        tokenProvider.clearUserId()
    }

    override fun getToken(): String? = tokenProvider.getToken()
    override fun saveToken(token: String) = tokenProvider.saveToken(token)
    override fun clearToken() = tokenProvider.clearToken()
    override fun getUserId(): Long = tokenProvider.getUserId()
    override fun saveUserId(userId: Long) = tokenProvider.saveUserId(userId)
    override fun clearUserId() = tokenProvider.clearUserId()
}

package ru.netology.nework.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProviderImpl @Inject constructor(
    @ApplicationContext context: Context
) : TokenProvider {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
    }

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    override fun getUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)
    override fun saveUserId(id: Long) { prefs.edit().putLong(KEY_USER_ID, id).apply() }
    override fun clearUserId() { prefs.edit().remove(KEY_USER_ID).apply() }
}

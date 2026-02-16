package ru.netology.nework.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        requestBuilder.addHeader("Api-Key", "c1378193-bc0e-42c8-a502-b8d66d189617")
        val token = tokenProvider.getToken()
        println("DEBUG: AuthInterceptor - токен: ${token?.take(20)}...")
        token?.let {
            requestBuilder.addHeader("Authorization", it)
            println("DEBUG: AuthInterceptor - добавляем заголовок Authorization с токеном (без Bearer)")
        } ?: run {
            println("DEBUG: AuthInterceptor - токен отсутствует")
        }
        val request = requestBuilder.build()
        println("DEBUG: Отправляем запрос на: ${request.url}")
        println("DEBUG: Заголовки: ${request.headers}")
        return chain.proceed(request)
    }
}
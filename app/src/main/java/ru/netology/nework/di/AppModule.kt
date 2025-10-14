package ru.netology.nework.di

import android.content.Context
import android.content.Intent
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.netology.nework.App
import ru.netology.nework.api.*
import ru.netology.nework.data.AppDatabase
import ru.netology.nework.repository.*
import javax.inject.Singleton
import okhttp3.Interceptor
import ru.netology.nework.viewmodel.AuthViewModel
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val BASE_URL = "http://94.228.125.136:8080/"
    @Provides
    @Singleton
    fun provideOkHttpClient(authRepository: AuthRepository): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Api-Key", "c1378193-bc0e-42c8-a502-b8d66d189617")
            authRepository.getToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun providePostApi(retrofit: Retrofit): PostApi = retrofit.create(PostApi::class.java)

    @Provides
    @Singleton
    fun provideEventApi(retrofit: Retrofit): EventApi = retrofit.create(EventApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideMediaApi(retrofit: Retrofit): MediaApi = retrofit.create(MediaApi::class.java)

    @Provides
    @Singleton
    fun provideJobApi(retrofit: Retrofit): JobApi = retrofit.create(JobApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun providePostRepository(postApi: PostApi, db: AppDatabase): PostRepository =
        PostRepositoryImpl(postApi, db.postDao())

    @Provides
    @Singleton
    fun provideEventRepository(eventApi: EventApi, db: AppDatabase): EventRepository =
        EventRepositoryImpl(eventApi, db.eventDao())

    @Provides
    @Singleton
    fun provideUserRepository(userApi: UserApi, db: AppDatabase): UserRepository =
        UserRepositoryImpl(userApi, db.userDao())

    @Provides
    @Singleton
    fun provideAuthRepository(authApi: AuthApi, @ApplicationContext context: Context): AuthRepository =
        AuthRepositoryImpl(authApi, context)

    @Provides
    @Singleton
    fun provideJobRepository(jobApi: JobApi): JobRepository = JobRepositoryImpl(jobApi)
}
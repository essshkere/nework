package ru.netology.nework.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.netology.nework.api.AuthApi
import ru.netology.nework.api.EventApi
import ru.netology.nework.api.PostApi
import ru.netology.nework.api.UserApi
import ru.netology.nework.data.AppDatabase
import ru.netology.nework.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://94.228.125.136:8080/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authRepository: AuthRepository): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Api-Key", "ваш-api-key-здесь")


            authRepository.getToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

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
}
package ru.netology.nework.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.adapter.ParticipantAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.api.*
import ru.netology.nework.auth.AuthInterceptor
import ru.netology.nework.auth.TokenProvider
import ru.netology.nework.auth.TokenProviderImpl
import ru.netology.nework.data.AppDatabase
import ru.netology.nework.repository.*
import javax.inject.Singleton

private const val BASE_URL = "http://94.228.125.136:8080/"

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenProvider(@ApplicationContext context: Context): TokenProvider =
        TokenProviderImpl(context)

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: TokenProvider): Interceptor =
        AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

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
    fun providePostRepository(
        postApi: PostApi,
        userApi: UserApi,
        mediaApi: MediaApi,
        db: AppDatabase
    ): PostRepository = PostRepositoryImpl(postApi, userApi, db.postDao(), mediaApi)

    @Provides
    @Singleton
    fun provideEventRepository(
        eventApi: EventApi,
        mediaApi: MediaApi,
        db: AppDatabase
    ): EventRepository = EventRepositoryImpl(eventApi, db.eventDao(), mediaApi)

    @Provides
    @Singleton
    fun provideUserRepository(userApi: UserApi, db: AppDatabase): UserRepository =
        UserRepositoryImpl(userApi, db.userDao())

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        tokenProvider: TokenProvider,
        @ApplicationContext context: Context
    ): AuthRepository = AuthRepositoryImpl(authApi, tokenProvider, context)

    @Provides
    @Singleton
    fun provideJobRepository(jobApi: JobApi): JobRepository = JobRepositoryImpl(jobApi)

    @Provides
    @Singleton
    fun provideParticipantAdapter(): ParticipantAdapter = ParticipantAdapter()

    @Provides
    @Singleton
    fun provideEventAdapter(): EventAdapter = EventAdapter()

    @Provides
    @Singleton
    fun provideUserAdapter(): UserAdapter = UserAdapter()
}

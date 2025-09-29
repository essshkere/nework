package ru.netology.nework.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dao.UserDao

@Database(
    entities = [PostEntity::class, EventEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
   abstract fun eventDao(): EventDao
    abstract fun userDao(): UserDao
}
package ru.netology.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.data.EventEntity
import ru.netology.nework.data.PostEntity

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY datetime DESC")
    fun pagingSource(): PagingSource<Int, EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clear()

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun removeById(id: Long)
}
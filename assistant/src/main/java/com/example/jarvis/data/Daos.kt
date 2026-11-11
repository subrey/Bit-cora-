package com.example.jarvis.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY done ASC, createdAt DESC")
    fun observeAll(): Flow<List<TodoItem>>

    @Query("SELECT COUNT(*) FROM todos WHERE done = 0")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insert(item: TodoItem): Long

    @Update
    suspend fun update(item: TodoItem)

    @Delete
    suspend fun delete(item: TodoItem)
}

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): MemoryItem?

    @Insert
    suspend fun insert(item: MemoryItem): Long

    @Delete
    suspend fun delete(item: MemoryItem)
}

@Dao
interface TimerDao {

    @Query("SELECT * FROM timers ORDER BY fired ASC, createdAt DESC")
    fun observeAll(): Flow<List<TimerItem>>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getById(id: Long): TimerItem?

    @Insert
    suspend fun insert(item: TimerItem): Long

    @Update
    suspend fun update(item: TimerItem)

    @Delete
    suspend fun delete(item: TimerItem)
}

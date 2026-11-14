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

    @Query("SELECT * FROM timers WHERE fired = 0 ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextActive(): TimerItem?

    @Insert
    suspend fun insert(item: TimerItem): Long

    @Update
    suspend fun update(item: TimerItem)

    @Delete
    suspend fun delete(item: TimerItem)
}

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY done ASC, triggerAt ASC")
    fun observeAll(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderItem?

    @Query("SELECT * FROM reminders WHERE done = 0 ORDER BY triggerAt ASC LIMIT 1")
    suspend fun nextPending(): ReminderItem?

    @Query("SELECT COUNT(*) FROM reminders WHERE done = 0")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insert(item: ReminderItem): Long

    @Update
    suspend fun update(item: ReminderItem)

    @Delete
    suspend fun delete(item: ReminderItem)
}

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatEntry>>

    @Query("SELECT COUNT(*) FROM chat")
    suspend fun count(): Int

    @Query("SELECT * FROM chat WHERE fromUser = 1 ORDER BY id DESC LIMIT :limit")
    suspend fun lastUserMessages(limit: Int): List<ChatEntry>

    @Query("SELECT * FROM chat ORDER BY id DESC LIMIT :limit")
    suspend fun lastMessages(limit: Int): List<ChatEntry>

    @Insert
    suspend fun insert(entry: ChatEntry): Long

    @Query("DELETE FROM chat")
    suspend fun clear()

    @Query("DELETE FROM chat WHERE id NOT IN (SELECT id FROM chat ORDER BY id DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}

package com.example.jarvis.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class JarvisRepository(
    private val todoDao: TodoDao,
    private val memoryDao: MemoryDao,
    private val timerDao: TimerDao,
) {

    fun observeTodos(): Flow<List<TodoItem>> = todoDao.observeAll()

    fun observePendingCount(): Flow<Int> = todoDao.observePendingCount()

    fun observeMemories(): Flow<List<MemoryItem>> = memoryDao.observeAll()

    fun observeTimers(): Flow<List<TimerItem>> = timerDao.observeAll()

    suspend fun addTodo(text: String): Long = todoDao.insert(TodoItem(text = text))

    suspend fun toggleTodo(item: TodoItem) = todoDao.update(item.copy(done = !item.done))

    suspend fun deleteTodo(item: TodoItem) = todoDao.delete(item)

    suspend fun addMemory(text: String): Long = memoryDao.insert(MemoryItem(text = text))

    suspend fun deleteMemory(item: MemoryItem) = memoryDao.delete(item)

    suspend fun latestMemory(): MemoryItem? = memoryDao.latest()

    suspend fun addTimer(label: String, seconds: Int): Long =
        timerDao.insert(TimerItem(label = label, durationSeconds = seconds))

    suspend fun getTimer(id: Long): TimerItem? = timerDao.getById(id)

    suspend fun markTimerFired(item: TimerItem) = timerDao.update(item.copy(fired = true))

    suspend fun deleteTimer(item: TimerItem) = timerDao.delete(item)

    // --- Recuentos para el resumen del asistente -------------------------

    suspend fun pendingTodoCount(): Int = todoDao.observePendingCount().first()

    suspend fun memoryCount(): Int = memoryDao.observeAll().first().size

    suspend fun activeTimerCount(): Int = timerDao.observeAll().first().count { !it.fired }
}

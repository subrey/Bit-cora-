package com.example.bitacora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC, id ASC")
    fun observeByNote(noteId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC, id ASC")
    suspend fun getByNote(noteId: Long): List<Attachment>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): Attachment?

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Insert
    suspend fun insertAll(attachments: List<Attachment>)

    @Delete
    suspend fun delete(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteByNote(noteId: Long)
}

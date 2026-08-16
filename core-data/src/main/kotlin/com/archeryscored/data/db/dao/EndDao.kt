package com.archeryscored.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.archeryscored.data.db.entity.EndEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EndDao {
    @Query("SELECT * FROM ends WHERE sessionId = :sessionId ORDER BY endNumber ASC")
    fun getEndsForSession(sessionId: Long): Flow<List<EndEntity>>

    @Query("SELECT * FROM ends WHERE id = :endId")
    suspend fun getById(endId: Long): EndEntity?

    @Insert
    suspend fun insert(end: EndEntity): Long

    @Update
    suspend fun update(end: EndEntity)

    @Delete
    suspend fun delete(end: EndEntity)
}

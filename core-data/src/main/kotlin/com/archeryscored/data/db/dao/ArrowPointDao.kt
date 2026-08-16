package com.archeryscored.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.archeryscored.data.db.entity.ArrowPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArrowPointDao {
    @Query("SELECT * FROM arrow_points WHERE endId = :endId ORDER BY id ASC")
    fun getPointsForEnd(endId: Long): Flow<List<ArrowPointEntity>>

    @Query(
        """
        SELECT arrow_points.* FROM arrow_points
        INNER JOIN ends ON arrow_points.endId = ends.id
        WHERE ends.sessionId = :sessionId
        """
    )
    fun getPointsForSession(sessionId: Long): Flow<List<ArrowPointEntity>>

    @Insert
    suspend fun insertAll(points: List<ArrowPointEntity>)

    @Query("DELETE FROM arrow_points WHERE endId = :endId")
    suspend fun deleteByEnd(endId: Long)
}

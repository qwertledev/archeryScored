package com.archeryscored.data.repository

import android.net.Uri
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.db.entity.EndEntity
import com.archeryscored.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.io.File

interface SessionRepository {
    fun getAllSessions(): Flow<List<SessionEntity>>
    fun getSession(sessionId: Long): Flow<SessionEntity?>
    suspend fun createSession(label: String?, targetFaceTypeId: String, distanceMeters: Float?): Long
    suspend fun endSession(sessionId: Long)
    suspend fun updateSessionLabel(sessionId: Long, label: String?)
    suspend fun deleteSession(sessionId: Long)

    fun getEndsForSession(sessionId: Long): Flow<List<EndEntity>>
    fun newPhotoFile(sessionId: Long, endNumber: Int): File
    fun importUploadedPhoto(sourceUri: Uri, destFile: File)
    /** [photoFile] is null for an end entered manually with no photo. */
    suspend fun createEnd(sessionId: Long, endNumber: Int, photoFile: File?, capturedAt: Instant): Long
    suspend fun updateCalibration(endId: Long, centerXPx: Float, centerYPx: Float, radiusPx: Float, confirmed: Boolean)

    fun getPointsForEnd(endId: Long): Flow<List<ArrowPointEntity>>
    fun getPointsForSession(sessionId: Long): Flow<List<ArrowPointEntity>>
    suspend fun saveArrowPoints(endId: Long, points: List<ArrowPointEntity>)
}

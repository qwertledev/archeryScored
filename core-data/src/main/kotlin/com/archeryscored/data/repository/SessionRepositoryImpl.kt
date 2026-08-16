package com.archeryscored.data.repository

import android.net.Uri
import com.archeryscored.data.db.dao.ArrowPointDao
import com.archeryscored.data.db.dao.EndDao
import com.archeryscored.data.db.dao.SessionDao
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.db.entity.EndEntity
import com.archeryscored.data.db.entity.SessionEntity
import com.archeryscored.data.storage.PhotoStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.File

class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val endDao: EndDao,
    private val arrowPointDao: ArrowPointDao,
    private val photoStorage: PhotoStorage
) : SessionRepository {

    override fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAll()

    override fun getSession(sessionId: Long): Flow<SessionEntity?> = sessionDao.getById(sessionId)

    override suspend fun createSession(label: String?, targetFaceTypeId: String, distanceMeters: Float?): Long =
        sessionDao.insert(
            SessionEntity(
                targetFaceTypeId = targetFaceTypeId,
                distanceMeters = distanceMeters,
                createdAt = Clock.System.now(),
                label = label?.trim()?.ifBlank { null }
            )
        )

    override suspend fun endSession(sessionId: Long) {
        sessionDao.setEndedAt(sessionId, Clock.System.now())
    }

    override suspend fun updateSessionLabel(sessionId: Long, label: String?) {
        sessionDao.setLabel(sessionId, label?.trim()?.ifBlank { null })
    }

    override suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteById(sessionId)
        photoStorage.deletePhotosForSession(sessionId)
    }

    override fun getEndsForSession(sessionId: Long): Flow<List<EndEntity>> = endDao.getEndsForSession(sessionId)

    override fun newPhotoFile(sessionId: Long, endNumber: Int): File =
        photoStorage.fileForEnd(sessionId, endNumber, Clock.System.now().toEpochMilliseconds())

    override fun importUploadedPhoto(sourceUri: Uri, destFile: File) {
        photoStorage.importUploadedPhoto(sourceUri, destFile)
    }

    override suspend fun createEnd(sessionId: Long, endNumber: Int, photoFile: File, capturedAt: Instant): Long =
        endDao.insert(
            EndEntity(
                sessionId = sessionId,
                endNumber = endNumber,
                photoPath = photoStorage.relativePath(photoFile),
                capturedAt = capturedAt
            )
        )

    override suspend fun updateCalibration(
        endId: Long,
        centerXPx: Float,
        centerYPx: Float,
        radiusPx: Float,
        confirmed: Boolean
    ) {
        val end = endDao.getById(endId) ?: return
        endDao.update(
            end.copy(
                centerXPx = centerXPx,
                centerYPx = centerYPx,
                radiusPx = radiusPx,
                calibrationConfirmed = confirmed
            )
        )
    }

    override fun getPointsForEnd(endId: Long): Flow<List<ArrowPointEntity>> = arrowPointDao.getPointsForEnd(endId)

    override fun getPointsForSession(sessionId: Long): Flow<List<ArrowPointEntity>> =
        arrowPointDao.getPointsForSession(sessionId)

    override suspend fun saveArrowPoints(endId: Long, points: List<ArrowPointEntity>) {
        arrowPointDao.deleteByEnd(endId)
        arrowPointDao.insertAll(points)
    }
}

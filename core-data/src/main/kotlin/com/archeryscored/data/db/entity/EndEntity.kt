package com.archeryscored.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "ends",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class EndEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val endNumber: Int,
    /** Relative to the app's private files dir, e.g. "photos/session_3/end_1_1699999999.jpg". */
    val photoPath: String,
    val capturedAt: Instant,
    val centerXPx: Float? = null,
    val centerYPx: Float? = null,
    val radiusPx: Float? = null,
    val calibrationConfirmed: Boolean = false
)

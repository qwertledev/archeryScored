package com.archeryscored.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetFaceTypeId: String,
    val distanceMeters: Float?,
    val createdAt: Instant,
    /** Null while the session is still in progress and can be resumed; set once the archer finishes it. */
    val endedAt: Instant? = null,
    val notes: String? = null
)

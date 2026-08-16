package com.archeryscored.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetFaceTypeId: String,
    val distanceMeters: Float?,
    /** The session's display name is derived from this (day/time) plus [label] - never stored redundantly. */
    val createdAt: Instant,
    /** Null while the session is still in progress and can be resumed; set once the archer finishes it. */
    val endedAt: Instant? = null,
    /** Optional archer-entered text appended to the auto day/time name; editable after creation. */
    val label: String? = null
)

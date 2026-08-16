package com.archeryscored.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.archeryscored.model.PointSource

/**
 * Stored in normalized coordinates ((px - centerPx) / radiusPx from the owning [EndEntity]) rather
 * than raw pixels, so the session-wide grouping chart can overlay arrows from different photos
 * without needing to reconcile per-photo resolution or calibration differences.
 *
 * [xNormalized]/[yNormalized] are null for a manually-entered score with no photo - there's no
 * position to plot, only a score. Consumers of the grouping chart must skip those.
 */
@Entity(
    tableName = "arrow_points",
    foreignKeys = [
        ForeignKey(
            entity = EndEntity::class,
            parentColumns = ["id"],
            childColumns = ["endId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("endId")]
)
data class ArrowPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endId: Long,
    val xNormalized: Float?,
    val yNormalized: Float?,
    val score: Int,
    val isX: Boolean = false,
    val source: PointSource,
    val spotIndex: Int = 0
)

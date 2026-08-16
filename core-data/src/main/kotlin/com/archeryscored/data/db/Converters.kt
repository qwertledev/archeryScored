package com.archeryscored.data.db

import androidx.room.TypeConverter
import com.archeryscored.model.PointSource
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun fromPointSource(value: String?): PointSource? = value?.let { PointSource.valueOf(it) }

    @TypeConverter
    fun toPointSource(source: PointSource?): String? = source?.name
}

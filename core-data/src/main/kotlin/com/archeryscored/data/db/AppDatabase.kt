package com.archeryscored.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.archeryscored.data.db.dao.ArrowPointDao
import com.archeryscored.data.db.dao.EndDao
import com.archeryscored.data.db.dao.SessionDao
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.db.entity.EndEntity
import com.archeryscored.data.db.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, EndEntity::class, ArrowPointEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun endDao(): EndDao
    abstract fun arrowPointDao(): ArrowPointDao

    companion object {
        const val DATABASE_NAME = "archery_scored.db"
    }
}

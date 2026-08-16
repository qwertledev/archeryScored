package com.archeryscored.app.di

import android.content.Context
import androidx.room.Room
import com.archeryscored.data.db.AppDatabase
import com.archeryscored.data.db.dao.ArrowPointDao
import com.archeryscored.data.db.dao.EndDao
import com.archeryscored.data.db.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Pre-1.0, no shipped data worth preserving across schema changes - a real
            // migration is worth writing once this app has users with data to keep.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideEndDao(db: AppDatabase): EndDao = db.endDao()

    @Provides
    fun provideArrowPointDao(db: AppDatabase): ArrowPointDao = db.arrowPointDao()
}

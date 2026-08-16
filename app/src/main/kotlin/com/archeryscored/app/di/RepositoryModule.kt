package com.archeryscored.app.di

import android.content.Context
import com.archeryscored.data.db.dao.ArrowPointDao
import com.archeryscored.data.db.dao.EndDao
import com.archeryscored.data.db.dao.SessionDao
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.data.repository.SessionRepositoryImpl
import com.archeryscored.data.storage.PhotoStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePhotoStorage(@ApplicationContext context: Context): PhotoStorage = PhotoStorage(context)

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        endDao: EndDao,
        arrowPointDao: ArrowPointDao,
        photoStorage: PhotoStorage
    ): SessionRepository = SessionRepositoryImpl(sessionDao, endDao, arrowPointDao, photoStorage)
}

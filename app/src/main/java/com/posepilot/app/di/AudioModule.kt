package com.posepilot.app.di

import android.content.Context
import com.posepilot.app.data.audio.AudioCoachingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioCoachingManager(
        @ApplicationContext context: Context
    ): AudioCoachingManager {
        return AudioCoachingManager(context)
    }
}

package com.posepilot.app.di

import android.content.Context
import com.posepilot.app.data.pose.PoseLandmarkerHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PoseModule {

    @Provides
    @Singleton
    fun providePoseLandmarkerHelper(
        @ApplicationContext context: Context
    ): PoseLandmarkerHelper {
        return PoseLandmarkerHelper(context)
    }
}

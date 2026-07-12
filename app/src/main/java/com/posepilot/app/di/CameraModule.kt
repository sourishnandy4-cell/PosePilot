package com.posepilot.app.di

import android.content.Context
import com.posepilot.app.data.camera.CameraManager
import com.posepilot.app.data.pose.PoseLandmarkerHelper
import com.posepilot.app.data.sensor.RotationSensorListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context,
        poseLandmarkerHelper: PoseLandmarkerHelper
    ): CameraManager {
        return CameraManager(context, poseLandmarkerHelper)
    }

    @Provides
    @Singleton
    fun provideRotationSensorListener(
        @ApplicationContext context: Context
    ): RotationSensorListener {
        return RotationSensorListener(context)
    }
}

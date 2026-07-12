package com.posepilot.app.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class RotationSensorListenerTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var mockSensor: Sensor
    private lateinit var listener: RotationSensorListener

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        sensorManager = mock(SensorManager::class.java)
        mockSensor = mock(Sensor::class.java)

        `when`(context.getSystemService(Context.SENSOR_SERVICE)).thenReturn(sensorManager)
        `when`(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)).thenReturn(mockSensor)

        listener = RotationSensorListener(context)
    }

    @Test
    fun testStartListeningRegistersListener() {
        listener.startListening()
        verify(sensorManager).registerListener(listener, mockSensor, SensorManager.SENSOR_DELAY_UI)
    }

    @Test
    fun testStopListeningUnregistersListener() {
        listener.startListening()
        listener.stopListening()
        verify(sensorManager).unregisterListener(listener)
    }
}

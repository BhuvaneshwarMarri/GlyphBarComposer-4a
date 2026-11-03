package com.maari.glyphbarcomposer.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class EnhancedGestureManager(context: Context, private val onTrigger: () -> Unit) :
    SensorEventListener {
    private var sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var shakeThreshold = 8f
    private var lastShakeTime = 0L

    private var lastY = 0f
    private var liftThreshold = 3.5f
    private var lastLiftTime = 0L

    private var lastTapTime = 0L
    private var tapCount = 0
    private var tapThreshold = 14f

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val now = System.currentTimeMillis()

        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt(x * x + y * y + z * z)
        val delta = currentAcceleration - lastAcceleration

        if (delta > shakeThreshold) {
            if (now - lastShakeTime > 2000) {
                lastShakeTime = now
                onTrigger()
                return
            }
        }

        val yDelta = y - lastY
        if (yDelta > liftThreshold && y > 6f) {
            if (now - lastLiftTime > 3000) {
                lastLiftTime = now
                onTrigger()
            }
        }
        lastY = y

        if (delta > tapThreshold) {
            if (now - lastTapTime < 500) {
                tapCount++
                if (tapCount >= 1) {
                    onTrigger()
                    tapCount = 0
                    lastTapTime = 0
                }
            } else {
                tapCount = 0
            }
            lastTapTime = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

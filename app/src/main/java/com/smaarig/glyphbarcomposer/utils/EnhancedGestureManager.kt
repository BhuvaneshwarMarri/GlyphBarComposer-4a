package com.smaarig.glyphbarcomposer.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class EnhancedGestureManager(context: Context, private val onTrigger: () -> Unit) : SensorEventListener {
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // ── Shake Settings ───────────────────────────────────────────────────
    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var shakeThreshold = 8f // Lowered from 12f for gentler trigger
    private var lastShakeTime = 0L

    // ── Lift Settings ────────────────────────────────────────────────────
    private var lastY = 0f
    private var liftThreshold = 3.5f
    private var lastLiftTime = 0L

    // ── Tap Settings ─────────────────────────────────────────────────────
    private var lastTapTime = 0L
    private var tapCount = 0
    private var tapThreshold = 14f // Sharp spike for tap detection

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

        // 1. Detect Shake (Gentle)
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

        // 2. Detect Lift (Upward tilt/movement)
        // Detect sudden change in Y-axis (vertical tilt)
        val yDelta = y - lastY
        if (yDelta > liftThreshold && y > 6f) { // Moving from flatish to upright
            if (now - lastLiftTime > 3000) {
                lastLiftTime = now
                onTrigger()
            }
        }
        lastY = y

        // 3. Detect Double Tap (Sharp Spikes)
        if (delta > tapThreshold) {
            if (now - lastTapTime < 500) {
                tapCount++
                if (tapCount >= 1) { // Second spike within 500ms
                    onTrigger()
                    tapCount = 0
                    lastTapTime = 0
                }
            } else {
                tapCount = 0 // Reset if too slow
            }
            lastTapTime = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

data class AirMouseTelemetry(
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val deltaX: Int = 0,
    val deltaY: Int = 0,
    val isActive: Boolean = false
)

class AirMouseSensorManager(
    context: Context,
    private val onMotionDelta: (dx: Int, dy: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscopeSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _telemetry = MutableStateFlow(AirMouseTelemetry())
    val telemetry: StateFlow<AirMouseTelemetry> = _telemetry.asStateFlow()

    private var isListening = false

    var sensitivity: Float = 2.2f
    var deadband: Float = 0.04f
    var smoothing: Float = 0.65f

    // Exponential moving average filter states
    private var filteredVx = 0f
    private var filteredVy = 0f

    val isSupported: Boolean
        get() = gyroscopeSensor != null

    fun start() {
        if (isListening || gyroscopeSensor == null) return
        resetFilters()
        sensorManager?.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
        isListening = true
        _telemetry.value = _telemetry.value.copy(isActive = true)
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        resetFilters()
        _telemetry.value = _telemetry.value.copy(isActive = false, deltaX = 0, deltaY = 0)
    }

    private fun resetFilters() {
        filteredVx = 0f
        filteredVy = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isListening) return

        // Gyroscope outputs angular velocity in rad/s:
        // event.values[0] -> X axis (Pitch: tilting top up/down)
        // event.values[1] -> Y axis (Roll: tilting left/right)
        // event.values[2] -> Z axis (Yaw: turning compass direction left/right)
        val rawPitch = event.values[0]
        val rawRoll = event.values[1]
        val rawYaw = event.values[2]

        // When held in hand pointing at a tablet:
        // Turning wrist left/right combines yaw (Z) and roll (Y) depending on tilt.
        // Tilting up/down is pitch (X).
        val rawVx = -(rawYaw * 0.75f + rawRoll * 0.45f)
        val rawVy = -rawPitch

        // Apply deadband
        val deadbandVx = if (abs(rawVx) < deadband) 0f else rawVx - (if (rawVx > 0) deadband else -deadband)
        val deadbandVy = if (abs(rawVy) < deadband) 0f else rawVy - (if (rawVy > 0) deadband else -deadband)

        // Low-pass exponential smoothing
        val alpha = (1.0f - smoothing).coerceIn(0.1f, 1.0f)
        filteredVx = alpha * deadbandVx + (1.0f - alpha) * filteredVx
        filteredVy = alpha * deadbandVy + (1.0f - alpha) * filteredVy

        // Scale by sensitivity
        val deltaX = (filteredVx * sensitivity * 16f).roundToInt()
        val deltaY = (filteredVy * sensitivity * 16f).roundToInt()

        if (deltaX != 0 || deltaY != 0) {
            onMotionDelta(deltaX, deltaY)
        }

        _telemetry.value = AirMouseTelemetry(
            gyroX = rawPitch,
            gyroY = rawRoll,
            gyroZ = rawYaw,
            deltaX = deltaX,
            deltaY = deltaY,
            isActive = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

package com.coreproc.kotlin.kotlinbase.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.coreproc.kotlin.kotlinbase.extensions.showShortToast
import com.coreproc.kotlin.kotlinbase.misc.AppPreferences
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SensorDetectorService : Service() {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mGyroscope: Sensor
    private var sensorData = ""

    private var sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            processData(event)
        }

        override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
        }

    }

    private fun processData(event: SensorEvent?) {
        event?.let {
            val sensorName: String = it.sensor!!.name;

            val timestamp = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                .withZone(ZoneOffset.systemDefault())
                .format(Instant.now())

            val modifiedSensorName = if (sensorName.contains("Accel")) "Accelerometer"
                else if (sensorName.contains("Gyro")) "Gyroscope"
                else sensorName

            // Save to prefs
            Log.e(
                "Sensor",
                "TS: $timestamp = Name: $modifiedSensorName X: ${it.values[0]} Y: ${it.values[1]} Z: ${it.values[2]};"
            )

            // VALUES
            // 0 - X, 1 - Y, 2 - Z
            sensorData += "$timestamp,$modifiedSensorName,${it.values[0]},${it.values[1]},${it.values[2]}\n"

        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        showShortToast("Sensor started.")
        Log.e("SENSOR SERVICE", "STARTED")
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        mSensorManager.registerListener(
            sensorEventListener,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        mSensorManager.registerListener(
            sensorEventListener,
            mGyroscope,
            SensorManager.SENSOR_DELAY_UI
        )
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(sensorEventListener)
        mSensorManager.flush(sensorEventListener)
        AppPreferences.saveSensorData(sensorData)
        sensorData = ""
        showShortToast("Service stopped.")
        Log.e("SENSOR SERVICE", "STOPPED")
    }
}
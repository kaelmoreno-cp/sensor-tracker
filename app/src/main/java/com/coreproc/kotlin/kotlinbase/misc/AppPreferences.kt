package com.coreproc.kotlin.kotlinbase.misc

import android.content.Context
import com.coreproc.kotlin.kotlinbase.App

object AppPreferences {
    private val API_KEY = "API_KEY"
    private val SENSOR_DATA = "SENSOR_DATA"

    private fun logout() {
        val prefs = App.instance!!.getSharedPreferences(App.instance!!.packageName, Context.MODE_PRIVATE)
        prefs.edit().putString(API_KEY, "").apply()
    }

    fun saveApiKey(key: String) {
        val prefs = App.instance!!.getSharedPreferences(App.instance!!.packageName, Context.MODE_PRIVATE)
        prefs.edit().putString(API_KEY, key).apply()
    }

    fun getApiKey(): String? {
        return try {
            val prefs = App.instance!!.getSharedPreferences(App.instance!!.packageName, Context.MODE_PRIVATE)
            return prefs.getString(API_KEY, "")
        } catch (ex: Exception) {
            null
        }
    }

    fun saveSensorData(data: String) {
        val prefs = App.instance!!.getSharedPreferences(App.instance!!.packageName, Context.MODE_PRIVATE)
        prefs.edit().putString(SENSOR_DATA, data).apply()
    }

    fun getSensorData(): String {
        return try {
            val prefs = App.instance!!.getSharedPreferences(App.instance!!.packageName, Context.MODE_PRIVATE)
            return prefs.getString(SENSOR_DATA, "No Data")!!
        } catch (ex: Exception) {
            "No Data"
        }
    }
}
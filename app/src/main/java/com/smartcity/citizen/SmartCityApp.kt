package com.smartcity.citizen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.smartcity.citizen.util.NotificationHelper

class SmartCityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationHelper.CHANNEL_ID,
                "SortIt Eco Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily sustainability prompts and waste sorting updates"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

}
package com.birthday.friends

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.birthday.friends.alarm.DailyCheckWorker

class BirthdayApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // 每日 fallback 检查（WorkManager），补救被 EMUI 杀掉的 Alarm
        DailyCheckWorker.enqueue(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                CHANNEL_ID_BIRTHDAY,
                "生日纪念日提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "生日、纪念日到来前的提醒通知"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_BIRTHDAY = "birthday_reminder"
    }
}

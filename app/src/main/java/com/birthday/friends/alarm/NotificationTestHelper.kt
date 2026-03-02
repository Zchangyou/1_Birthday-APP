package com.birthday.friends.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.birthday.friends.BirthdayApp
import com.birthday.friends.R

object NotificationTestHelper {

    fun sendTestNow(context: Context) {
        val notification = NotificationCompat.Builder(context, BirthdayApp.CHANNEL_ID_BIRTHDAY)
            .setSmallIcon(R.drawable.ic_cake)
            .setContentTitle("测试提醒")
            .setContentText("如果你看到这条通知，说明提醒功能正常！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(999999, notification)
    }
}

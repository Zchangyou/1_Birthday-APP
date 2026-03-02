package com.birthday.friends.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.birthday.friends.BirthdayApp
import com.birthday.friends.MainActivity
import com.birthday.friends.R
import com.birthday.friends.data.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val eventName = intent.getStringExtra(EXTRA_EVENT_NAME) ?: return
        val daysBefore = intent.getIntExtra(EXTRA_DAYS_BEFORE, 0)

        // 发出通知
        showNotification(context, eventId, eventName, daysBefore)

        // 注册下一年的 Alarm（循环）
        if (eventId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = EventRepository(context)
                    val event = repo.getById(eventId)
                    if (event != null) {
                        AlarmScheduler.schedule(context, event, daysBefore)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        eventId: Long,
        eventName: String,
        daysBefore: Int
    ) {
        val title: String
        val text: String
        when (daysBefore) {
            0 -> {
                title = "今天是 $eventName 的特别日子"
                text = "记得送上祝福！"
            }
            else -> {
                title = "$eventName 的生日/纪念日即将到来"
                text = "还有 $daysBefore 天，提前准备一下吧！"
            }
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, eventId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BirthdayApp.CHANNEL_ID_BIRTHDAY)
            .setSmallIcon(R.drawable.ic_cake)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(eventId.toInt(), notification)
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_NAME = "extra_event_name"
        const val EXTRA_DAYS_BEFORE = "extra_days_before"
    }
}

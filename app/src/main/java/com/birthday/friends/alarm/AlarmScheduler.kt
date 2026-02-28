package com.birthday.friends.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.birthday.friends.data.model.Event
import com.birthday.friends.util.EventUtils
import java.util.Calendar

/**
 * 为事件注册 / 取消 AlarmManager 提醒。
 * 每次触发后，AlarmReceiver 会自动注册下一年的 Alarm（循环）。
 */
object AlarmScheduler {

    // PendingIntent request code 编码规则：eventId * 10 + offset
    // offset: 0 = 当天, 1 = 提前3天, 2 = 提前7天
    private const val OFFSET_TODAY = 0
    private const val OFFSET_3DAY = 1
    private const val OFFSET_7DAY = 2

    fun scheduleAll(context: Context, event: Event) {
        if (event.month == null || event.day == null) return

        if (event.remindOnDay) schedule(context, event, 0)
        if (event.remind3Days) schedule(context, event, 3)
        if (event.remind7Days) schedule(context, event, 7)
    }

    fun cancelAll(context: Context, event: Event) {
        cancelForOffset(context, event.id, 0)
        cancelForOffset(context, event.id, 3)
        cancelForOffset(context, event.id, 7)
    }

    /**
     * @param daysBeforeEvent 0=当天, 3=提前3天, 7=提前7天
     */
    fun schedule(context: Context, event: Event, daysBeforeEvent: Int) {
        val reminderTime = getReminderTime(context)
        val nextOccurrence = EventUtils.getNextOccurrenceCalendar(event)

        val triggerCal = (nextOccurrence.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -daysBeforeEvent)
            set(Calendar.HOUR_OF_DAY, reminderTime.first)
            set(Calendar.MINUTE, reminderTime.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 如果触发时间已过，推到下一年
        val now = Calendar.getInstance()
        if (triggerCal <= now) {
            val nextYear = EventUtils.getNextOccurrenceCalendar(
                event,
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            )
            triggerCal.timeInMillis = nextYear.timeInMillis
            triggerCal.add(Calendar.DAY_OF_YEAR, -daysBeforeEvent)
            triggerCal.set(Calendar.HOUR_OF_DAY, reminderTime.first)
            triggerCal.set(Calendar.MINUTE, reminderTime.second)
            triggerCal.set(Calendar.SECOND, 0)
            triggerCal.set(Calendar.MILLISECOND, 0)
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pi = buildPendingIntent(context, event, daysBeforeEvent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerCal.timeInMillis,
                        pi
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerCal.timeInMillis,
                        pi
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerCal.timeInMillis,
                    pi
                )
            }
        } catch (e: SecurityException) {
            // Permission denied — no alarm set
        }
    }

    fun cancelForOffset(context: Context, eventId: Long, daysBeforeEvent: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val requestCode = requestCode(eventId, daysBeforeEvent)
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(
        context: Context,
        event: Event,
        daysBeforeEvent: Int
    ): PendingIntent {
        val requestCode = requestCode(event.id, daysBeforeEvent)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(AlarmReceiver.EXTRA_EVENT_NAME, event.name)
            putExtra(AlarmReceiver.EXTRA_DAYS_BEFORE, daysBeforeEvent)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 获取全局提醒时间（小时，分钟） */
    private fun getReminderTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("reminder_hour", 9)
        val minute = prefs.getInt("reminder_minute", 0)
        return Pair(hour, minute)
    }

    private fun requestCode(eventId: Long, daysBeforeEvent: Int): Int {
        val offset = when (daysBeforeEvent) {
            0 -> OFFSET_TODAY
            3 -> OFFSET_3DAY
            else -> OFFSET_7DAY
        }
        return (eventId * 10 + offset).toInt()
    }
}

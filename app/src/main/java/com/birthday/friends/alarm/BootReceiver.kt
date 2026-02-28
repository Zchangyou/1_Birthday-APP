package com.birthday.friends.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.birthday.friends.data.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机完成后重新注册所有 Alarm。
 * Alarm 在手机重启后会被系统清除，需要在 BOOT_COMPLETED 时重新注册。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = EventRepository(context)
                val events = repo.getAllEventsWithDate()
                events.forEach { event ->
                    AlarmScheduler.scheduleAll(context, event)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

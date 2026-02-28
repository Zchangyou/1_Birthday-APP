package com.birthday.friends.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.birthday.friends.data.repository.EventRepository
import java.util.concurrent.TimeUnit

/**
 * 每日检查一次，补充注册可能漏掉的 Alarm（WorkManager fallback）。
 * 在 EMUI 省电策略下，AlarmManager 可能被杀，WorkManager 作为兜底。
 */
class DailyCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = EventRepository(applicationContext)
        val events = repo.getAllEventsWithDate()
        events.forEach { event ->
            AlarmScheduler.scheduleAll(applicationContext, event)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_alarm_check"

        /**
         * 在 Application 启动时调用，确保每日检查任务已注册。
         */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

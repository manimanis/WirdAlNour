package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.WirdDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = DailyReminderScheduler(appContext)
                val config = WirdDatabase.getInstance(appContext).planConfigDao().getConfigOnce()

                if (config == null || config.reminderEnabled) {
                    val payload = scheduler.buildDynamicDailyPayload()
                    scheduler.showReminderNotification(
                        title = payload.title,
                        body = payload.body
                    )
                }

                // Chain next day's reminder to maintain reliable daily delivery across Doze mode
                if (config != null && config.reminderEnabled) {
                    scheduler.scheduleDailyReminder(
                        hour = config.reminderHour,
                        minute = config.reminderMinute,
                        forceTomorrow = true
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

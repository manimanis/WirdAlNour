package com.example.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.WirdDatabase
import com.example.data.local.WirdSeedData
import com.example.data.repository.WirdRepository
import com.example.util.formatAyahCountArabic
import com.example.util.toArabicDigits
import java.util.Calendar

data class DailyNotificationPayload(
    val title: String,
    val body: String
)

class DailyReminderScheduler(private val context: Context) {

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableLights(true)
                lightColor = Color.parseColor("#1B4D3E")
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun calculateNextTriggerMillis(hour: Int, minute: Int, forceTomorrow: Boolean = false): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (forceTomorrow || timeInMillis <= now + 5_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    fun isNextReminderToday(hour: Int, minute: Int): Boolean {
        val triggerCal = Calendar.getInstance().apply {
            timeInMillis = calculateNextTriggerMillis(hour, minute)
        }
        val todayCal = Calendar.getInstance()
        return triggerCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            triggerCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Schedules a Doze-compatible local daily reminder without requiring restricted exact-alarm
     * permissions. DailyReminderReceiver automatically schedules the next day's trigger upon firing.
     */
    fun scheduleDailyReminder(hour: Int, minute: Int, forceTomorrow: Boolean = false) {
        ensureNotificationChannel()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildReminderPendingIntent()
        val triggerAtMillis = calculateNextTriggerMillis(hour, minute, forceTomorrow)

        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildReminderPendingIntent())
    }

    suspend fun buildDynamicDailyPayload(): DailyNotificationPayload {
        val db = WirdDatabase.getInstance(context.applicationContext)
        val surahs = db.surahPlanDao().getAllSurahsOnce()
        if (surahs.isEmpty()) {
            return DailyNotificationPayload(
                title = context.getString(R.string.notif_title),
                body = context.getString(R.string.notif_body)
            )
        }

        val config = db.planConfigDao().getConfigOnce()
        val todayEpoch = WirdRepository.currentEpochDay()
        val calendarDay = if (config != null) {
            ((todayEpoch - config.startDateEpochDay).toInt() + 1).coerceIn(1, WirdSeedData.TOTAL_DAYS)
        } else {
            1
        }

        val grouped = surahs.groupBy { it.dayNumber }
        val activeDayNumber = (1..WirdSeedData.TOTAL_DAYS).firstOrNull { day ->
            grouped[day].orEmpty().any { !it.isCompleted }
        } ?: calendarDay

        val activeDaySurahs = grouped[activeDayNumber].orEmpty().sortedBy { it.surahNumber }
        val remainingSurahs = activeDaySurahs.filter { !it.isCompleted }

        if (remainingSurahs.isEmpty()) {
            return DailyNotificationPayload(
                title = context.getString(R.string.notif_khatmah_done_title),
                body = context.getString(R.string.notif_khatmah_done_body)
            )
        }

        val surahNamesPreview = remainingSurahs.take(3).joinToString(" • ") { "سورة ${it.arabicName}" }
        val extraCount = remainingSurahs.size - 3
        val surahSummary = if (extraCount > 0) {
            "$surahNamesPreview وغيرها (${remainingSurahs.size.toArabicDigits()} سور)"
        } else {
            surahNamesPreview
        }
        val totalRemainingAyahs = remainingSurahs.sumOf { it.ayahCount }

        val title = context.getString(R.string.notif_dynamic_title_format, activeDayNumber.toArabicDigits())
        val body = "$surahSummary — ${formatAyahCountArabic(totalRemainingAyahs)}. ${context.getString(R.string.notif_encouragement_suffix)}"
        return DailyNotificationPayload(title = title, body = body)
    }

    fun showReminderNotification(
        title: String = context.getString(R.string.notif_title),
        body: String = context.getString(R.string.notif_body)
    ): Boolean {
        ensureNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wird)
            .setColor(Color.parseColor("#1B4D3E"))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_stat_wird,
                context.getString(R.string.notif_open_wird_action),
                contentPendingIntent
            )
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun buildReminderPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "wird_daily_reminder_channel_v2"
        private const val NOTIFICATION_ID = 1401
        private const val REMINDER_REQUEST_CODE = 1402
        private const val OPEN_APP_REQUEST_CODE = 1403
    }
}

package com.aistudio.tdbassistant.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aistudio.tdbassistant.MainActivity
import com.aistudio.tdbassistant.R
import java.util.Calendar

class TdbReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "TDB Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "আপনার নির্ধারিত কাজের সময় হয়েছে।"
        val id = intent.getIntExtra(EXTRA_ID, 4001)
        showNotification(context, id, title, body)
    }

    companion object {
        private const val CHANNEL_ID = "tdb_reminders_channel"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_BODY = "reminder_body"
        const val EXTRA_ID = "reminder_id"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "TDB Study & Work Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Scheduled study, work, break, and daily goal reminders from TDB."
                }
                context.getSystemService(NotificationManager::class.java)
                    ?.createNotificationChannel(channel)
            }
        }

        fun showNotification(context: Context, id: Int, title: String, body: String) {
            ensureChannel(context)
            val openIntent = PendingIntent.getActivity(
                context,
                id,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
            context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
        }

        fun scheduleDailyOrNext(
            context: Context,
            requestCode: Int,
            hour24: Int,
            minute: Int,
            title: String,
            body: String
        ) {
            ensureChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val intent = Intent(context, TdbReminderReceiver::class.java).apply {
                putExtra(EXTRA_ID, requestCode)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}

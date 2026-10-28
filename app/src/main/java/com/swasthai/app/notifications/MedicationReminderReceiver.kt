package com.swasthai.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.swasthai.app.MainActivity
import com.swasthai.app.data.local.SwasthDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = SwasthDatabase.getInstance(context)
                val dao = database.medicationReminderDao()
                if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == Intent.ACTION_TIME_CHANGED ||
                    intent.action == Intent.ACTION_TIMEZONE_CHANGED
                ) {
                    dao.getEnabledReminders().forEach { MedicationReminderScheduler.scheduleNext(context, it) }
                    return@launch
                }

                val reminderId = intent.getLongExtra(MedicationReminderScheduler.EXTRA_REMINDER_ID, -1L)
                if (reminderId <= 0L) return@launch
                val reminder = dao.getEnabledReminder(reminderId) ?: return@launch
                val personName = database.personProfileDao().getProfile(reminder.profileId)?.displayName
                showNotification(context, reminderId, reminder.medicationName, reminder.instructions, personName)
                MedicationReminderScheduler.scheduleNext(context, reminder)
            } catch (error: Exception) {
                android.util.Log.e("MedicationReminderReceiver", "Could not deliver or reschedule reminder", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, reminderId: Long, name: String, instructions: String, personName: String?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Medicine reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val details = instructions.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Medicine reminder${personName?.let { " · $it" }.orEmpty()}")
            .setContentText("$name$details")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(
                Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Medicine reminder")
                    .setContentText("Open SwasthAI to view")
                    .build()
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        runCatching { manager.notify(reminderId.toInt(), notification) }
    }

    companion object {
        private const val CHANNEL_ID = "medication_reminders"
    }
}

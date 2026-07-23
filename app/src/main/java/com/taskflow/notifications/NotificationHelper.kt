package com.taskflow.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    const val CHANNEL_ID = "task_reminders"

    /** Call once, from Application.onCreate — channel creation is a no-op if it already exists. */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for tasks with a due date"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(context: Context, taskId: Long, title: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // Swap for a real app icon drawable/mipmap once you have one you like —
            // this system icon just guarantees the notification compiles and shows for now.
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task due")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        }
        // If permission isn't granted, we silently skip rather than crash — the app should
        // have already asked for POST_NOTIFICATIONS on first launch (see MainActivity).
    }
}
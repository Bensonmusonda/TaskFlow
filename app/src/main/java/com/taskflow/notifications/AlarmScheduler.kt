package com.taskflow.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.taskflow.data.entity.Task

/**
 * Wraps AlarmManager for task-due reminders. Prefers an exact alarm when the OS allows it
 * (Android 12+ requires the user to have granted "Alarms & reminders" for exact delivery);
 * otherwise falls back to an inexact alarm rather than interrupting the user with a
 * permission-settings detour just to set a task reminder.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(task: Task) {
        val dueDate = task.dueDate ?: return
        if (task.isCompleted) return
        if (dueDate <= System.currentTimeMillis()) return // don't schedule a reminder in the past

        val pendingIntent = buildPendingIntent(task.id, task.title)
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueDate, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueDate, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call — fall back rather than crash.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueDate, pendingIntent)
        }
    }

    fun cancel(taskId: Long) {
        val pendingIntent = buildPendingIntent(taskId, title = null)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * PendingIntent matching (for both scheduling and cancelling the same alarm) is based on
     * the Intent's component + requestCode — extras like [title] don't affect matching, so
     * cancel() can pass null for title and still hit the right pending alarm.
     */
    private fun buildPendingIntent(taskId: Long, title: String?): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
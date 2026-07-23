package com.taskflow.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskflow.TaskFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // onReceive must return quickly; goAsync() + a background coroutine lets us
        // finish the DB query and re-schedule alarms without being killed mid-work.
        val pendingResult = goAsync()
        val app = context.applicationContext as TaskFlowApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.rescheduleAllPendingReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
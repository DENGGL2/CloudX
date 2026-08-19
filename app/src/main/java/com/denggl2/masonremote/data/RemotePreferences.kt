package com.denggl2.masonremote.data

import android.content.Context
import com.denggl2.masonremote.ui.settings.TaskNotificationMode

private const val PREFS_NAME = "mason_remote_preferences"
private const val KEY_NOTIFICATION_MODE = "task_notification_mode"

class RemotePreferences(private val context: Context) {
    var taskNotificationMode: TaskNotificationMode
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTIFICATION_MODE, TaskNotificationMode.REGULAR.name)
            ?.let { value -> TaskNotificationMode.entries.firstOrNull { it.name == value } }
            ?: TaskNotificationMode.REGULAR
        set(value) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NOTIFICATION_MODE, value.name)
                .apply()
        }
}

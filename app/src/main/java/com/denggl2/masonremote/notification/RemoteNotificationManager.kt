package com.denggl2.masonremote.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.denggl2.masonremote.MainActivity
import com.denggl2.masonremote.R
import com.denggl2.masonremote.ui.remote.RemoteTaskNotificationEvent
import com.denggl2.masonremote.ui.settings.TaskNotificationMode

data class RemoteNotificationResult(
    val sent: Boolean,
    val needsPermission: Boolean = false,
    val message: String? = null,
)

/** Notification behavior kept aligned with MASON's computer-side task notifications. */
class RemoteNotificationManager(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeNotificationIds = linkedSetOf<Int>()
    private var appInForeground = true

    init {
        createChannels()
    }

    fun setAppInForeground(value: Boolean) {
        appInForeground = value
    }

    fun shouldRequestPostNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED

    fun preview(mode: TaskNotificationMode): RemoteNotificationResult {
        if (mode == TaskNotificationMode.DISABLED) {
            cancelAll()
            return RemoteNotificationResult(sent = true)
        }
        if (shouldRequestPostNotificationPermission()) {
            return RemoteNotificationResult(
                sent = false,
                needsPermission = true,
                message = "缺少通知权限，请在系统设置中允许 CloudX 发送通知",
            )
        }
        if (!notificationManager.areNotificationsEnabled()) {
            return RemoteNotificationResult(
                sent = false,
                message = "系统通知已关闭，请在系统设置中允许 CloudX 发送通知",
            )
        }

        val useIsland = canUseNotificationIsland(mode)
        val notificationId = PREVIEW_NOTIFICATION_ID
        notificationManager.cancel(LIVE_UPDATE_NOTIFICATION_ID)
        val previewText = "CloudX 会在电脑端任务状态变化时通知你"
        val text = if (mode == TaskNotificationMode.ISLAND && !useIsland) {
            "$previewText，当前设备暂不支持岛通知，已使用任务实时状态通知"
        } else {
            previewText
        }
        post(
            notificationId = notificationId,
            title = if (mode == TaskNotificationMode.ISLAND) "岛通知已选择" else "常规通知已启用",
            text = text,
            liveUpdate = useIsland,
            progress = 35,
            shortText = if (useIsland) "岛通知" else null,
            final = false,
            threadId = null,
            allowForeground = true,
        )
        if (useIsland) {
            mainHandler.removeCallbacksAndMessages(PREVIEW_TOKEN)
            mainHandler.postAtTime(
                {
                    post(
                        notificationId = PREVIEW_NOTIFICATION_ID,
                        title = "岛通知已选择",
                        text = "CloudX 会在电脑端任务状态变化时通知你",
                        liveUpdate = true,
                        progress = 100,
                        shortText = "完成",
                        final = true,
                        threadId = null,
                        allowForeground = true,
                    )
                },
                PREVIEW_TOKEN,
                System.currentTimeMillis() + 3_000L,
            )
        }
        return RemoteNotificationResult(sent = true)
    }

    fun notifyTaskEvent(
        event: RemoteTaskNotificationEvent,
        mode: TaskNotificationMode,
    ): RemoteNotificationResult {
        if (mode == TaskNotificationMode.DISABLED) return RemoteNotificationResult(sent = true)
        if (shouldRequestPostNotificationPermission()) {
            return RemoteNotificationResult(sent = false, needsPermission = true)
        }
        if (!notificationManager.areNotificationsEnabled()) {
            return RemoteNotificationResult(
                sent = false,
                message = "系统通知已关闭，请在系统设置中允许 CloudX 发送通知",
            )
        }

        val useIsland = canUseNotificationIsland(mode)
        val notificationId = stableNotificationId(event.threadId)
        val (title, text, progress, shortText, final) = when (event.kind) {
            RemoteTaskNotificationEvent.Kind.RUNNING -> NotificationContent(
                title = "CloudX 正在处理任务",
                text = "${event.conversationTitle} 正在电脑端执行，可点按返回 CloudX 查看进度",
                progress = 5,
                shortText = "执行中",
                final = false,
            )
            RemoteTaskNotificationEvent.Kind.WAITING_FOR_PERMISSION -> NotificationContent(
                title = "CloudX 需要你的确认",
                text = "${event.conversationTitle} 等待你批准电脑端权限请求",
                progress = 50,
                shortText = "需确认",
                final = false,
            )
            RemoteTaskNotificationEvent.Kind.COMPLETED -> NotificationContent(
                title = "CloudX 已完成",
                text = event.detail ?: "${event.conversationTitle} 已处理完成，可点按返回查看结果",
                progress = 100,
                shortText = "已完成",
                final = true,
            )
            RemoteTaskNotificationEvent.Kind.FAILED -> NotificationContent(
                title = "CloudX 任务失败",
                text = event.detail ?: "${event.conversationTitle} 未能完成，可返回 CloudX 查看原因",
                progress = 100,
                shortText = "失败",
                final = true,
            )
        }
        val sent = post(
            notificationId = notificationId,
            title = title,
            text = text,
            liveUpdate = useIsland,
            progress = progress,
            shortText = shortText,
            final = final,
            threadId = event.threadId,
            allowForeground = false,
        )
        return RemoteNotificationResult(sent = sent)
    }

    fun cancelAll() {
        mainHandler.removeCallbacksAndMessages(PREVIEW_TOKEN)
        notificationManager.cancelAll()
        activeNotificationIds.forEach(notificationManager::cancel)
        activeNotificationIds.clear()
    }

    private fun canUseNotificationIsland(mode: TaskNotificationMode): Boolean =
        mode == TaskNotificationMode.ISLAND &&
            Build.VERSION.SDK_INT >= 36 &&
            notificationManager.canPostPromotedNotifications()

    private fun post(
        notificationId: Int,
        title: String,
        text: String,
        liveUpdate: Boolean,
        progress: Int,
        shortText: String?,
        final: Boolean,
        threadId: String?,
        allowForeground: Boolean,
    ): Boolean {
        if (!allowForeground && appInForeground) {
            return false
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_TASK
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                threadId?.let { putExtra(EXTRA_THREAD_ID, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(
            context,
            if (liveUpdate) LIVE_UPDATE_CHANNEL_ID else CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification_mason)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (liveUpdate) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(contentIntent)
            .setAutoCancel(!liveUpdate || final)

        if (liveUpdate) {
            builder
                .setOngoing(!final)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setShortCriticalText(shortText?.take(7).orEmpty().ifBlank { "$progress%" })
                .setStyle(
                    NotificationCompat.ProgressStyle()
                        .setStyledByProgress(true)
                        .setProgress(progress.coerceIn(0, 100)),
                )
            if (!final && Build.VERSION.SDK_INT >= 36) builder.setRequestPromotedOngoing(true)
        }
        notificationManager.notify(notificationId, builder.build())
        activeNotificationIds += notificationId
        return true
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Android keeps channel records across upgrades. Remove old branded
        // channels, including renamed variants.
        notificationManager.notificationChannels
            .filter { channel ->
                sequenceOf(channel.id, channel.name?.toString(), channel.description)
                    .filterNotNull()
                    .any { value -> value.contains("mason", ignoreCase = true) }
            }
            .forEach { channel -> notificationManager.deleteNotificationChannel(channel.id) }
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        notificationManager.deleteNotificationChannel(LEGACY_LIVE_UPDATE_CHANNEL_ID)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "CloudX 发送的任务状态通知" },
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                LIVE_UPDATE_CHANNEL_ID,
                "CloudX 任务实时状态",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "用于 Android 16 的任务实时通知" },
        )
    }

    private fun stableNotificationId(threadId: String): Int =
        (threadId.hashCode() and 0x7fffffff).coerceAtLeast(47010)

    private data class NotificationContent(
        val title: String,
        val text: String,
        val progress: Int,
        val shortText: String,
        val final: Boolean,
    )

    companion object {
        const val CHANNEL_ID = "cloudx_tool_notification"
        const val LIVE_UPDATE_CHANNEL_ID = "cloudx_task_live_update"
        const val CHANNEL_NAME = "CloudX 工具通知"
        const val ACTION_OPEN_TASK = "com.denggl2.masonremote.action.OPEN_TASK"
        const val EXTRA_THREAD_ID = "thread_id"
        private const val LIVE_UPDATE_NOTIFICATION_ID = 47001
        private const val PREVIEW_NOTIFICATION_ID = 47002
        private const val LEGACY_CHANNEL_ID = "mason_tool_notification"
        private const val LEGACY_LIVE_UPDATE_CHANNEL_ID = "mason_task_live_update"
        private val PREVIEW_TOKEN = Any()
    }
}

package com.denggl2.masonremote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale
import com.denggl2.masonremote.ui.settings.RemoteLanguagePreference
import com.denggl2.masonremote.ui.settings.RemoteFontSizePreference
import com.denggl2.masonremote.ui.settings.RemoteInterfaceStyle
import com.denggl2.masonremote.ui.settings.RemoteMessageSendMode
import com.denggl2.masonremote.ui.settings.RemoteThemeMode
import com.denggl2.masonremote.ui.settings.TaskNotificationMode

/** The two UI languages supported by the phone client. */
enum class RemoteResolvedLanguage {
    CHINESE,
    ENGLISH,
}

@Immutable
class RemoteStrings internal constructor(
    val language: RemoteResolvedLanguage,
) {
    val isEnglish: Boolean
        get() = language == RemoteResolvedLanguage.ENGLISH

    fun text(chinese: String, english: String): String =
        if (isEnglish) english else chinese

    /** Translate a known app-owned string. Unknown values are intentionally kept intact. */
    fun t(chinese: String): String = if (isEnglish) remoteEnglishText(chinese) else chinese

    /** Text values used by the Compose display layer, including common dynamic labels. */
    fun displayText(value: String): String {
        if (!isEnglish || value.isEmpty()) return value
        remoteEnglishText(value).takeIf { it != value }?.let { return it }
        translateWebRtcPairingFailure(value)?.let { return it }
        translateWebRtcSignalingFailure(value)?.let { return it }
        return when {
            value.startsWith("已配对（") && value.endsWith("）") ->
                "Paired (${value.removePrefix("已配对（").removeSuffix("）")})"
            value.startsWith("发送失败：") -> "Send failed: ${displayText(value.removePrefix("发送失败："))}"
            value.startsWith("图片读取失败：") -> "Image read failed: ${displayText(value.removePrefix("图片读取失败："))}"
            value.startsWith("已下载到 ") -> "Downloaded to ${value.removePrefix("已下载到 ")}"
            value.startsWith("已保存到 ") -> "Saved to ${value.removePrefix("已保存到 ")}"
            value.startsWith("保存失败：") -> "Save failed: ${displayText(value.removePrefix("保存失败："))}"
            value.startsWith("分享失败：") -> "Share failed: ${displayText(value.removePrefix("分享失败："))}"
            value.startsWith("错误码") -> "Error ${value.removePrefix("错误码")}"
            value.startsWith("已断开连接，请重试\n") ->
                "Connection lost. Please retry\n${value.substringAfter("\n")}"
            value.startsWith("电脑文件 · ") -> "Computer file · ${value.removePrefix("电脑文件 · ")}"
            value.startsWith("进行中 · ") -> "In progress · ${value.removePrefix("进行中 · ")}"
            value.startsWith("耗时") -> "Duration ${value.removePrefix("耗时")}"
            value.startsWith("已执行") && value.endsWith("条") -> {
                val count = value.removePrefix("已执行").removeSuffix("条").trim()
                "Executed $count ${if (count == "1") "item" else "items"}"
            }
            value.startsWith("当前连接方式为") && value.endsWith("，是否断开？") ->
                "Connection method: ${displayText(value.removePrefix("当前连接方式为").removeSuffix("，是否断开？"))}. Disconnect?"
            value.startsWith("请扫描 ") && value.endsWith("二维码") ->
                "Scan the ${displayText(value.removePrefix("请扫描 ").removeSuffix("二维码"))} QR code"
            value.contains("\n预计清理 ") ->
                "Only image preview cache will be cleared\nEstimated: ${value.substringAfter("\n预计清理 ")}"
            value.contains("服务器\n") || value.contains("设备指纹\n") || value.contains("二维码有效期\n") ->
                value.replace("服务器\n", "Server\n")
                    .replace("设备指纹\n", "Device fingerprint\n")
                    .replace("二维码有效期\n", "QR code validity\n")
                    .replace("\n剩余 ", "\n")
                    .replace("分", "m ")
                    .replace("秒", "s")
            value.startsWith("有效至 ") -> value
                .replace("（剩余 ", " ( ")
                .replace("分", "m ")
                .replace("秒）", "s remaining)")
            value.startsWith("二维码已过期") -> "The QR code has expired. Ask the computer app to generate a new one"
            value.startsWith("诊断日志导出失败：") ->
                "Diagnostic log export failed: ${displayText(value.removePrefix("诊断日志导出失败："))}"
            value.startsWith("图片预览缓存") -> value.replace("图片预览缓存已清理", "Image preview cache cleared")
            value.startsWith("向 ") && value.endsWith(" 发送消息") ->
                "Message ${value.removePrefix("向 ").removeSuffix(" 发送消息")}"
            value.startsWith("每次最多添加 ") && value.endsWith(" 个附件") ->
                "You can add up to ${value.removePrefix("每次最多添加 ").removeSuffix(" 个附件")} attachments at a time"
            value.startsWith("无法创建 ") -> "Unable to create ${value.removePrefix("无法创建 ")}"
            value == "关闭推理" -> "Off"
            value == "极简" -> "Minimal"
            value == "默认推理" -> "Default reasoning"
            value == "低" -> "Low"
            value == "中" -> "Medium"
            value == "高" -> "High"
            value == "极高" -> "Very high"
            value == "最高" -> "Max"
            value == "超高" -> "Ultra"
            value == "未读取" -> "Unavailable"
            value == "请求批准" -> "Ask for approval"
            value == "帮我批准" -> "Approve for me"
            value == "完全访问权限" -> "Full access"
            value == "进行中" -> "In progress"
            value == "已执行" -> "Executed"
            value == "条" -> " items"
            else -> remoteEnglishText(value)
        }
    }

    private fun translateWebRtcPairingFailure(value: String): String? {
        val (sourcePrefix, targetPrefix) = when {
            value.startsWith("RTC 配对超时（阶段：") -> "RTC 配对超时（阶段：" to "RTC pairing timed out (stage: "
            value.startsWith("RTC 配对失败（阶段：") -> "RTC 配对失败（阶段：" to "RTC pairing failed (stage: "
            else -> return null
        }
        val remainder = value.removePrefix(sourcePrefix)
        val metadataEnd = remainder.indexOf('）')
        if (metadataEnd < 0) return null
        val metadata = remainder.substring(0, metadataEnd)
        val iceMarker = "，ICE 状态："
        val turnMarker = "，TURN："
        val iceIndex = metadata.indexOf(iceMarker)
        val turnIndex = metadata.indexOf(turnMarker)
        if (iceIndex < 0 || turnIndex <= iceIndex) return null
        val stage = metadata.substring(0, iceIndex)
        val iceState = metadata.substring(iceIndex + iceMarker.length, turnIndex)
        val turnState = metadata.substring(turnIndex + turnMarker.length)
        val detail = remainder.substring(metadataEnd + 1).removePrefix("：")
        return buildString {
            append(targetPrefix)
            append(remoteEnglishText(stage))
            append(", ICE state: ")
            append(iceState)
            append(", TURN: ")
            append(
                when (turnState) {
                    "未配置" -> "not configured"
                    "已配置" -> "configured"
                    else -> turnState
                },
            )
            append(')')
            if (detail.isNotBlank()) {
                append(": ")
                append(displayText(detail))
            }
        }
    }

    private fun translateWebRtcSignalingFailure(value: String): String? {
        val (sourcePrefix, targetPrefix) = WEBRTC_SIGNALING_FAILURE_PREFIXES
            .firstOrNull { (source, _) -> value.startsWith(source) }
            ?: return null
        val suffix = value.removePrefix(sourcePrefix)
        if (!suffix.startsWith('（')) return targetPrefix + suffix
        val statusEnd = suffix.indexOf('）')
        if (statusEnd < 0) return targetPrefix + suffix
        return buildString {
            append(targetPrefix)
            append(" (")
            append(suffix.substring(1, statusEnd))
            append(')')
            append(suffix.substring(statusEnd + 1))
        }
    }

    /** Translate app-authored markdown snippets while preserving user content. */
    fun content(value: String): String {
        if (!isEnglish || value.isEmpty()) return value
        val translated = message(value)
        if (translated != value) return translated.orEmpty()
        val replacements = listOf(
            "任务结果" to "Task result",
            "这是普通文本，支持" to "This is regular text with support for",
            "这是普通文本，支持 **加粗**、`灰底代码` 和 [可点击链接](https://github.com/openai/codex)。" to
                "This is regular text with support for **bold text**, `inline code`, and [a clickable link](https://github.com/openai/codex).",
            "加粗" to "bold text",
            "灰底代码" to "inline code",
            "这一行与上一行之间保留换行。" to "The line break between this line and the previous one is preserved.",
            "这是引用内容。" to "This is quoted content.",
            "无序列表项目" to "Unordered list item",
            "第二个项目" to "Second item",
            "有序列表项目" to "Ordered list item",
            "已完成任务" to "Completed task",
            "待处理任务" to "Pending task",
            "状态" to "Status",
            "数量" to "Count",
            "完成" to "Completed",
            "进行中" to "In progress",
            "新增行" to "Added line",
            "删除行" to "Removed line",
            "保留行" to "Unchanged line",
            "展示手机端富文本效果" to "Show rich text on the phone",
            "把 Codex 的执行状态整理给我看" to "Show me the Codex execution status",
            "过程会按事件顺序显示，进行中的任务直接跟随对应行更新。" to "Events appear in order, and running tasks update beside their row.",
            "第一次思考更新：分析任务目标。" to "First thinking update: analyze the task goal.",
            "第二次思考更新：确认执行步骤。" to "Second thinking update: confirm the execution steps.",
            "开始检查电脑端工作区。" to "Start checking the computer workspace.",
            "第一条命令输出。" to "Output from the first command.",
            "第二条命令输出：同类命令更新覆盖上一条。" to "Output from the second command: the latest update replaces the previous one.",
            "搜索结果已返回。" to "Search results returned.",
            "调用远程工具并等待结果。" to "Call a remote tool and wait for the result.",
            "已写入目标文件。" to "The target file was written.",
            "计划已更新。" to "The plan was updated.",
            "已生成图片预览。" to "Image preview generated.",
            "其他操作已完成。" to "The other operation is complete.",
            "整理 CloudX 远程控制项目" to "Organize the CloudX remote-control project",
            "我正在检查工作区结构，并准备整理远程控制相关模块。" to "I am checking the workspace structure and preparing the remote-control modules.",
            "正在扫描工作区，等待电脑端返回任务进度。" to "Scanning the workspace and waiting for task progress from the computer.",
            "检查远程电脑端工作区" to "Check the remote computer workspace",
            "任务已完成。执行记录默认收起，点击执行行可以展开查看具体命令。" to "Task complete. Execution details are collapsed by default; tap a run row to view the command.",
            "命令已完成。" to "The command is complete.",
            "文件修改已完成。" to "The file change is complete.",
            "修复扫码连接超时" to "Fix QR connection timeout",
            "任务失败：未收到电脑端响应。请确认电脑端项目已经启动。" to "Task failed: no response from the computer. Make sure the computer project is running.",
            "未在等待时间内收到电脑端响应。" to "No response from the computer arrived in time.",
            "测试远程任务恢复" to "Test remote task recovery",
            "任务已中断，可以从最近消息继续。" to "Task interrupted. You can continue from the latest message.",
            "任务在电脑端停止，当前页面保留最近消息。" to "The task stopped on the computer; this page keeps the latest messages.",
            "已完成：这是电脑端 Codex 返回的对话内容。你可以展开下面的过程行查看任务详情。" to "Complete: this is the conversation returned by Codex on the computer. Expand the process row below for details.",
            "已完成本次任务。这里会显示电脑端返回的过程和任务结果。" to "This task is complete. The process and result returned by the computer appear here.",
            "电脑端正在等待你的允许" to "The computer is waiting for your approval",
            "等待电脑端确认请求。" to "Waiting for an approval request from the computer.",
            "运行需要确认的远程命令" to "Run a remote command that needs approval",
            "电脑端正在等待你的允许，确认后会继续执行。" to "The computer is waiting for your approval and will continue after confirmation.",
        )
        return replacements
            .sortedByDescending { (source, _) -> source.length }
            .fold(value) { result, (source, target) -> result.replace(source, target) }
    }

    fun multiline(chinese: String, english: String): String = text(chinese, english)

    /** Translate app-generated messages while leaving remote-authored content untouched. */
    fun message(message: String?): String? {
        if (!isEnglish || message.isNullOrBlank()) return message
        return remoteEnglishText(message)
    }
}

val LocalRemoteStrings = staticCompositionLocalOf {
    RemoteStrings(RemoteResolvedLanguage.CHINESE)
}

@Composable
fun resolveRemoteStrings(preference: RemoteLanguagePreference): RemoteStrings {
    val systemLanguage = LocalConfiguration.current.locales[0]?.language.orEmpty()
    val resolved = when (preference) {
        RemoteLanguagePreference.CHINESE -> RemoteResolvedLanguage.CHINESE
        RemoteLanguagePreference.ENGLISH -> RemoteResolvedLanguage.ENGLISH
        RemoteLanguagePreference.SYSTEM -> if (systemLanguage.startsWith("zh", ignoreCase = true)) {
            RemoteResolvedLanguage.CHINESE
        } else {
            RemoteResolvedLanguage.ENGLISH
        }
    }
    return RemoteStrings(resolved)
}

internal fun RemoteLanguagePreference.localizedLabel(strings: RemoteStrings): String = when (this) {
    RemoteLanguagePreference.SYSTEM -> strings.text("跟随系统", "Follow system")
    RemoteLanguagePreference.CHINESE -> strings.text("中文", "Chinese")
    RemoteLanguagePreference.ENGLISH -> "English"
}

internal fun RemoteThemeMode.localizedLabel(strings: RemoteStrings): String = when (this) {
    RemoteThemeMode.SYSTEM -> strings.text("跟随系统", "Follow system")
    RemoteThemeMode.LIGHT -> strings.text("浅色", "Light")
    RemoteThemeMode.DARK -> strings.text("深色", "Dark")
}

internal fun RemoteInterfaceStyle.localizedLabel(strings: RemoteStrings): String = when (this) {
    RemoteInterfaceStyle.NATIVE -> strings.text("原生", "Native")
    RemoteInterfaceStyle.GLASS -> strings.text("玻璃", "Glass")
}

internal fun RemoteFontSizePreference.localizedLabel(strings: RemoteStrings): String = when (this) {
    RemoteFontSizePreference.SMALL -> strings.text("小", "Small")
    RemoteFontSizePreference.MEDIUM -> strings.text("中", "Medium")
    RemoteFontSizePreference.LARGE -> strings.text("大", "Large")
    RemoteFontSizePreference.EXTRA_LARGE -> strings.text("超大", "Extra large")
}

internal fun TaskNotificationMode.localizedLabel(strings: RemoteStrings): String = when (this) {
    TaskNotificationMode.REGULAR -> strings.text("启用常规通知", "Enable regular notifications")
    TaskNotificationMode.ISLAND -> strings.text("启用岛通知", "Enable island notifications")
    TaskNotificationMode.DISABLED -> strings.text("不启用", "Disabled")
}

internal fun TaskNotificationMode.localizedSummary(strings: RemoteStrings): String = when (this) {
    TaskNotificationMode.REGULAR -> strings.text("常规通知", "Regular notifications")
    TaskNotificationMode.ISLAND -> strings.text("岛通知", "Island notifications")
    TaskNotificationMode.DISABLED -> strings.text("不启用", "Disabled")
}

internal fun RemoteMessageSendMode.localizedLabel(strings: RemoteStrings): String = when (this) {
    RemoteMessageSendMode.STEER -> strings.text("插队", "Send now")
    RemoteMessageSendMode.QUEUE -> strings.text("排队", "Queue")
}

internal fun remoteEnglishText(value: String): String = when (value) {
    "外观" -> "Appearance"
    "通知" -> "Notifications"
    "对话" -> "Conversations"
    "远程配置" -> "Remote connection"
    "系统" -> "System"
    "设置" -> "Settings"
    "关于" -> "About"
    "折射效果" -> "Refraction"
    "透明度" -> "Transparency"
    "输入透明度" -> "Enter transparency"
    "霜冻" -> "Frost"
    "输入霜冻强度" -> "Enter frost strength"
    "任务通知" -> "Task notifications"
    "进行中发送消息" -> "Messages during a task"
    "深色模式" -> "Dark mode"
    "字体大小" -> "Font size"
    "语言" -> "Language"
    "风格" -> "Style"
    "启用常规通知" -> "Enable regular notifications"
    "启用岛通知" -> "Enable island notifications"
    "常规通知" -> "Regular notifications"
    "岛通知" -> "Island notifications"
    "不启用" -> "Disabled"
    "插队" -> "Send now"
    "排队" -> "Queue"
    "跟随系统" -> "Follow system"
    "浅色" -> "Light"
    "深色" -> "Dark"
    "原生" -> "Native"
    "玻璃" -> "Glass"
    "小" -> "Small"
    "中" -> "Medium"
    "大" -> "Large"
    "超大" -> "Extra large"
    "当前选项" -> "Current option"
    "当前模式" -> "Current mode"
    "当前字体大小" -> "Current font size"
    "当前语言" -> "Current language"
    "当前风格" -> "Current style"
    "已配对（$value）" -> value
    "远端电脑配置" -> "Remote computer"
    "管理" -> "Manage"
    "版本号" -> "Version"
    "项目地址" -> "Project"
    "开源协议" -> "License"
    "检查更新" -> "Check for updates"
    "导出日志" -> "Export logs"
    "清缓存" -> "Clear cache"
    "信息确认" -> "Confirm"
    "只清理图片预览缓存" -> "Only image preview cache will be cleared"
    "取消" -> "Cancel"
    "清理" -> "Clear"
    "诊断日志已导出" -> "Diagnostic log exported"
    "未知错误" -> "Unknown error"
    "图片预览缓存已清理" -> "Image preview cache cleared"
    "缓存清理失败" -> "Unable to clear cache"
    "仅安卓12+生效" -> "Requires Android 12 or newer"
    "仅安卓13+生效" -> "Requires Android 13 or newer"
    "收起选项" -> "Collapse options"
    "展开选项" -> "Expand options"
    "打开设置项" -> "Open setting"
    "未配对远端电脑" -> "No remote computer paired"
    "电脑上暂无会话" -> "No conversations on the computer"
    "置顶" -> "Pinned"
    "最近" -> "Recent"
    "点击重试" -> ", tap to retry"
    "断开中" -> "Disconnecting"
    "连接中" -> "Connecting"
    "已连接" -> "Connected"
    "连接状态" -> "Connection status"
    "新建远端对话" -> "New remote conversation"
    "新对话" -> "New conversation"
    "正在读取电脑选项" -> "Reading computer options"
    "项目" -> "Project"
    "选择项目" -> "Select project"
    "模型" -> "Model"
    "选择模型" -> "Select model"
    "推理层级" -> "Reasoning"
    "访问权限" -> "Permissions"
    "选择权限" -> "Select permissions"
    "向电脑端发起对话" -> "Start a conversation with the computer"
    "发起对话" -> "Start conversation"
    "当前任务进行中，可插入或排队" -> "A task is running; send now or queue"
    "添加" -> "Add"
    "添加图片" -> "Add image"
    "添加文件" -> "Add file"
    "使用 Skill" -> "Use skill"
    "正在读取远程 Skill" -> "Reading remote skills"
    "远程端暂无可用 Skill" -> "No remote skills available"
    "停止" -> "Stop"
    "发送" -> "Send"
    "移除" -> "Remove"
    "拒绝" -> "Decline"
    "允许" -> "Allow"
    "回到最新消息" -> "Jump to latest message"
    "加载中" -> "Loading"
    "还没有消息" -> "No messages yet"
    "等待确认" -> "Awaiting confirmation"
    "确定" -> "Confirm"
    "取消置顶" -> "Unpin"
    "归档" -> "Archive"
    "重试" -> "Retry"
    "返回" -> "Back"
    "确认" -> "Confirm"
    "电脑端启动后扫码配对" -> "Scan the QR code after starting the computer app"
    "请选择连接方式" -> "Choose a connection method"
    "Cloudflare 隧道" -> "Cloudflare tunnel"
    "本地网络" -> "Local network"
    "中转，电脑端重启需要重新配对" -> "Relay; pairing is required after restarting the computer app"
    "WebRTC 直连" -> "WebRTC direct"
    "手机直连，信令服务器配对" -> "Direct connection; pair through the signaling server"
    "开始" -> "Start"
    "扫码配对" -> "Scan to pair"
    "确认配对" -> "Confirm pairing"
    "正在配对" -> "Pairing"
    "已完成配对" -> "Pairing complete"
    "配对失败" -> "Pairing failed"
    "扫描电脑端项目生成的二维码" -> "Scan the QR code generated by the computer project"
    "允许相机权限" -> "Allow camera access"
    "电脑" -> "Computer"
    "服务器" -> "Server"
    "设备指纹" -> "Device fingerprint"
    "二维码有效期" -> "QR code validity"
    "确认并配对" -> "Confirm and pair"
    "正在建立安全连接…" -> "Establishing a secure connection…"
    "已连接到电脑端" -> "Connected to the computer"
    "进入对话" -> "Open conversations"
    "重新扫码" -> "Scan again"
    "未知" -> "Unknown"
    "已过期，请重新生成" -> "Expired; generate a new code"
    "已断开连接，请重试" -> "Connection lost. Please retry"
    "重新配对" -> "Pair again"
    "请确认断开连接" -> "Confirm disconnection"
    "当前连接" -> "Current connection"
    "是否断开？" -> "Disconnect?"
    "电脑文件" -> "Computer file"
    "进行中" -> "In progress"
    "耗时" -> "Duration "
    "已执行" -> "Executed "
    "条" -> " items"
    "命令输出" -> "Command output"
    "命令详情" -> "Command details"
    "命令" -> "Command"
    "输出" -> "Output"
    "复制" -> "Copy"
    "已复制" -> "Copied "
    "图片预览失败" -> "Image preview failed"
    "图片预览" -> "Image preview"
    "文件预览" -> "File preview"
    "关闭文件预览" -> "Close file preview"
    "正在读取文件" -> "Reading file"
    "无法预览" -> "Preview unavailable"
    "（空文件）" -> "(Empty file)"
    "下载中" -> "Downloading"
    "下载" -> "Download"
    "权限确认" -> "Permission required"
    "空闲" -> "Idle"
    "已完成" -> "Completed"
    "已停止" -> "Stopped"
    "失败" -> "Failed"
    "思考" -> "Thinking"
    "执行" -> "Run"
    "搜索" -> "Search"
    "工具" -> "Tool"
    "修改" -> "Edit"
    "说明" -> "Note"
    "计划" -> "Plan"
    "更新计划" -> "Update plan"
    "图片" -> "Image"
    "图像" -> "Image"
    "其他" -> "Other"
    "运行命令" -> "Run command"
    "搜索网页" -> "Search the web"
    "调用工具" -> "Call tool"
    "修改文件" -> "Edit file"
    "执行说明" -> "Execution details"
    "生成图像" -> "Generate image"
    "查看图像" -> "View image"
    "组织回复" -> "Compose response"
    "上下文压缩" -> "Compress context"
    "连接" -> "Connect"
    "恢复" -> "Resume"
    "任务进展" -> "Task progress"
    "等待电脑反馈" -> "Waiting for computer"
    "发送消息" -> "Sending message"
    "正在等待电脑端响应" -> "Waiting for the computer to respond"
    "正在等待电脑端活动更新" -> "Waiting for activity from the computer"
    "电脑端记录暂时无法显示。" -> "The computer record is temporarily unavailable."
    "电脑端暂时没有可显示的消息。" -> "No messages are currently available from the computer."
    "消息已发送，但无法刷新对话" -> "Message sent, but the conversation could not be refreshed"
    "无法发送到电脑" -> "Unable to send to the computer"
    "文件下载失败" -> "File download failed"
    "无法读取附件" -> "Unable to read the attachment"
    "无法创建下载文件" -> "Unable to create the download file"
    "无法写入下载文件" -> "Unable to write the download file"
    "电脑端附件读取失败" -> "Unable to read the attachment from the computer"
    "电脑端请求失败" -> "The computer request failed"
    "电脑端连接失败" -> "Unable to connect to the computer"
    "无法建立电脑端连接" -> "Unable to establish a connection to the computer"
    "RTC 配对失败" -> "RTC pairing failed"
    "二维码信息无效" -> "The QR code is invalid"
    "测试二维码内容无效" -> "The test QR code is invalid"
    "二维码签名验证失败" -> "QR code signature verification failed"
    "配对二维码已过期" -> "The pairing QR code has expired"
    "配对二维码已过期，请让电脑端重新生成" -> "The pairing QR code has expired. Ask the computer to generate a new one"
    "电脑端正在进行，文字可以排队；图片和文件请等当前任务完成后发送" -> "The computer is busy. Text can be queued; wait for the task to finish before sending images or files"
    "电脑端没有接受这条消息，请确认桌面 Codex 已打开该对话" -> "The computer did not accept this message. Make sure Desktop Codex has this conversation open"
    "无法读取电脑端会话" -> "Unable to read conversations from the computer"
    "置顶操作失败" -> "Unable to pin the conversation"
    "归档操作失败" -> "Unable to archive the conversation"
    "无法读取电脑端新建对话选项" -> "Unable to read new conversation options"
    "无法在电脑端新建对话" -> "Unable to create a conversation on the computer"
    "无法读取电脑端确认请求" -> "Unable to read the computer's approval request"
    "无法提交确认结果" -> "Unable to submit the approval result"
    "无法停止电脑端任务" -> "Unable to stop the computer task"
    "通知权限未授予，通知模式已保存，但暂时无法发送通知" -> "Notification permission was not granted. The notification mode was saved, but notifications cannot be sent yet"
    "关闭" -> "Off"
    "关闭推理" -> "Off"
    "极低" -> "Minimal"
    "极简" -> "Minimal"
    "低" -> "Low"
    "高" -> "High"
    "极高" -> "Very high"
    "最高" -> "Max"
    "超高" -> "Ultra"
    "默认推理" -> "Default reasoning"
    "请求批准" -> "Ask for approval"
    "帮我批准" -> "Approve for me"
    "完全访问权限" -> "Full access"
    "未读取" -> "Unavailable"
    "等待确认远程命令" -> "Awaiting confirmation for a remote command"
    "电脑端正在等待你的允许" -> "The computer is waiting for your approval"
    "运行需要确认的远程命令" -> "Run a remote command that needs approval"
    "电脑端正在等待你的允许，确认后会继续执行。" -> "The computer is waiting for your approval and will continue after confirmation."
    "把 Codex 的执行状态整理给我看" -> "Show me the Codex execution status"
    "过程会按事件顺序显示，进行中的任务直接跟随对应行更新。" -> "Events appear in order, and running tasks update beside their row."
    "第一次思考更新：分析任务目标。" -> "First thinking update: analyze the task goal."
    "第二次思考更新：确认执行步骤。" -> "Second thinking update: confirm the execution steps."
    "开始检查电脑端工作区。" -> "Start checking the computer workspace."
    "第一条命令输出。" -> "Output from the first command."
    "第二条命令输出：同类命令更新覆盖上一条。" -> "Output from the second command: the latest update replaces the previous one."
    "搜索结果已返回。" -> "Search results returned."
    "调用远程工具并等待结果。" -> "Call a remote tool and wait for the result."
    "已写入目标文件。" -> "The target file was written."
    "计划已更新。" -> "The plan was updated."
    "已生成图片预览。" -> "Image preview generated."
    "其他操作已完成。" -> "The other operation is complete."
    "整理 CloudX 远程控制项目" -> "Organize the CloudX remote-control project"
    "我正在检查工作区结构，并准备整理远程控制相关模块。" -> "I am checking the workspace structure and preparing the remote-control modules."
    "正在扫描工作区，等待电脑端返回任务进度" -> "Scanning the workspace and waiting for task progress"
    "正在扫描工作区，等待电脑端返回任务进度。" -> "Scanning the workspace and waiting for task progress from the computer."
    "检查远程电脑端工作区" -> "Check the remote computer workspace"
    "任务已完成。执行记录默认收起，点击执行行可以展开查看具体命令。" -> "Task complete. Execution details are collapsed by default; tap a run row to view the command."
    "命令已完成。" -> "The command is complete."
    "文件修改已完成。" -> "The file change is complete."
    "实现扫码配对流程" -> "Implement QR pairing"
    "已完成：配对界面和连接状态展示" -> "Completed: pairing UI and connection status"
    "检查 Windows Connector 状态" -> "Check Windows Connector status"
    "等待下一步指令" -> "Waiting for the next instruction"
    "同步设置页面" -> "Sync the settings page"
    "已完成：外观、通知和设备配对设置" -> "Completed: appearance, notifications, and pairing settings"
    "修复扫码连接超时" -> "Fix QR connection timeout"
    "任务失败：未收到电脑端响应" -> "Failed: no response from the computer"
    "任务失败：未收到电脑端响应。请确认电脑端项目已经启动。" -> "Task failed: no response from the computer. Make sure the computer project is running."
    "未在等待时间内收到电脑端响应。" -> "No response from the computer arrived in time."
    "测试远程任务恢复" -> "Test remote task recovery"
    "已中断：可以从最近消息继续" -> "Interrupted: continue from the latest message"
    "任务已中断，可以从最近消息继续。" -> "Task interrupted. You can continue from the latest message."
    "任务在电脑端停止，当前页面保留最近消息。" -> "The task stopped on the computer; this page keeps the latest messages."
    "整理项目文档" -> "Organize project documentation"
    "已完成项目结构检查" -> "Project structure check completed"
    "已完成：这是电脑端 Codex 返回的对话内容。你可以展开下面的过程行查看任务详情。" -> "Complete: this is the conversation returned by Codex on the computer. Expand the process row below for details."
    "已完成本次任务。这里会显示电脑端返回的过程和任务结果。" -> "This task is complete. The process and result returned by the computer appear here."
    "远程 Codex 对话" -> "Remote Codex conversation"
    "电脑端请求执行一项需要确认的操作。" -> "The computer requested an action that needs your approval."
    "等待电脑端确认请求。" -> "Waiting for an approval request from the computer."
    "已收到这条消息。电脑端返回内容后，会在这里显示完整结果。" -> "Message received. The complete result will appear here when the computer responds."
    "已添加内容" -> "Content added"
    "电脑端附件不可用" -> "The computer attachment is unavailable"
    "当前对话没有可读取的电脑端图片" -> "No computer image is available in this conversation"
    "当前对话没有可读取的电脑端文件" -> "No computer file is available in this conversation"
    "附件" -> "Attachment"
    "内容较长，已截取最近详情。" -> "Long content; showing the latest details."
    "每次最多添加 5 个附件" -> "You can add up to 5 attachments at a time"
    "单个附件不能超过 20 MB" -> "Each attachment must be 20 MB or smaller"
    "允许在工作区内读写" -> "Read and write in the workspace"
    "仅读取，不修改文件" -> "Read only; do not modify files"
    "读取中" -> "Reading"
    "保存图片到本地" -> "Save image locally"
    "正在保存图片" -> "Saving image"
    "分享图片" -> "Share image"
    "图片无法预览" -> "Image cannot be previewed"
    "需要存储权限才能保存图片" -> "Storage permission is required to save the image"
    "无法准备图片" -> "Unable to prepare the image"
    "无法写入图片" -> "Unable to write the image"
    "无法写入本地图片" -> "Unable to write the local image"
    "无法创建本地图片" -> "Unable to create a local image"
    "无法完成本地图片保存" -> "Unable to save the image locally"
    "图片内容为空" -> "Image content is empty"
    "图片超过 100 MB，无法直接预览" -> "Images over 100 MB cannot be previewed directly"
    "图片尺寸无效" -> "Invalid image dimensions"
    "图片无法解码" -> "Unable to decode the image"
    "SVG 图片超过 8 MB" -> "SVG images over 8 MB are not supported"
    "SVG 不允许外部实体" -> "External entities are not allowed in SVG"
    "SVG 图片无法预览" -> "SVG image cannot be previewed"
    "正在打开相机…" -> "Opening camera…"
    "远程 Agent" -> "Remote agent"
    "对应方式" -> "selected connection method"
    "RTC 配对超时，请检查手机网络可访问 HTTPS 信令和 TURN 中继" -> "RTC pairing timed out. Check that the phone can reach HTTPS signaling and the TURN relay"
    "无法解析信令服务器地址，请确认手机网络可访问该 HTTPS 地址" -> "Unable to resolve the signaling server. Check that the phone can reach its HTTPS address"
    "无法连接信令服务器，请确认 HTTPS 服务已启动并可从公网访问" -> "Unable to connect to the signaling server. Check that its HTTPS service is running and publicly reachable"
    "配对协议版本不受支持" -> "The pairing protocol version is not supported"
    "二维码 offerId 无效" -> "The QR code offer ID is invalid"
    "二维码 deviceId 无效" -> "The QR code device ID is invalid"
    "二维码公钥不一致" -> "The QR code public key does not match"
    "二维码 nonce 无效" -> "The QR code nonce is invalid"
    "二维码过期时间不一致" -> "The QR code expiration time does not match"
    "二维码缺少 Connector 签名" -> "The QR code is missing the Connector signature"
    "HTTP 传输缺少 endpoint" -> "The HTTP transport is missing an endpoint"
    "WebRTC 配对缺少 signaling endpoint" -> "WebRTC pairing is missing a signaling endpoint"
    "WebRTC signaling endpoint 无效" -> "The WebRTC signaling endpoint is invalid"
    "正式 WebRTC 信令必须使用 HTTPS" -> "Production WebRTC signaling must use HTTPS"
    "电脑端连接必须使用 HTTPS" -> "The computer connection must use HTTPS"
    "WebRTC 传输缺少 Android Context" -> "The WebRTC transport is missing an Android context"
    "无法解析电脑端地址" -> "Unable to resolve the computer address"
    "WebRTC 尚未接入 HTTP 客户端" -> "WebRTC is not connected to the HTTP client"
    "不接受客户端证书" -> "Client certificates are not accepted"
    "电脑端证书缺失" -> "The computer certificate is missing"
    "电脑端证书指纹与配对信息不一致" -> "The computer certificate fingerprint does not match the pairing information"
    "电脑端证书指纹格式无效" -> "The computer certificate fingerprint format is invalid"
    "无法创建 WebRTC PeerConnection" -> "Unable to create the WebRTC peer connection"
    "无法创建 WebRTC DataChannel" -> "Unable to create the WebRTC data channel"
    "创建 WebRTC PeerConnection" -> "Create WebRTC peer connection"
    "获取桌面端 SDP offer" -> "Get the computer SDP offer"
    "应用桌面端 SDP offer" -> "Apply the computer SDP offer"
    "创建手机端 SDP answer" -> "Create the phone SDP answer"
    "应用手机端 SDP answer" -> "Apply the phone SDP answer"
    "等待手机端 ICE candidates" -> "Wait for phone ICE candidates"
    "提交手机端 SDP answer" -> "Submit the phone SDP answer"
    "提交手机端 ICE candidates" -> "Submit phone ICE candidates"
    "等待桌面端 ICE candidates 和 DataChannel" -> "Wait for computer ICE candidates and the data channel"
    "创建手机端 SDP offer" -> "Create the phone SDP offer"
    "应用手机端 SDP offer" -> "Apply the phone SDP offer"
    "请求桌面端 SDP answer" -> "Request the computer SDP answer"
    "应用桌面端 SDP answer" -> "Apply the computer SDP answer"
    "WebRTC DataChannel 已关闭" -> "The WebRTC data channel is closed"
    "WebRTC DataChannel 发送失败" -> "Unable to send through the WebRTC data channel"
    "WebRTC SDP 为空" -> "The WebRTC SDP is empty"
    "WebRTC SDP 创建失败" -> "Unable to create the WebRTC SDP"
    "WebRTC SDP 设置失败" -> "Unable to set the WebRTC SDP"
    "缺少通知权限，请在系统设置中允许 CloudX 发送通知" -> "Notification permission is missing. Allow CloudX notifications in system settings"
    "系统通知已关闭，请在系统设置中允许 CloudX 发送通知" -> "System notifications are disabled. Allow CloudX notifications in system settings"
    "无法打开导出文件" -> "Unable to open the export file"
    else -> value
}

private val WEBRTC_SIGNALING_FAILURE_PREFIXES = listOf(
    "WebRTC offer 获取失败" to "Unable to get the WebRTC offer",
    "WebRTC answer 登记失败" to "Unable to register the WebRTC answer",
    "WebRTC ICE 登记失败" to "Unable to register WebRTC ICE candidates",
    "WebRTC ICE 获取失败" to "Unable to get WebRTC ICE candidates",
    "WebRTC 信令失败" to "WebRTC signaling failed",
)

internal fun RemoteStrings.localizedPairingError(value: String): String {
    return if (isEnglish) displayText(value) else value
}

internal fun RemoteStrings.localizedCacheMessage(cleared: Boolean): String =
    if (cleared) t("图片预览缓存已清理") else t("缓存清理失败")

internal fun RemoteStrings.localizedDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis.coerceAtLeast(0L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds / 60L) % 60L
    val seconds = totalSeconds % 60L
    return if (isEnglish) {
        when {
            hours > 0 -> "%dh %02dm".format(Locale.US, hours, minutes)
            minutes > 0 -> "%dm %02ds".format(Locale.US, minutes, seconds)
            else -> "%ds".format(Locale.US, seconds)
        }
    } else {
        when {
            hours > 0 -> "%d时%02d分".format(Locale.CHINA, hours, minutes)
            minutes > 0 -> "%d分%02d秒".format(Locale.CHINA, minutes, seconds)
            else -> "%d秒".format(Locale.CHINA, seconds)
        }
    }
}

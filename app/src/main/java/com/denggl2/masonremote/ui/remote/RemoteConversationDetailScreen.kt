package com.denggl2.masonremote.ui.remote

import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denggl2.masonremote.R
import com.denggl2.masonremote.ui.chat.ChatBackdropBlur
import com.denggl2.masonremote.ui.chat.ChatGlassDropdown
import com.denggl2.masonremote.ui.chat.ChatGlassMaterial
import com.denggl2.masonremote.ui.chat.ChatSurfaceRole
import com.denggl2.masonremote.ui.chat.LocalChatBackdropState
import com.denggl2.masonremote.ui.chat.captureChatBackdrop
import com.denggl2.masonremote.ui.chat.glassClickable
import com.denggl2.masonremote.ui.chat.masonGlassShadow
import com.denggl2.masonremote.ui.chat.rememberChatBackdropState
import com.denggl2.masonremote.ui.FigmaSvgAsset
import com.denggl2.masonremote.ui.theme.LocalInterfaceEffects
import com.denggl2.masonremote.ui.theme.MasonAlertDialog as AlertDialog
import com.denggl2.masonremote.data.AndroidDeviceIdentityStore
import com.denggl2.masonremote.transport.ConnectorConversationActivityKind
import com.denggl2.masonremote.transport.ConnectorConversationActivityPayload
import com.denggl2.masonremote.transport.ConnectorConversationActivityStatus
import com.denggl2.masonremote.transport.ConnectorConversationAttachmentPayload
import com.denggl2.masonremote.transport.ConnectorApprovalRequest
import com.denggl2.masonremote.transport.ConnectorConversationDetailPayload
import com.denggl2.masonremote.transport.ConnectorConversationRole
import com.denggl2.masonremote.transport.ConnectorAttachmentKind
import com.denggl2.masonremote.transport.ConnectorComposerOptions
import com.denggl2.masonremote.transport.ConnectorModelOption
import com.denggl2.masonremote.transport.ConnectorMessageRequest
import com.denggl2.masonremote.transport.ConnectorPermissionProfileOption
import com.denggl2.masonremote.transport.ConnectorReasoningEffortOption
import com.denggl2.masonremote.transport.ConnectorSkillOption
import com.denggl2.masonremote.transport.ConnectorSkillSelection
import com.denggl2.masonremote.transport.ConnectorExecutionStatus
import com.denggl2.masonremote.transport.PairedConnector
import com.denggl2.masonremote.transport.RemoteConnectorClient
import com.denggl2.masonremote.transport.displayName
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DetailMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val attachments: List<ConnectorConversationAttachmentPayload> = emptyList(),
)

private enum class DetailActivityKind {
    THINKING,
    COMMAND,
    WEB_SEARCH,
    TOOL,
    FILE_CHANGE,
    COMMENTARY,
    PLAN,
    IMAGE,
    OTHER,
}

private enum class DetailActivityStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    INTERRUPTED,
    FAILED,
}

private data class DetailActivity(
    val id: String,
    val kind: DetailActivityKind,
    val title: String,
    val text: String,
    val status: DetailActivityStatus,
)

private data class DetailDemo(
    val messages: List<DetailMessage>,
    val activities: List<DetailActivity>,
    val running: Boolean,
)

private data class DetailSelectorItem(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
)

private data class PendingDetailAttachment(
    val kind: ConnectorAttachmentKind,
    val name: String,
    val uri: Uri,
    val mimeType: String?,
)

private val DetailTopFadeHeight = 30.dp
private const val MaxVisibleDetailActivities = 9
private const val MaxVisibleDetailMessages = 40
private const val MaxDetailMessageCharacters = 4_000
private const val MaxPendingDetailAttachments = 5
private const val MaxDetailAttachmentBytes = 20L * 1024L * 1024L
private const val DETAIL_POLL_INTERVAL_MILLIS = 1_500L

@Composable
internal fun RemoteConversationDetailScreen(
    threadId: String,
    pairedConnector: PairedConnector? = null,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val remoteClient = remember(pairedConnector) {
        pairedConnector?.let {
            RemoteConnectorClient(it, AndroidDeviceIdentityStore(), context.applicationContext)
        }
    }
    val agentLabel = pairedConnector?.agentKind?.displayName() ?: "远程 Agent"
    var remoteDetail by remember(threadId) { mutableStateOf<ConnectorConversationDetailPayload?>(null) }
    var remoteError by remember(threadId) { mutableStateOf<String?>(null) }
    var loadingAttachmentId by remember(threadId) { mutableStateOf<String?>(null) }
    var previewImage by remember(threadId) { mutableStateOf<RemotePreviewImage?>(null) }
    var composerOptions by remember(threadId) { mutableStateOf<ConnectorComposerOptions?>(null) }
    var optionsError by remember(threadId) { mutableStateOf<String?>(null) }
    var optionsLoading by remember(threadId) { mutableStateOf(false) }
    LaunchedEffect(remoteClient, threadId) {
        if (remoteClient == null || threadId.startsWith("demo-")) return@LaunchedEffect
        while (isActive) {
            runCatching {
                remoteClient.readConversation(checkNotNull(pairedConnector).deviceId, threadId)
            }.onSuccess {
                remoteDetail = it
                remoteError = null
            }.onFailure {
                if (remoteDetail == null) {
                    remoteError = it.message ?: "无法读取电脑端对话"
                }
            }
            delay(DETAIL_POLL_INTERVAL_MILLIS)
        }
    }
    LaunchedEffect(remoteClient, threadId) {
        if (remoteClient == null || pairedConnector == null || threadId.startsWith("demo-")) return@LaunchedEffect
        optionsLoading = true
        runCatching {
            remoteClient.composerOptions(checkNotNull(pairedConnector).deviceId, threadId)
        }.onSuccess {
            composerOptions = it
            optionsError = null
        }.onFailure {
            optionsError = it.message ?: "无法读取 $agentLabel 配置"
        }
        optionsLoading = false
    }
    var pendingApproval by remember(threadId) { mutableStateOf<ConnectorApprovalRequest?>(null) }
    var approvalBusy by remember(threadId) { mutableStateOf(false) }
    var approvalError by remember(threadId) { mutableStateOf<String?>(null) }
    LaunchedEffect(remoteClient, pairedConnector, threadId) {
        if (remoteClient == null || pairedConnector == null || threadId.startsWith("demo-")) return@LaunchedEffect
        while (isActive) {
            runCatching {
                remoteClient.pendingApprovals(checkNotNull(pairedConnector).deviceId, threadId)
            }.onSuccess { approvals ->
                pendingApproval = approvals.firstOrNull()
                if (approvals.isEmpty()) approvalError = null
            }.onFailure { error ->
                approvalError = error.message ?: "无法读取电脑端确认请求"
            }
            delay(if (pendingApproval == null) 1_000L else 500L)
        }
    }
    val detailLoading = !threadId.startsWith("demo-") && remoteDetail == null && remoteError == null
    val demo = when {
        threadId.startsWith("demo-") -> remember(threadId) { demoRemoteDetail(threadId) }
        remoteDetail != null -> remoteDetail!!.toDetailDemo()
        else -> DetailDemo(
            messages = remoteError?.let {
                listOf(DetailMessage("remote-error", false, it))
            }.orEmpty(),
            activities = emptyList(),
            running = false,
        )
    }
    var messages by remember(threadId) { mutableStateOf(demo.messages) }
    var draft by remember(threadId) { mutableStateOf("") }
    var expandedActivityIds by remember(threadId) { mutableStateOf(emptySet<String>()) }
    val demoOptions = remember(threadId) { demoComposerOptions() }
    val activeComposerOptions = composerOptions ?: if (threadId.startsWith("demo-")) demoOptions else ConnectorComposerOptions()
    var selectedModelId by remember(threadId) { mutableStateOf<String?>(null) }
    var selectedReasoningEffort by remember(threadId) { mutableStateOf<String?>(null) }
    var selectedPermissionProfileId by remember(threadId) { mutableStateOf<String?>(null) }
    var selectedSkill by remember(threadId) { mutableStateOf<ConnectorSkillOption?>(null) }
    var attachments by remember(threadId) { mutableStateOf(emptyList<PendingDetailAttachment>()) }
    var isSending by remember(threadId) { mutableStateOf(false) }
    var isInterrupting by remember(threadId) { mutableStateOf(false) }
    var sendError by remember(threadId) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val topInset = safeDrawingPadding.calculateTopPadding()
    val bottomInset = safeDrawingPadding.calculateBottomPadding()
    var composerHeightPx by remember { mutableIntStateOf(0) }
    val composerHeight = if (composerHeightPx > 0) {
        with(androidx.compose.ui.platform.LocalDensity.current) { composerHeightPx.toDp() }
    } else {
        bottomInset + 118.dp
    }
    val interfaceEffects = LocalInterfaceEffects.current
    val backdropState = rememberChatBackdropState(interfaceEffects.backdropBlurEnabled)

    LaunchedEffect(activeComposerOptions) {
        val model = activeComposerOptions.models.firstOrNull { it.id == selectedModelId }
            ?: activeComposerOptions.models.firstOrNull { it.id == activeComposerOptions.currentModelId }
            ?: activeComposerOptions.models.firstOrNull { it.isDefault }
            ?: activeComposerOptions.models.firstOrNull()
        if (selectedModelId == null || activeComposerOptions.models.none { it.id == selectedModelId }) {
            selectedModelId = model?.id
        }
        val selectedModel = model ?: activeComposerOptions.models.firstOrNull { it.id == selectedModelId }
        if (
            selectedReasoningEffort == null ||
                selectedModel?.supportedReasoningEfforts?.none { it.id == selectedReasoningEffort } == true
        ) {
            selectedReasoningEffort = activeComposerOptions.currentReasoningEffort
                ?.takeIf { effort -> selectedModel?.supportedReasoningEfforts?.any { it.id == effort } == true }
                ?: selectedModel?.defaultReasoningEffort
                    ?.takeIf { effort -> selectedModel.supportedReasoningEfforts.any { it.id == effort } }
                ?: selectedModel?.supportedReasoningEfforts?.firstOrNull()?.id
        }
        if (
            selectedPermissionProfileId == null ||
                activeComposerOptions.permissionProfiles.none { it.id == selectedPermissionProfileId && it.allowed }
        ) {
            selectedPermissionProfileId = activeComposerOptions.currentPermissionProfileId
                ?.takeIf { id -> activeComposerOptions.permissionProfiles.any { it.id == id && it.allowed } }
                ?: activeComposerOptions.permissionProfiles.firstOrNull { it.allowed }?.id
        }
        selectedSkill = selectedSkill?.let { skill ->
            activeComposerOptions.skills.firstOrNull { it.path == skill.path }
        }
    }

    fun addAttachment(kind: ConnectorAttachmentKind, uri: Uri) {
        if (attachments.size >= MaxPendingDetailAttachments) {
            sendError = "每次最多添加 $MaxPendingDetailAttachments 个附件"
            return
        }
        if (attachments.any { it.uri == uri }) return
        val size = queryAttachmentSize(context, uri)
        if (size != null && size > MaxDetailAttachmentBytes) {
            sendError = "单个附件不能超过 20 MB"
            return
        }
        attachments = attachments + PendingDetailAttachment(
            kind = kind,
            name = queryAttachmentName(context, uri),
            uri = uri,
            mimeType = context.contentResolver.getType(uri),
        )
        sendError = null
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addAttachment(ConnectorAttachmentKind.IMAGE, it)
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addAttachment(ConnectorAttachmentKind.FILE, it)
        }
    }

    LaunchedEffect(remoteDetail, remoteError) {
        if (!threadId.startsWith("demo-")) messages = demo.messages
    }

    LaunchedEffect(messages.size, demo.activities.size) {
        val target = listState.layoutInfo.totalItemsCount - 1
        if (target >= 0) listState.scrollToItem(target)
    }

    fun sendMessage() {
        val text = draft.trim()
        if (
            (text.isBlank() && attachments.isEmpty() && selectedSkill == null) ||
                isSending ||
                demo.running
        ) return
        val pendingAttachments = attachments
        val pendingSkill = selectedSkill
        val requestText = text.ifBlank { "已添加内容" }
        messages = messages + DetailMessage(
            id = "user-${messages.size}-${requestText.hashCode()}",
            isUser = true,
            text = requestText,
        )
        sendError = null
        if (remoteClient == null || pairedConnector == null || threadId.startsWith("demo-")) {
            scope.launch {
                isSending = true
                kotlinx.coroutines.delay(550)
                messages = messages + DetailMessage(
                    id = "assistant-${messages.size}",
                    isUser = false,
                    text = "已收到这条消息。电脑端返回内容后，会在这里显示完整结果。",
                )
                draft = ""
                attachments = emptyList()
                selectedSkill = null
                isSending = false
            }
            return
        }
        scope.launch {
            isSending = true
            runCatching {
                val uploadedAttachments = pendingAttachments.map { pending ->
                    remoteClient.uploadAttachment(
                        deviceId = pairedConnector.deviceId,
                        kind = pending.kind,
                        name = pending.name,
                        mimeType = pending.mimeType,
                        bytes = readAttachmentBytes(context, pending.uri),
                    )
                }
                remoteClient.sendMessage(
                    deviceId = pairedConnector.deviceId,
                    threadId = threadId,
                    request = ConnectorMessageRequest(
                        text = text,
                        attachmentIds = uploadedAttachments.map { it.attachmentId },
                        skill = pendingSkill?.let { ConnectorSkillSelection(it.name, it.path) },
                        modelId = selectedModelId,
                        reasoningEffort = selectedReasoningEffort,
                        permissionProfileId = selectedPermissionProfileId,
                    ),
                )
            }.onSuccess {
                draft = ""
                attachments = emptyList()
                selectedSkill = null
                runCatching {
                    remoteClient.readConversation(pairedConnector.deviceId, threadId)
                }.onSuccess { remoteDetail = it }
                    .onFailure { remoteError = it.message ?: "消息已发送，但无法刷新对话" }
            }.onFailure {
                sendError = it.message ?: "无法发送到电脑"
            }
            isSending = false
        }
    }

    fun openRemoteImage(attachment: ConnectorConversationAttachmentPayload) {
        if (attachment.kind != ConnectorAttachmentKind.IMAGE || loadingAttachmentId != null) return
        val client = remoteClient
        val connector = pairedConnector
        if (client == null || connector == null || threadId.startsWith("demo-")) {
            Toast.makeText(context, "当前对话没有可读取的电脑端图片", Toast.LENGTH_SHORT).show()
            return
        }
        loadingAttachmentId = attachment.attachmentId
        scope.launch {
            runCatching {
                client.downloadConversationAttachment(
                    deviceId = connector.deviceId,
                    threadId = threadId,
                    attachmentId = attachment.attachmentId,
                )
            }.onSuccess { bytes ->
                previewImage = RemotePreviewImage(
                    attachmentId = attachment.attachmentId,
                    name = attachment.name,
                    mimeType = attachment.mimeType,
                    bytes = bytes,
                )
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    "图片读取失败：${error.message ?: "电脑端附件不可用"}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            loadingAttachmentId = null
        }
    }

    fun resolveApproval(decision: String) {
        val approval = pendingApproval ?: return
        val client = remoteClient ?: return
        val connector = pairedConnector ?: return
        scope.launch {
            approvalBusy = true
            approvalError = null
            runCatching {
                client.resolveApproval(
                    deviceId = connector.deviceId,
                    threadId = threadId,
                    requestId = approval.requestId,
                    decision = decision,
                )
            }.onSuccess {
                pendingApproval = null
                runCatching { client.readConversation(connector.deviceId, threadId) }
                    .onSuccess { remoteDetail = it }
            }.onFailure { error ->
                approvalError = error.message ?: "无法提交确认结果"
            }
            approvalBusy = false
        }
    }

    fun interruptConversation() {
        val client = remoteClient ?: return
        val connector = pairedConnector ?: return
        if (threadId.startsWith("demo-") || isInterrupting) return
        scope.launch {
            isInterrupting = true
            runCatching {
                client.interruptConversation(
                    deviceId = connector.deviceId,
                    threadId = threadId,
                )
            }.onSuccess {
                runCatching { client.readConversation(connector.deviceId, threadId) }
                    .onSuccess { remoteDetail = it }
            }.onFailure { error ->
                sendError = error.message ?: "无法停止电脑端任务"
            }
            isInterrupting = false
        }
    }

    CompositionLocalProvider(LocalChatBackdropState provides backdropState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .captureChatBackdrop(backdropState),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = topInset + 76.dp,
                        end = 16.dp,
                        bottom = composerHeight + 6.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(messages, key = DetailMessage::id) { message ->
                        DetailMessageRow(
                            message = message,
                            deviceId = pairedConnector?.deviceId,
                            loadingAttachmentId = loadingAttachmentId,
                            remoteClient = remoteClient,
                            threadId = threadId,
                            onPreviewAttachment = ::openRemoteImage,
                        )
                    }
                    if (!detailLoading) {
                        item(key = "activities") {
                            DetailProcessPanel(
                                activities = demo.activities,
                                running = demo.running || isSending,
                                expandedActivityIds = expandedActivityIds,
                                onToggle = { activityId ->
                                    expandedActivityIds = if (activityId in expandedActivityIds) {
                                        expandedActivityIds - activityId
                                    } else {
                                        expandedActivityIds + activityId
                                    }
                                },
                            )
                        }
                    }
                    sendError?.let { error ->
                        item(key = "send-error") {
                            Text(
                                text = "发送失败：$error",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 2.dp),
                            )
                        }
                    }
                }
            }

            if (!detailLoading) {
                DetailEdgeFades(
                    topInset = topInset,
                    bottomHeight = composerHeight,
                    background = MaterialTheme.colorScheme.background,
                )
            }

            RemoteBackButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = topInset + 8.dp),
            )

            if (detailLoading) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInset + 8.dp)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.7.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "加载中",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                DetailComposer(
                    draft = draft,
                    model = activeComposerOptions.models.firstOrNull { it.id == selectedModelId },
                    agentLabel = agentLabel,
                    reasoning = selectedReasoningEffort,
                    permission = selectedPermissionProfileId,
                    options = activeComposerOptions,
                    optionsLoading = optionsLoading,
                    optionsError = optionsError,
                    attachments = attachments,
                    selectedSkill = selectedSkill,
                    running = demo.running || isSending,
                    onDraftChange = { draft = it },
                    onSend = ::sendMessage,
                    onInterrupt = ::interruptConversation,
                    interrupting = isInterrupting,
                    onAddImage = { imagePicker.launch(arrayOf("image/*")) },
                    onAddFile = { filePicker.launch(arrayOf("*/*")) },
                    onRemoveAttachment = { index -> attachments = attachments.filterIndexed { i, _ -> i != index } },
                    onClearSkill = { selectedSkill = null },
                    onSkillSelected = { selectedSkill = it },
                    onModelChange = { id ->
                        selectedModelId = id
                        val model = activeComposerOptions.models.firstOrNull { it.id == id }
                        selectedReasoningEffort = model?.defaultReasoningEffort
                            ?.takeIf { effort -> model.supportedReasoningEfforts.any { it.id == effort } }
                            ?: model?.supportedReasoningEfforts?.firstOrNull()?.id
                    },
                    onReasoningChange = { selectedReasoningEffort = it },
                    onPermissionChange = { selectedPermissionProfileId = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { composerHeightPx = it.height },
                )
            }
            pendingApproval?.let { approval ->
                AlertDialog(
                    onDismissRequest = {},
                    dismissButton = {
                        TextButton(
                            enabled = !approvalBusy,
                            onClick = { resolveApproval("decline") },
                        ) { Text("拒绝") }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !approvalBusy,
                            onClick = { resolveApproval("accept") },
                        ) { Text("允许") }
                    },
                    title = { Text(approval.title) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                approval.detail.ifBlank { "这项操作需要你的确认。" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            approvalError?.let { error ->
                                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                )
            }

            previewImage?.let { image ->
                RemoteImagePreviewDialog(
                    image = image,
                    onDismiss = { previewImage = null },
                    onShare = { shareRemoteImage(context, image) },
                )
            }
        }
    }
}

@Composable
private fun DetailMessageRow(
    message: DetailMessage,
    deviceId: String?,
    loadingAttachmentId: String?,
    remoteClient: RemoteConnectorClient?,
    threadId: String,
    onPreviewAttachment: (ConnectorConversationAttachmentPayload) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .then(
                    if (message.isUser) {
                        Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            RoundedCornerShape(8.dp),
                        )
                    } else {
                        Modifier
                    },
                )
            .padding(horizontal = if (message.isUser) 14.dp else 2.dp, vertical = 10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
                val imageAttachments = message.attachments.filter {
                    it.kind == ConnectorAttachmentKind.IMAGE
                }
                if (imageAttachments.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = imageAttachments,
                            key = { attachment -> "image-${attachment.attachmentId}" },
                        ) { attachment ->
                            RemoteConversationImageThumbnail(
                                attachment = attachment,
                                deviceId = deviceId,
                                loading = loadingAttachmentId == attachment.attachmentId,
                                onClick = { onPreviewAttachment(attachment) },
                                remoteClient = remoteClient,
                                threadId = threadId,
                            )
                        }
                    }
                }
                message.attachments.filterNot {
                    it.kind == ConnectorAttachmentKind.IMAGE
                }.forEach { attachment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
                            .clickable(
                                enabled = false,
                                onClick = {},
                            )
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                attachment.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "电脑文件 · ${formatRemoteAttachmentSize(attachment.sizeBytes)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteConversationImageThumbnail(
    attachment: ConnectorConversationAttachmentPayload,
    deviceId: String?,
    loading: Boolean,
    onClick: () -> Unit,
    remoteClient: RemoteConnectorClient?,
    threadId: String,
) {
    val thumbnailState by produceState<RemoteThumbnailState>(
        initialValue = RemoteThumbnailState.Loading,
        key1 = remoteClient,
        key2 = deviceId,
        key3 = "$threadId/${attachment.attachmentId}",
    ) {
        val client = remoteClient
        val connectorId = deviceId
        if (client == null || connectorId.isNullOrBlank()) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                RemoteThumbnailState.Ready(
                    decodeRemoteBitmap(
                        client.downloadConversationAttachment(
                            deviceId = connectorId,
                            threadId = threadId,
                            attachmentId = attachment.attachmentId,
                        ),
                        maxEdge = 320,
                    ),
                )
            }.getOrElse { error ->
                RemoteThumbnailState.Failed(error.message ?: "图片无法预览")
            }
        }
    }
    val bitmap = (thumbnailState as? RemoteThumbnailState.Ready)?.bitmap
    DisposableEffect(bitmap) {
        onDispose { bitmap?.takeUnless(Bitmap::isRecycled)?.recycle() }
    }
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
            .clickable(
                enabled = !loading && thumbnailState is RemoteThumbnailState.Ready,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (thumbnailState) {
            RemoteThumbnailState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
            is RemoteThumbnailState.Ready -> Image(
                bitmap = checkNotNull(bitmap).asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            is RemoteThumbnailState.Failed -> Icon(
                Icons.Outlined.Image,
                contentDescription = "图片预览失败",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(22.dp),
            )
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(21.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private sealed interface RemoteThumbnailState {
    data object Loading : RemoteThumbnailState
    data class Ready(val bitmap: Bitmap) : RemoteThumbnailState
    data class Failed(val message: String) : RemoteThumbnailState
}

@Composable
private fun DetailProcessPanel(
    activities: List<DetailActivity>,
    running: Boolean,
    expandedActivityIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    val visibleActivities = mergeDetailActivities(activities)
        .takeLast(MaxVisibleDetailActivities)
        .ifEmpty {
        listOf(
            DetailActivity(
                id = "execution-idle",
                kind = DetailActivityKind.OTHER,
                title = "执行过程",
                text = "暂无进行中的操作。",
                status = if (running) DetailActivityStatus.RUNNING else DetailActivityStatus.IDLE,
            ),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                RoundedCornerShape(8.dp),
            )
            .animateContentSize(),
    ) {
        Column {
        visibleActivities.forEachIndexed { index, activity ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.09f))
            }
            val expanded = activity.id in expandedActivityIds
            val canExpand = activity.text.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (canExpand) Modifier.glassClickable { onToggle(activity.id) } else Modifier)
                    .detailActivityShimmer(activity.status == DetailActivityStatus.RUNNING)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FigmaSvgAsset(
                        assetPath = detailActivityAsset(activity.kind),
                        modifier = Modifier.size(18.dp),
                        darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = activity.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (activity.status == DetailActivityStatus.RUNNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = detailActivityStatusLabel(activity.status),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    if (canExpand) {
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = if (expanded) "收起过程" else "展开过程",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(19.dp)
                                .rotate(if (expanded) 90f else 0f),
                        )
                    }
                }
            }
            if (expanded && canExpand) {
                Text(
                    text = activity.text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 42.dp, end = 12.dp, bottom = 12.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun Modifier.detailActivityShimmer(active: Boolean): Modifier {
    if (!active) return this
    val transition = rememberInfiniteTransition(label = "detail_activity_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "detail_activity_shimmer_progress",
    )
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f)
    return drawWithContent {
        drawContent()
        val bandWidth = 64.dp.toPx()
        val startX = -bandWidth + (size.width + bandWidth * 2f) * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = androidx.compose.ui.geometry.Offset(startX, 0f),
                end = androidx.compose.ui.geometry.Offset(startX + bandWidth, size.height),
            ),
        )
    }
}

@Composable
private fun DetailComposer(
    draft: String,
    model: ConnectorModelOption?,
    agentLabel: String,
    reasoning: String?,
    permission: String?,
    options: ConnectorComposerOptions,
    optionsLoading: Boolean,
    optionsError: String?,
    attachments: List<PendingDetailAttachment>,
    selectedSkill: ConnectorSkillOption?,
    running: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onInterrupt: () -> Unit,
    interrupting: Boolean,
    onAddImage: () -> Unit,
    onAddFile: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onClearSkill: () -> Unit,
    onSkillSelected: (ConnectorSkillOption) -> Unit,
    onModelChange: (String) -> Unit,
    onReasoningChange: (String) -> Unit,
    onPermissionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addMenuExpanded by remember { mutableStateOf(false) }
    var skillMenuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        optionsError?.takeIf(String::isNotBlank)?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .height(40.dp)
                .padding(bottom = 6.dp),
            contentPadding = PaddingValues(start = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                DetailSelectorPill(
                    label = model?.displayName ?: if (optionsLoading) "读取中" else "选择模型",
                    items = options.models.map { DetailSelectorItem(it.id, it.displayName) },
                    selectedId = model?.id,
                    onSelect = { onModelChange(it) },
                )
            }
            item {
                DetailSelectorPill(
                    label = remoteReasoningLabel(reasoning),
                    items = model?.supportedReasoningEfforts?.map {
                        DetailSelectorItem(it.id, remoteReasoningLabel(it.id))
                    }.orEmpty(),
                    selectedId = reasoning,
                    onSelect = { onReasoningChange(it) },
                )
            }
            item {
                DetailSelectorPill(
                    label = remotePermissionLabel(permission),
                    items = options.permissionProfiles.map {
                        DetailSelectorItem(it.id, remotePermissionLabel(it.id), it.allowed)
                    },
                    selectedId = permission,
                    onSelect = { onPermissionChange(it) },
                )
            }
        }

        if (attachments.isNotEmpty() || selectedSkill != null) {
            Spacer(Modifier.height(3.dp))
            // Keep context chips outside the glass composer so their surface does
            // not create a second blurred layer over the input panel.
            DetailComposerContextStrip(
                attachments = attachments,
                selectedSkill = selectedSkill,
                onRemoveAttachment = onRemoveAttachment,
                onClearSkill = onClearSkill,
            )
            Spacer(Modifier.height(3.dp))
        }

        val panelShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .masonGlassShadow(cornerRadius = 18.dp)
                .clip(panelShape),
        ) {
            ChatGlassMaterial(
                shape = panelShape,
                cornerRadius = 18.dp,
                role = ChatSurfaceRole.Large,
                blur = ChatBackdropBlur.Soft,
                refraction = true,
                blurredAlpha = 0.80f,
                fallbackAlpha = 0.99f,
            )
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .glassClickable { addMenuExpanded = !addMenuExpanded },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "添加",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ChatGlassDropdown(
                            expanded = addMenuExpanded,
                            onDismissRequest = { addMenuExpanded = false },
                            width = 160.dp,
                            cornerRadius = 30.dp,
                            alignEnd = false,
                            forceOpenAbove = true,
                            preserveInputFocus = true,
                        ) {
                            DetailComposerMenuRow("添加图片") {
                                addMenuExpanded = false
                                onAddImage()
                            }
                            DetailComposerMenuRow("添加文件") {
                                addMenuExpanded = false
                                onAddFile()
                            }
                            DetailComposerMenuRow("使用 Skill") {
                                addMenuExpanded = false
                                skillMenuExpanded = true
                            }
                        }
                        ChatGlassDropdown(
                            expanded = skillMenuExpanded,
                            onDismissRequest = { skillMenuExpanded = false },
                            width = 250.dp,
                            cornerRadius = 30.dp,
                            alignEnd = false,
                            forceOpenAbove = true,
                            preserveInputFocus = true,
                        ) {
                            when {
                                optionsLoading -> Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "正在读取远程 Skill",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                    )
                                }
                                options.skills.isEmpty() -> Text(
                                    "远程端暂无可用 Skill",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                )
                                else -> options.skills.forEach { skill ->
                                    DetailSkillMenuRow(skill) {
                                        skillMenuExpanded = false
                                        onSkillSelected(skill)
                                    }
                                }
                            }
                        }
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        enabled = !running,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 31.dp, max = 100.dp)
                            .padding(horizontal = 7.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxWidth()) {
                                if (draft.isBlank()) {
                                    Text(
                                        text = if (running) "$agentLabel 正在执行" else "向 $agentLabel 发送消息",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    val canSend = draft.isNotBlank() || attachments.isNotEmpty() || selectedSkill != null
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .glassClickable(
                                enabled = if (running) !interrupting else canSend,
                                onClick = if (running) onInterrupt else onSend,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (running) Icons.Outlined.Close else Icons.Outlined.ArrowUpward,
                            contentDescription = if (running) "停止" else "发送",
                            tint = if (running || (canSend && !running)) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailComposerContextStrip(
    attachments: List<PendingDetailAttachment>,
    selectedSkill: ConnectorSkillOption?,
    onRemoveAttachment: (Int) -> Unit,
    onClearSkill: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        selectedSkill?.let { skill ->
            item(key = "skill-${skill.path}") {
                DetailContextChip(
                    icon = Icons.Outlined.Extension,
                    label = skill.displayName,
                    onRemove = onClearSkill,
                )
            }
        }
        itemsIndexed(attachments, key = { _, attachment -> attachment.uri.toString() }) { index, attachment ->
            DetailContextChip(
                icon = if (attachment.kind == ConnectorAttachmentKind.IMAGE) {
                    Icons.Outlined.Image
                } else {
                    Icons.Outlined.Description
                },
                label = attachment.name,
                onRemove = { onRemoveAttachment(index) },
            )
        }
    }
}

@Composable
private fun DetailContextChip(
    icon: ImageVector,
    label: String,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), shape)
            .padding(start = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 176.dp),
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .glassClickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "移除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun DetailSkillMenuRow(skill: ConnectorSkillOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Extension, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = skill.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (skill.description.isNotBlank()) {
                Text(
                    text = skill.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DetailSelectorPill(
    label: String,
    items: List<DetailSelectorItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(180),
        label = "remote_detail_selector_arrow",
    )
    Box {
        val shape = RoundedCornerShape(999.dp)
        RemoteFloatingSurface(
            shape = shape,
            cornerRadius = 17.dp,
            modifier = Modifier.height(34.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .glassClickable(enabled = items.isNotEmpty()) { expanded = !expanded }
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp).rotate(arrowRotation),
                )
            }
        }
        ChatGlassDropdown(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            width = 150.dp,
            cornerRadius = 30.dp,
            alignEnd = false,
            forceOpenAbove = true,
            preserveInputFocus = true,
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassClickable(enabled = item.enabled) {
                                expanded = false
                                onSelect(item.id)
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.label,
                            color = if (item.enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            },
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (item.id == selectedId) {
                            Icon(Icons.Outlined.Check, contentDescription = "当前选项", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailComposerMenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}

@Composable
private fun BoxScope.DetailEdgeFades(
    topInset: Dp,
    bottomHeight: Dp,
    background: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(topInset + DetailTopFadeHeight)
            .background(
                Brush.verticalGradient(
                    listOf(background.copy(alpha = 0.96f), background.copy(alpha = 0.45f), Color.Transparent),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomHeight)
            .align(Alignment.BottomCenter)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, background.copy(alpha = 0.10f), background.copy(alpha = 0.32f)),
                ),
            ),
    )
}

private fun detailActivityStatusLabel(status: DetailActivityStatus): String = when (status) {
    DetailActivityStatus.IDLE -> "空闲"
    DetailActivityStatus.RUNNING -> "进行中"
    DetailActivityStatus.COMPLETED -> "完成"
    DetailActivityStatus.INTERRUPTED -> "已停止"
    DetailActivityStatus.FAILED -> "失败"
}

private fun detailActivityAsset(kind: DetailActivityKind): String = when (kind) {
    DetailActivityKind.THINKING -> "figma/detail_thinking.svg"
    DetailActivityKind.COMMAND -> "figma/detail_command.svg"
    DetailActivityKind.WEB_SEARCH -> "figma/detail_web_search.svg"
    DetailActivityKind.TOOL -> "figma/detail_tool.svg"
    DetailActivityKind.FILE_CHANGE -> "figma/detail_file_change.svg"
    DetailActivityKind.COMMENTARY -> "figma/detail_execution.svg"
    DetailActivityKind.PLAN -> "figma/detail_plan.svg"
    DetailActivityKind.IMAGE -> "figma/detail_image.svg"
    DetailActivityKind.OTHER -> "figma/detail_other.svg"
}

@Composable
private fun detailActivityIcon(kind: DetailActivityKind): ImageVector = when (kind) {
    DetailActivityKind.THINKING -> Icons.Outlined.Lightbulb
    DetailActivityKind.COMMAND -> Icons.Outlined.Terminal
    DetailActivityKind.WEB_SEARCH -> Icons.Outlined.Language
    DetailActivityKind.TOOL -> Icons.Outlined.Code
    DetailActivityKind.FILE_CHANGE -> Icons.Outlined.Edit
    DetailActivityKind.COMMENTARY -> Icons.Outlined.Description
    DetailActivityKind.PLAN -> Icons.Outlined.CheckCircle
    DetailActivityKind.IMAGE -> Icons.Outlined.Image
    DetailActivityKind.OTHER -> Icons.Outlined.Extension
}

private fun formatRemoteAttachmentSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(Locale.US, bytes / 1024.0)
    else -> "%.1f MB".format(Locale.US, bytes / 1024.0 / 1024.0)
}

private fun demoComposerOptions(): ConnectorComposerOptions = ConnectorComposerOptions(
    models = listOf(
        ConnectorModelOption(
            id = "demo-codex",
            model = "codex",
            displayName = "Codex",
            isDefault = true,
            defaultReasoningEffort = "medium",
            supportedReasoningEfforts = listOf(
                ConnectorReasoningEffortOption("low"),
                ConnectorReasoningEffortOption("medium"),
                ConnectorReasoningEffortOption("high"),
            ),
        ),
    ),
    permissionProfiles = listOf(
        ConnectorPermissionProfileOption(
            id = "workspace",
            description = "允许在工作区内读写",
            allowed = true,
        ),
        ConnectorPermissionProfileOption(
            id = "read-only",
            description = "仅读取，不修改文件",
            allowed = true,
        ),
    ),
    currentModelId = "demo-codex",
    currentReasoningEffort = "medium",
    currentPermissionProfileId = "workspace",
)

private fun remoteReasoningLabel(value: String?): String = when (value?.lowercase()) {
    "none" -> "关闭"
    "minimal" -> "极低"
    "low" -> "低"
    "medium" -> "中"
    "high" -> "高"
    "xhigh" -> "极高"
    "max" -> "最高"
    "ultra" -> "超高"
    null, "" -> "默认推理"
    else -> value
}

private fun remotePermissionLabel(value: String?): String = when (
    value?.trim()?.removePrefix(":")?.lowercase()
) {
    "read-only", "readonly", "read_only" -> "请求批准"
    "workspace", "workspace-write", "workspacewrite", "workspace_write" -> "帮我批准"
    "danger-full-access", "danger_full_access", "full-access", "full_access" -> "完全访问权限"
    null, "" -> "未读取"
    else -> value.removePrefix(":")
}

private fun queryAttachmentName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0)?.takeIf(String::isNotBlank) else null
    }
}.getOrNull()
    ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
    ?: "附件"

private fun queryAttachmentSize(context: Context, uri: Uri): Long? = runCatching {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
    }
}.getOrNull()

private suspend fun readAttachmentBytes(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0L
    context.contentResolver.openInputStream(uri)?.use { input ->
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MaxDetailAttachmentBytes) error("单个附件不能超过 20 MB")
            output.write(buffer, 0, count)
        }
    } ?: error("无法读取附件")
    output.toByteArray()
}

private fun ConnectorConversationDetailPayload.toDetailDemo(): DetailDemo = DetailDemo(
    messages = messages
        .takeLast(MaxVisibleDetailMessages)
        .mapIndexed { index, message ->
        DetailMessage(
            id = "remote-message-$index",
            isUser = message.role == ConnectorConversationRole.USER,
            text = message.text.take(MaxDetailMessageCharacters).let { visible ->
                if (message.text.length > MaxDetailMessageCharacters) {
                    "$visible\n\n内容较长，已截取最近详情。"
                } else {
                    visible
                }
            },
            attachments = message.attachments,
        )
    }.ifEmpty {
        listOf(DetailMessage("remote-empty", false, "电脑端暂时没有可显示的消息。"))
    },
    // A long Codex run can contain hundreds of low-level activity updates. Keep the detail panel
    // responsive while retaining the most recent process entries.
    activities = mergeDetailActivities(
        activities.map(ConnectorConversationActivityPayload::toDetailActivity),
    ).takeLast(MaxVisibleDetailActivities),
    running = executionStatus == ConnectorExecutionStatus.RUNNING ||
        executionStatus == ConnectorExecutionStatus.WAITING_FOR_APPROVAL,
)

private fun ConnectorConversationActivityPayload.toDetailActivity(): DetailActivity = DetailActivity(
    id = id,
    kind = when (kind) {
        ConnectorConversationActivityKind.THINKING -> DetailActivityKind.THINKING
        ConnectorConversationActivityKind.COMMAND -> DetailActivityKind.COMMAND
        ConnectorConversationActivityKind.WEB_SEARCH -> DetailActivityKind.WEB_SEARCH
        ConnectorConversationActivityKind.TOOL -> DetailActivityKind.TOOL
        ConnectorConversationActivityKind.FILE_CHANGE -> DetailActivityKind.FILE_CHANGE
        ConnectorConversationActivityKind.COMMENTARY -> DetailActivityKind.COMMENTARY
        ConnectorConversationActivityKind.PLAN -> DetailActivityKind.PLAN
        ConnectorConversationActivityKind.IMAGE -> DetailActivityKind.IMAGE
        ConnectorConversationActivityKind.OTHER -> DetailActivityKind.OTHER
    },
    title = title,
    text = text,
    status = when (status) {
        ConnectorConversationActivityStatus.RUNNING -> DetailActivityStatus.RUNNING
        ConnectorConversationActivityStatus.COMPLETED -> DetailActivityStatus.COMPLETED
        ConnectorConversationActivityStatus.INTERRUPTED -> DetailActivityStatus.INTERRUPTED
        ConnectorConversationActivityStatus.FAILED -> DetailActivityStatus.FAILED
    },
)

private fun mergeDetailActivities(activities: List<DetailActivity>): List<DetailActivity> {
    if (activities.size < 2) return activities
    val merged = LinkedHashMap<String, DetailActivity>()
    activities.forEach { activity ->
        val key = "${activity.kind.name}:${activity.title.trim().lowercase(Locale.ROOT)}"
        val previous = merged[key]
        merged[key] = if (previous == null) {
            activity
        } else {
            previous.copy(
                // Keep one expandable row per process type and show the latest
                // text/status instead of repeating every streaming update.
                text = activity.text.ifBlank { previous.text },
                status = activity.status,
            )
        }
    }
    return merged.values.toList()
}

private fun demoRemoteDetail(threadId: String): DetailDemo = when (threadId) {
    "demo-statuses" -> DetailDemo(
        messages = listOf(
            DetailMessage("status-user", true, "把 Codex 的执行状态整理给我看"),
            DetailMessage("status-assistant", false, "同类过程会合并为一行，只保留最新状态和内容。"),
        ),
        activities = listOf(
            DetailActivity(
                id = "thinking-1",
                kind = DetailActivityKind.THINKING,
                title = "思考",
                text = "第一次思考更新：分析任务目标。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "thinking-2",
                kind = DetailActivityKind.THINKING,
                title = "思考",
                text = "第二次思考更新：确认执行步骤。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "commentary",
                kind = DetailActivityKind.COMMENTARY,
                title = "执行说明",
                text = "开始检查电脑端工作区。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "command-1",
                kind = DetailActivityKind.COMMAND,
                title = "命令执行",
                text = "第一条命令输出。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "command-2",
                kind = DetailActivityKind.COMMAND,
                title = "命令执行",
                text = "第二条命令输出：同类命令更新覆盖上一条。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "web-search",
                kind = DetailActivityKind.WEB_SEARCH,
                title = "终端 Web 搜索",
                text = "搜索结果已返回。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "tool-call",
                kind = DetailActivityKind.TOOL,
                title = "工具调用",
                text = "调用远程工具并等待结果。",
                status = DetailActivityStatus.RUNNING,
            ),
            DetailActivity(
                id = "file-change",
                kind = DetailActivityKind.FILE_CHANGE,
                title = "修改文件",
                text = "已写入目标文件。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "plan",
                kind = DetailActivityKind.PLAN,
                title = "计划",
                text = "计划已更新。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "image",
                kind = DetailActivityKind.IMAGE,
                title = "图片预览",
                text = "已生成图片预览。",
                status = DetailActivityStatus.COMPLETED,
            ),
            DetailActivity(
                id = "other",
                kind = DetailActivityKind.OTHER,
                title = "其他操作",
                text = "其他操作已完成。",
                status = DetailActivityStatus.COMPLETED,
            ),
        ),
        running = true,
    )
    "demo-running" -> DetailDemo(
        messages = listOf(
            DetailMessage("user", true, "整理 MASON 远程控制项目"),
            DetailMessage("assistant", false, "我正在检查工作区结构，并准备整理远程控制相关模块。"),
        ),
        activities = listOf(
            DetailActivity(
                id = "thinking",
                kind = DetailActivityKind.THINKING,
                title = "思考",
                text = "正在扫描工作区，等待电脑端返回任务进度。",
                status = DetailActivityStatus.RUNNING,
            ),
        ),
        running = true,
    )
    "demo-failed" -> DetailDemo(
        messages = listOf(
            DetailMessage("user", true, "修复扫码连接超时"),
            DetailMessage("assistant", false, "任务失败：未收到电脑端响应。请确认电脑端项目已经启动。"),
        ),
        activities = listOf(
            DetailActivity(
                id = "connect",
                kind = DetailActivityKind.COMMAND,
                title = "连接任务",
                text = "未在等待时间内收到电脑端响应。",
                status = DetailActivityStatus.FAILED,
            ),
        ),
        running = false,
    )
    "demo-interrupted" -> DetailDemo(
        messages = listOf(
            DetailMessage("user", true, "测试远程任务恢复"),
            DetailMessage("assistant", false, "任务已中断，可以从最近消息继续。"),
        ),
        activities = listOf(
            DetailActivity(
                id = "resume",
                kind = DetailActivityKind.PLAN,
                title = "恢复任务",
                text = "任务在电脑端停止，当前页面保留最近消息。",
                status = DetailActivityStatus.INTERRUPTED,
            ),
        ),
        running = false,
    )
    else -> DetailDemo(
        messages = listOf(
            DetailMessage("user", true, demoRemoteTitle(threadId)),
            DetailMessage("assistant", false, "已完成：这是电脑端 Codex 返回的对话内容。你可以展开下面的过程行查看任务详情。"),
        ),
        activities = listOf(
            DetailActivity(
                id = "thinking",
                kind = DetailActivityKind.THINKING,
                title = "思考",
                text = "已完成本次任务。这里会显示电脑端返回的过程和任务结果。",
                status = DetailActivityStatus.COMPLETED,
            ),
        ),
        running = false,
    )
}

private fun demoRemoteTitle(threadId: String): String = when (threadId) {
    "demo-completed-unread" -> "实现扫码配对流程"
    "demo-idle" -> "检查 Windows Connector 状态"
    "demo-completed" -> "同步设置页面"
    "demo-archived" -> "整理项目文档"
    else -> "远程 Codex 对话"
}

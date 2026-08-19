package com.denggl2.masonremote.ui.remote

import android.content.Context
import androidx.lifecycle.ViewModel
import com.denggl2.masonremote.data.AndroidDeviceIdentityStore
import com.denggl2.masonremote.transport.ConnectorConversationChangePayload
import com.denggl2.masonremote.transport.ConnectorConversationCreateRequest
import com.denggl2.masonremote.transport.ConnectorConversationSummaryPayload
import com.denggl2.masonremote.transport.ConnectorComposerOptions
import com.denggl2.masonremote.transport.ConnectorExecutionStatus
import com.denggl2.masonremote.transport.PairedConnector
import com.denggl2.masonremote.transport.RemoteConnectorClient
import com.denggl2.masonremote.transport.TransportMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

private const val EVENT_OBSERVATION_RETRY_DELAY_MILLIS = 1_000L

data class RemoteConversationSummary(
    val threadId: String,
    val title: String,
    val preview: String = "",
    val updatedAt: Long = 0,
    val projectPath: String? = null,
    val executionStatus: RemoteExecutionStatus = RemoteExecutionStatus.IDLE,
    val isPinned: Boolean = false,
)

enum class RemoteExecutionStatus {
    IDLE,
    RUNNING,
    WAITING_FOR_PERMISSION,
    COMPLETED,
    INTERRUPTED,
    FAILED,
}

data class RemoteTaskNotificationEvent(
    val threadId: String,
    val conversationTitle: String,
    val kind: Kind,
    val detail: String? = null,
) {
    enum class Kind {
        RUNNING,
        WAITING_FOR_PERMISSION,
        COMPLETED,
        FAILED,
    }
}

data class RemoteReasoningEffortOption(
    val id: String,
    val description: String,
)

data class RemoteModelOption(
    val id: String,
    val model: String,
    val displayName: String,
    val description: String,
    val isDefault: Boolean = false,
    val defaultReasoningEffort: String,
    val supportedReasoningEfforts: List<RemoteReasoningEffortOption> = emptyList(),
)

data class RemotePermissionProfileOption(
    val id: String,
    val description: String? = null,
    val allowed: Boolean,
)

data class RemoteProjectOption(
    val path: String,
    val displayName: String,
)

data class RemoteComposerOptions(
    val projects: List<RemoteProjectOption> = emptyList(),
    val models: List<RemoteModelOption> = emptyList(),
    val permissionProfiles: List<RemotePermissionProfileOption> = emptyList(),
    val currentModelId: String? = null,
    val currentReasoningEffort: String? = null,
    val currentPermissionProfileId: String? = null,
)

data class RemoteConversationListUiState(
    val connector: TransportMode? = null,
    val conversations: List<RemoteConversationSummary> = emptyList(),
    val nextCursor: String? = null,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val mutatingThreadIds: Set<String> = emptySet(),
    val unreadCompletionThreadIds: Set<String> = emptySet(),
    val newConversationOptions: RemoteComposerOptions = RemoteComposerOptions(),
    val newConversationDraft: String = "",
    val selectedNewProjectPath: String? = null,
    val selectedNewModelId: String? = null,
    val selectedNewReasoningEffort: String? = null,
    val selectedNewPermissionProfileId: String? = null,
    val isNewConversationOptionsLoading: Boolean = false,
    val isCreatingConversation: Boolean = false,
    val createdConversationThreadId: String? = null,
    val newConversationError: String? = null,
    val isDisconnecting: Boolean = false,
    val disconnectError: String? = null,
    val errorMessage: String? = null,
)

internal class RemoteConversationListViewModel(
    private val pairedConnector: PairedConnector?,
    private val appContext: Context,
) : ViewModel() {
    private val isPaired = pairedConnector != null
    private val remoteScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectorClient = pairedConnector?.let {
        RemoteConnectorClient(it, AndroidDeviceIdentityStore(), appContext)
    }
    private var eventJob: Job? = null
    private var eventRevision = 0L
    private val project = RemoteProjectOption("C:/CodexWork/MASON", "MASON")
    private val _uiState = MutableStateFlow(
        RemoteConversationListUiState(
            connector = pairedConnector?.transportMode,
        ),
    )
    val uiState = _uiState.asStateFlow()
    private val _pairingDisconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pairingDisconnected = _pairingDisconnected.asSharedFlow()
    private val _notificationEvents = MutableSharedFlow<RemoteTaskNotificationEvent>(replay = 1, extraBufferCapacity = 8)
    val notificationEvents = _notificationEvents.asSharedFlow()

    fun onResume() {
        if (connectorClient != null) refresh()
    }

    fun startExecutionEventObservation() {
        if (connectorClient == null || eventJob != null) return
        eventJob = remoteScope.launch {
            while (isActive) {
                try {
                    val page = connectorClient.awaitConversationEvents(
                        deviceId = checkNotNull(pairedConnector).deviceId,
                        afterRevision = eventRevision,
                    )
                    applyEventPage(page.revision, page.changes)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = error.message)
                    delay(EVENT_OBSERVATION_RETRY_DELAY_MILLIS)
                }
            }
        }
    }

    fun stopExecutionEventObservation() {
        eventJob?.cancel()
        eventJob = null
    }

    fun updateRemoteExecutionStatus(
        threadId: String,
        status: RemoteExecutionStatus,
        preview: String? = null,
        detail: String? = null,
    ) {
        val conversation = _uiState.value.conversations.firstOrNull { it.threadId == threadId } ?: return
        val next = conversation.copy(
            executionStatus = status,
            preview = preview ?: conversation.preview,
            updatedAt = System.currentTimeMillis(),
        )
        updateConversation(threadId) { next }
        val kind = when (status) {
            RemoteExecutionStatus.RUNNING -> RemoteTaskNotificationEvent.Kind.RUNNING
            RemoteExecutionStatus.WAITING_FOR_PERMISSION -> RemoteTaskNotificationEvent.Kind.WAITING_FOR_PERMISSION
            RemoteExecutionStatus.COMPLETED -> RemoteTaskNotificationEvent.Kind.COMPLETED
            RemoteExecutionStatus.FAILED -> RemoteTaskNotificationEvent.Kind.FAILED
            else -> null
        }
        kind?.let {
            _notificationEvents.tryEmit(
                RemoteTaskNotificationEvent(
                    threadId = threadId,
                    conversationTitle = conversation.title,
                    kind = it,
                    detail = detail,
                ),
            )
        }
    }

    fun refresh() {
        val client = connectorClient ?: return
        remoteScope.launch {
            _uiState.value = _uiState.value.copy(
                isInitialLoading = _uiState.value.conversations.isEmpty(),
                isRefreshing = true,
                errorMessage = null,
            )
            runCatching {
                client.listConversations(checkNotNull(pairedConnector).deviceId)
            }.onSuccess { page ->
                applyConversationPage(page.conversations, page.revision)
                _uiState.value = _uiState.value.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = error.message ?: "无法读取电脑端会话",
                )
            }
        }
    }

    fun loadMore() = Unit
    fun retry() = refresh()
    fun markConversationSeen(threadId: String) {
        _uiState.value = _uiState.value.copy(
            unreadCompletionThreadIds = _uiState.value.unreadCompletionThreadIds - threadId,
        )
    }

    fun setConversationPinned(threadId: String, isPinned: Boolean) {
        val client = connectorClient
        if (client == null) {
            updateConversation(threadId) { it.copy(isPinned = isPinned) }
            return
        }
        remoteScope.launch {
            runCatching {
                if (isPinned) {
                    client.pinConversation(checkNotNull(pairedConnector).deviceId, threadId)
                } else {
                    client.unpinConversation(checkNotNull(pairedConnector).deviceId, threadId)
                }
            }.onSuccess { refresh() }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        }
    }

    fun archiveConversation(threadId: String) {
        val client = connectorClient
        if (client == null) {
            _uiState.value = _uiState.value.copy(
                conversations = _uiState.value.conversations.filterNot { it.threadId == threadId },
            )
            return
        }
        remoteScope.launch {
            runCatching {
                client.archiveConversation(checkNotNull(pairedConnector).deviceId, threadId)
            }.onSuccess { refresh() }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        }
    }

    fun clearDisconnectError() {
        _uiState.value = _uiState.value.copy(disconnectError = null)
    }

    fun disconnectPairing() {
        val client = connectorClient
        val connector = pairedConnector
        if (client == null || connector == null) {
            clearLocalPairing()
            return
        }
        _uiState.value = _uiState.value.copy(
            isDisconnecting = true,
            disconnectError = null,
        )
        remoteScope.launch {
            runCatching {
                client.revoke(connector.deviceId)
            }.onSuccess {
                clearLocalPairing()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDisconnecting = false,
                    disconnectError = error.message ?: "无法撤销电脑端设备授权",
                )
            }
        }
    }

    private fun clearLocalPairing() {
        _uiState.value = _uiState.value.copy(
            connector = null,
            conversations = emptyList(),
            isDisconnecting = false,
        )
        _pairingDisconnected.tryEmit(Unit)
    }

    override fun onCleared() {
        connectorClient?.close()
        remoteScope.cancel()
        super.onCleared()
    }

    fun loadNewConversationOptions() {
        val client = connectorClient ?: return
        _uiState.value = _uiState.value.copy(
            isNewConversationOptionsLoading = true,
            newConversationError = null,
        )
        remoteScope.launch {
            runCatching {
                client.newConversationOptions(
                    deviceId = checkNotNull(pairedConnector).deviceId,
                )
            }.onSuccess { options ->
                val selectedModelId = options.currentModelId
                    ?: options.models.firstOrNull()?.id
                val selectedModel = options.models.firstOrNull { it.id == selectedModelId }
                val selectedPermission = options.currentPermissionProfileId
                    ?: options.permissionProfiles.firstOrNull { it.allowed }?.id
                _uiState.value = _uiState.value.copy(
                    newConversationOptions = options.toUiOptions(),
                    selectedNewProjectPath = options.cwd
                        ?: options.projects.firstOrNull()?.path,
                    selectedNewModelId = selectedModelId,
                    selectedNewReasoningEffort = options.currentReasoningEffort
                        ?: selectedModel?.defaultReasoningEffort,
                    selectedNewPermissionProfileId = selectedPermission,
                    isNewConversationOptionsLoading = false,
                    newConversationError = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isNewConversationOptionsLoading = false,
                    newConversationError = error.message ?: "无法读取电脑端新建对话选项",
                )
            }
        }
    }

    fun updateNewConversationDraft(value: String) {
        _uiState.value = _uiState.value.copy(newConversationDraft = value)
    }

    fun selectNewProject(value: String) {
        _uiState.value = _uiState.value.copy(selectedNewProjectPath = value)
    }

    fun selectNewModel(value: String) {
        val nextModel = _uiState.value.newConversationOptions.models.firstOrNull { it.id == value }
        _uiState.value = _uiState.value.copy(
            selectedNewModelId = value,
            selectedNewReasoningEffort = nextModel?.defaultReasoningEffort,
        )
    }

    fun selectNewReasoningEffort(value: String) {
        _uiState.value = _uiState.value.copy(selectedNewReasoningEffort = value)
    }

    fun selectNewPermissionProfile(value: String) {
        _uiState.value = _uiState.value.copy(selectedNewPermissionProfileId = value)
    }

    fun createConversation() {
        val state = _uiState.value
        val client = connectorClient ?: return
        val projectPath = state.selectedNewProjectPath ?: return
        val modelId = state.selectedNewModelId ?: return
        val permissionProfileId = state.selectedNewPermissionProfileId ?: return
        if (state.newConversationDraft.isBlank() || state.isCreatingConversation) return
        _uiState.value = state.copy(
            isCreatingConversation = true,
            newConversationError = null,
        )
        remoteScope.launch {
            runCatching {
                client.createConversation(
                    deviceId = checkNotNull(pairedConnector).deviceId,
                    request = ConnectorConversationCreateRequest(
                        text = state.newConversationDraft,
                        projectPath = projectPath,
                        modelId = modelId,
                        reasoningEffort = state.selectedNewReasoningEffort,
                        permissionProfileId = permissionProfileId,
                    ),
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isCreatingConversation = false,
                    newConversationDraft = "",
                    createdConversationThreadId = result.threadId,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCreatingConversation = false,
                    newConversationError = error.message ?: "无法在电脑端新建对话",
                )
            }
        }
    }

    fun consumeCreatedConversation() {
        _uiState.value = _uiState.value.copy(createdConversationThreadId = null)
    }

    private fun updateConversation(threadId: String, transform: (RemoteConversationSummary) -> RemoteConversationSummary) {
        _uiState.value = _uiState.value.copy(
            conversations = _uiState.value.conversations.map {
                if (it.threadId == threadId) transform(it) else it
            },
        )
    }

    private fun applyConversationPage(
        conversations: List<ConnectorConversationSummaryPayload>,
        revision: Long,
    ) {
        val previous = _uiState.value.conversations.associateBy(RemoteConversationSummary::threadId)
        val next = conversations.map { it.toUiSummary() }
        val newlyCompleted = next.asSequence()
            .filter { it.executionStatus == RemoteExecutionStatus.COMPLETED }
            .filter { previous[it.threadId]?.executionStatus != RemoteExecutionStatus.COMPLETED }
            .map(RemoteConversationSummary::threadId)
            .toSet()
        eventRevision = maxOf(eventRevision, revision)
        _uiState.value = _uiState.value.copy(
            conversations = next,
            nextCursor = null,
            unreadCompletionThreadIds = _uiState.value.unreadCompletionThreadIds + newlyCompleted,
        )
    }

    private fun applyEventPage(
        revision: Long,
        changes: List<ConnectorConversationChangePayload>,
    ) {
        eventRevision = maxOf(eventRevision, revision)
        changes.forEach { change ->
            updateRemoteExecutionStatus(
                threadId = change.threadId,
                status = change.status.toUiStatus(),
            )
        }
    }

    private fun ConnectorConversationSummaryPayload.toUiSummary() = RemoteConversationSummary(
        threadId = threadId,
        title = title,
        preview = preview,
        updatedAt = updatedAt,
        projectPath = projectPath,
        executionStatus = executionStatus.toUiStatus(),
        isPinned = isPinned,
    )

    private fun ConnectorComposerOptions.toUiOptions() = RemoteComposerOptions(
        projects = projects.map { RemoteProjectOption(it.path, it.displayName) },
        models = models.map { model ->
            RemoteModelOption(
                id = model.id,
                model = model.model,
                displayName = model.displayName,
                description = model.description,
                isDefault = model.isDefault,
                defaultReasoningEffort = model.defaultReasoningEffort,
                supportedReasoningEfforts = model.supportedReasoningEfforts.map {
                    RemoteReasoningEffortOption(it.id, it.description)
                },
            )
        },
        permissionProfiles = permissionProfiles.map {
            RemotePermissionProfileOption(it.id, it.description, it.allowed)
        },
        currentModelId = currentModelId,
        currentReasoningEffort = currentReasoningEffort,
        currentPermissionProfileId = currentPermissionProfileId,
    )

    private fun ConnectorExecutionStatus.toUiStatus(): RemoteExecutionStatus = when (this) {
        ConnectorExecutionStatus.IDLE -> RemoteExecutionStatus.IDLE
        ConnectorExecutionStatus.RUNNING -> RemoteExecutionStatus.RUNNING
        ConnectorExecutionStatus.WAITING_FOR_APPROVAL -> RemoteExecutionStatus.WAITING_FOR_PERMISSION
        ConnectorExecutionStatus.COMPLETED -> RemoteExecutionStatus.COMPLETED
        ConnectorExecutionStatus.INTERRUPTED -> RemoteExecutionStatus.INTERRUPTED
        ConnectorExecutionStatus.FAILED -> RemoteExecutionStatus.FAILED
        ConnectorExecutionStatus.UNKNOWN -> RemoteExecutionStatus.IDLE
    }

    private fun demoConversations(): List<RemoteConversationSummary> {
        val now = System.currentTimeMillis()
        return listOf(
            RemoteConversationSummary(
                threadId = "demo-running",
                title = "整理 MASON 远程控制项目",
                preview = "正在扫描工作区，等待电脑端返回任务进度",
                updatedAt = now - 2 * 60_000L,
                projectPath = project.path,
                executionStatus = RemoteExecutionStatus.RUNNING,
            ),
            RemoteConversationSummary(
                threadId = "demo-completed-unread",
                title = "实现扫码配对流程",
                preview = "已完成：配对界面和连接状态展示",
                updatedAt = now - 18 * 60_000L,
                projectPath = project.path,
                executionStatus = RemoteExecutionStatus.COMPLETED,
            ),
            RemoteConversationSummary(
                threadId = "demo-idle",
                title = "检查 Windows Connector 状态",
                preview = "等待下一步指令",
                updatedAt = now - 52 * 60_000L,
                projectPath = project.path,
                executionStatus = RemoteExecutionStatus.IDLE,
            ),
            RemoteConversationSummary(
                threadId = "demo-completed",
                title = "同步设置页面",
                preview = "已完成：外观、通知和设备配对设置",
                updatedAt = now - 4 * 3_600_000L,
                projectPath = project.path,
                executionStatus = RemoteExecutionStatus.COMPLETED,
            ),
            RemoteConversationSummary(
                threadId = "demo-failed",
                title = "修复扫码连接超时",
                preview = "任务失败：未收到电脑端响应",
                updatedAt = now - 26 * 3_600_000L,
                projectPath = project.path,
                executionStatus = RemoteExecutionStatus.FAILED,
            ),
            RemoteConversationSummary(
                threadId = "demo-interrupted",
                title = "测试远程任务恢复",
                preview = "已中断：可以从最近消息继续",
                updatedAt = now - 2 * 86_400_000L,
                projectPath = project.path,
                executionStatus = RemoteExecutionStatus.INTERRUPTED,
            ),
            RemoteConversationSummary(
                threadId = "demo-archived",
                title = "整理项目文档",
                preview = "已完成项目结构检查",
                updatedAt = now - 6 * 86_400_000L,
                projectPath = project.path,
                executionStatus = RemoteExecutionStatus.COMPLETED,
            ),
        )
    }
}

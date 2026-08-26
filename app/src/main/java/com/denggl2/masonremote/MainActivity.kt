package com.denggl2.masonremote

import android.Manifest
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.util.Base64
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.denggl2.masonremote.data.PairingStore
import com.denggl2.masonremote.data.RemotePreferences
import com.denggl2.masonremote.diagnostics.DiagnosticLog
import com.denggl2.masonremote.notification.RemoteNotificationManager
import com.denggl2.masonremote.ui.MasonRemoteTheme
import com.denggl2.masonremote.ui.DisconnectedScreen
import com.denggl2.masonremote.ui.PairingLandingScreen
import com.denggl2.masonremote.ui.pairing.PairingSheet
import com.denggl2.masonremote.ui.remote.RemoteConversationListScreen
import com.denggl2.masonremote.ui.remote.RemoteConversationDetailScreen
import com.denggl2.masonremote.ui.remote.PairingDisconnectDialog
import com.denggl2.masonremote.ui.settings.RemoteFontSizePreference
import com.denggl2.masonremote.ui.settings.RemoteInterfaceStyle
import com.denggl2.masonremote.ui.settings.RemoteMessageSendMode
import com.denggl2.masonremote.ui.settings.RemoteThemeMode
import com.denggl2.masonremote.ui.settings.TaskNotificationMode
import com.denggl2.masonremote.ui.settings.SettingsScreen
import com.denggl2.masonremote.ui.remote.RemoteConversationListViewModel
import com.denggl2.masonremote.transport.TransportMode
import com.denggl2.masonremote.transport.parsePairingOffer

private sealed interface RemotePage {
    data object Pairing : RemotePage
    data object Disconnected : RemotePage
    data object Conversations : RemotePage
    data class Detail(val threadId: String) : RemotePage
    data object Settings : RemotePage
}

class MainActivity : ComponentActivity() {
    private val notificationThreadId = mutableStateOf<String?>(null)
    private val debugPairingPayload = mutableStateOf<String?>(null)
    private val debugThreadId = mutableStateOf<String?>(null)
    private val debugList = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagnosticLog.record("MAIN_ACTIVITY_CREATE")
        notificationThreadId.value = intent.remoteThreadId()
        debugPairingPayload.value = intent.debugPairingPayload()
        debugThreadId.value = intent.debugThreadId()
        debugList.value = intent.debugList()
        enableEdgeToEdge()
        // The chat composer consumes IME insets itself. Do not let the platform pan
        // the whole conversation window when the keyboard becomes visible.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        setContent {
            MasonRemoteApp(
                notificationThreadId = notificationThreadId.value,
                debugPairingPayload = debugPairingPayload.value,
                debugThreadId = debugThreadId.value,
                debugList = debugList.value,
                onNotificationThreadConsumed = { notificationThreadId.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationThreadId.value = intent.remoteThreadId()
        debugPairingPayload.value = intent.debugPairingPayload()
        debugThreadId.value = intent.debugThreadId()
        debugList.value = intent.debugList()
    }
}

@Composable
private fun MasonRemoteApp(
    notificationThreadId: String? = null,
    debugPairingPayload: String? = null,
    debugThreadId: String? = null,
    debugList: Boolean = false,
    onNotificationThreadConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val store = remember(appContext) { PairingStore(appContext) }
    val preferences = remember(appContext) { RemotePreferences(appContext) }
    val notificationManager = remember(appContext) { RemoteNotificationManager(appContext) }
    var isPaired by remember { mutableStateOf(store.isPaired) }
    var pairingRevision by remember { mutableIntStateOf(0) }
    var showPairingSheet by remember { mutableStateOf(debugPairingPayload != null) }
    var showPairingChooser by remember { mutableStateOf(!isPaired && debugPairingPayload == null) }
    var selectedPairingMode by remember { mutableStateOf<TransportMode?>(TransportMode.CLOUDFLARE_TUNNEL) }
    var activeTransportMode by remember { mutableStateOf(store.load()?.transportMode) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedRemoteConversationId by remember { mutableStateOf(debugThreadId) }
    val remoteConversationDrafts = remember { mutableStateMapOf<String, String>() }
    var showDisconnected by remember { mutableStateOf(false) }
    var disconnectedAfterRetry by remember { mutableStateOf(false) }
    var disconnectConfirmationStage by remember { mutableIntStateOf(0) }
    var themeMode by remember { mutableStateOf(preferences.themeMode) }
    var interfaceStyle by remember { mutableStateOf(preferences.interfaceStyle) }
    var fontSize by remember { mutableStateOf(preferences.fontSize) }
    var glassRefractionEnabled by remember { mutableStateOf(preferences.glassRefractionEnabled) }
    var glassTransparency by remember { mutableStateOf(preferences.glassTransparency) }
    var glassFrost by remember { mutableStateOf(preferences.glassFrost) }
    var notificationMode by remember { mutableStateOf(preferences.taskNotificationMode) }
    var messageSendMode by remember { mutableStateOf(preferences.messageSendMode) }
    val remoteViewModel = remember(isPaired, pairingRevision, appContext, debugList) {
        RemoteConversationListViewModel(
            pairedConnector = store.load(),
            appContext = appContext,
            debugDemoMode = BuildConfig.DEBUG && debugList,
        )
    }
    val remoteUiState by remoteViewModel.uiState.collectAsState()

    fun requestPairing(mode: TransportMode) {
        selectedPairingMode = mode
        showPairingChooser = false
        showPairingSheet = true
    }

    fun openPairingChooser() {
        selectedPairingMode = TransportMode.CLOUDFLARE_TUNNEL
        showPairingSheet = false
        showPairingChooser = true
        showSettings = false
        showDisconnected = false
        selectedRemoteConversationId = null
    }

    fun selectTransportMode(mode: TransportMode) {
        if (store.activate(mode)) {
            activeTransportMode = mode
            pairingRevision += 1
        } else {
            requestPairing(mode)
        }
    }

    fun requestDisconnect() {
        if (isPaired && remoteUiState.connector != null && !remoteUiState.isDisconnecting) {
            remoteViewModel.clearDisconnectError()
            disconnectConfirmationStage = 1
        }
    }
    var pendingNotificationPreview by remember { mutableStateOf<TaskNotificationMode?>(null) }
    val latestNotificationMode by rememberUpdatedState(notificationMode)
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingNotificationPreview
        pendingNotificationPreview = null
        if (granted && pending != null) {
            notificationManager.preview(pending)
        } else if (!granted && pending != null) {
            Toast.makeText(
                context,
                "通知权限未授予，通知模式已保存，但暂时无法发送通知",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun applyNotificationMode(mode: TaskNotificationMode) {
        notificationMode = mode
        preferences.taskNotificationMode = mode
        if (mode == TaskNotificationMode.DISABLED) {
            notificationManager.cancelAll()
            return
        }
        if (notificationManager.shouldRequestPostNotificationPermission()) {
            pendingNotificationPreview = mode
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val result = notificationManager.preview(mode)
            if (!result.sent && result.message != null) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(remoteViewModel) {
        remoteViewModel.notificationEvents.collect { event ->
            notificationManager.notifyTaskEvent(event, latestNotificationMode)
        }
    }

    LaunchedEffect(remoteViewModel) {
        remoteViewModel.pairingDisconnected.collect {
            disconnectConfirmationStage = 0
            store.clearActive()
            activeTransportMode = store.load()?.transportMode
            pairingRevision += 1
            isPaired = store.isPaired
            showDisconnected = true
            showPairingChooser = false
            selectedRemoteConversationId = null
            showSettings = false
        }
    }

    LaunchedEffect(notificationThreadId) {
        notificationThreadId?.let {
            selectedRemoteConversationId = it
            onNotificationThreadConsumed()
        }
    }

    LaunchedEffect(debugThreadId) {
        if (BuildConfig.DEBUG) {
            debugThreadId?.let { selectedRemoteConversationId = it }
        }
    }

    LaunchedEffect(debugPairingPayload) {
        debugPairingPayload
            ?.let(::parsePairingOffer)
            ?.bootstrap
            ?.transportMode
            ?.takeIf {
                it == TransportMode.LOCAL_TLS ||
                    it == TransportMode.CLOUDFLARE_TUNNEL ||
                    it == TransportMode.WEBRTC_DIRECT
            }
            ?.let {
                selectedPairingMode = it
                showPairingChooser = false
            }
        if (debugPairingPayload != null && !isPaired) {
            showPairingSheet = true
        }
    }

    DisposableEffect(lifecycleOwner, notificationManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> notificationManager.setAppInForeground(true)
                Lifecycle.Event.ON_PAUSE -> notificationManager.setAppInForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            notificationManager.setAppInForeground(true)
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        RemoteThemeMode.SYSTEM -> systemDark
        RemoteThemeMode.LIGHT -> false
        RemoteThemeMode.DARK -> true
    }

    MasonRemoteTheme(
        darkTheme = useDarkTheme,
        interfaceStyle = interfaceStyle,
        glassRefractionEnabled = glassRefractionEnabled,
        glassTransparency = glassTransparency,
        glassFrost = glassFrost,
    ) {
        val activity = context as? ComponentActivity
        val windowBackground = MaterialTheme.colorScheme.background
        SideEffect {
            // Keep the platform window fallback aligned with Compose during
            // page and theme transitions so a light frame cannot leak into
            // dark mode.
            activity?.window?.setBackgroundDrawable(ColorDrawable(windowBackground.toArgb()))
            activity?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
        }
        val page = when {
            showSettings -> RemotePage.Settings
            selectedRemoteConversationId != null -> RemotePage.Detail(checkNotNull(selectedRemoteConversationId))
            BuildConfig.DEBUG && debugList -> RemotePage.Conversations
            showPairingChooser -> RemotePage.Pairing
            showDisconnected -> RemotePage.Disconnected
            isPaired -> RemotePage.Conversations
            else -> RemotePage.Pairing
        }
        LaunchedEffect(page, remoteUiState.isRefreshing, remoteUiState.errorMessage) {
            val retryFinished =
                page == RemotePage.Conversations &&
                    !remoteUiState.isRefreshing &&
                    remoteUiState.errorMessage == null
            val retryFailed = page == RemotePage.Disconnected && remoteUiState.errorMessage != null
            if (disconnectedAfterRetry && (retryFinished || retryFailed)) {
                disconnectedAfterRetry = false
            }
        }

        AnimatedContent(
            targetState = page,
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                // Keep the outgoing and incoming pages on the active theme while
                // AnimatedContent is moving them across the window.
                .background(MaterialTheme.colorScheme.background),
            transitionSpec = {
                val movingForward = isForwardRemotePageTransition(
                    initial = initialState,
                    target = targetState,
                    disconnectedAfterRetry = disconnectedAfterRetry,
                )
                if (movingForward) {
                    (
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(360, easing = FastOutSlowInEasing),
                        ) + fadeIn(tween(220)) + scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(360, easing = FastOutSlowInEasing),
                        )
                    ) togetherWith (
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                        ) + fadeOut(tween(180)) + scaleOut(
                            targetScale = 0.985f,
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                        )
                    )
                } else {
                    (
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(340, easing = FastOutSlowInEasing),
                        ) + fadeIn(tween(200)) + scaleIn(
                            initialScale = 0.99f,
                            animationSpec = tween(340, easing = FastOutSlowInEasing),
                        )
                    ) togetherWith (
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        ) + fadeOut(tween(160)) + scaleOut(
                            targetScale = 0.99f,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        )
                    )
                }
            },
            label = "remote_page_transition",
        ) { targetPage ->
            when (targetPage) {
                RemotePage.Settings -> SettingsScreen(
                    isPaired = isPaired,
                    pairedDeviceName = store.deviceName,
                    themeMode = themeMode,
                    interfaceStyle = interfaceStyle,
                    fontSize = fontSize,
                    glassRefractionEnabled = glassRefractionEnabled,
                    glassTransparency = glassTransparency,
                    glassFrost = glassFrost,
                    notificationMode = notificationMode,
                    messageSendMode = messageSendMode,
                    onBack = { showSettings = false },
                    onDevicePairingClick = {
                        if (isPaired) requestDisconnect()
                        else openPairingChooser()
                    },
                    onThemeModeChange = {
                        themeMode = it
                        preferences.themeMode = it
                    },
                    onInterfaceStyleChange = {
                        interfaceStyle = it
                        preferences.interfaceStyle = it
                    },
                    onFontSizeChange = {
                        fontSize = it
                        preferences.fontSize = it
                    },
                    onGlassRefractionChange = {
                        glassRefractionEnabled = it
                        preferences.glassRefractionEnabled = it
                    },
                    onGlassTransparencyChange = {
                        glassTransparency = it
                        preferences.glassTransparency = it
                    },
                    onGlassFrostChange = {
                        glassFrost = it
                        preferences.glassFrost = it
                    },
                    onNotificationModeChange = ::applyNotificationMode,
                    onMessageSendModeChange = {
                        messageSendMode = it
                        preferences.messageSendMode = it
                    },
                )
                is RemotePage.Detail -> RemoteConversationDetailScreen(
                    threadId = targetPage.threadId,
                    pairedConnector = store.load(),
                    defaultMessageSendMode = messageSendMode,
                    draft = remoteConversationDrafts[targetPage.threadId].orEmpty(),
                    onDraftChange = { draft ->
                        if (draft.isEmpty()) {
                            remoteConversationDrafts.remove(targetPage.threadId)
                        } else {
                            remoteConversationDrafts[targetPage.threadId] = draft
                        }
                    },
                    onBack = { selectedRemoteConversationId = null },
                )
                RemotePage.Conversations -> RemoteConversationListScreen(
                    onSettings = { showSettings = true },
                    onRequestDisconnect = ::requestDisconnect,
                    onConversationSelected = { threadId ->
                        DiagnosticLog.record("CONVERSATION_SELECTED threadId=$threadId")
                        selectedRemoteConversationId = threadId
                    },
                    viewModel = remoteViewModel,
                )
                RemotePage.Pairing -> PairingLandingScreen(
                    selectedMode = selectedPairingMode,
                    onModeSelected = { selectedPairingMode = it },
                    onSettings = { showSettings = true },
                    onStart = {
                        showDisconnected = false
                        selectedPairingMode?.let(::requestPairing)
                    },
                )
                RemotePage.Disconnected -> DisconnectedScreen(
                    onRetry = {
                        disconnectedAfterRetry = true
                        showDisconnected = false
                        remoteViewModel.retry()
                    },
                    onRepair = {
                        disconnectedAfterRetry = false
                        openPairingChooser()
                    },
                    errorCode = remoteUiState.errorMessage
                        ?.let { message ->
                            "错误码${Integer.toHexString(message.hashCode()).uppercase().padStart(8, '0')}"
                        }
                        ?: "错误码XXXXXXXXXXXXXXXXXXX",
                )
            }
        }

        if (showPairingSheet) {
            PairingSheet(
                onDismiss = {
                    showPairingSheet = false
                    showPairingChooser = true
                },
                debugRawPayload = debugPairingPayload,
                requestedTransportMode = selectedPairingMode,
                onPaired = { connector ->
                    store.markPaired(connector)
                    store.activate(connector.transportMode)
                    pairingRevision += 1
                    activeTransportMode = connector.transportMode
                    isPaired = true
                    showDisconnected = false
                    showPairingChooser = false
                    disconnectedAfterRetry = false
                    showPairingSheet = false
                },
            )
        }

        PairingDisconnectDialog(
            stage = disconnectConfirmationStage,
            state = remoteUiState,
            onDismiss = {
                remoteViewModel.clearDisconnectError()
                disconnectConfirmationStage = 0
            },
            onFirstConfirm = { disconnectConfirmationStage = 2 },
            onFinalConfirm = remoteViewModel::disconnectPairing,
            onBackToFirst = {
                remoteViewModel.clearDisconnectError()
                disconnectConfirmationStage = 1
            },
        )

    }
}

private fun Intent?.remoteThreadId(): String? =
    takeIf { it?.action == RemoteNotificationManager.ACTION_OPEN_TASK }
        ?.getStringExtra(RemoteNotificationManager.EXTRA_THREAD_ID)

private fun Intent?.debugPairingPayload(): String? {
    if (!BuildConfig.DEBUG) return null
    return this?.getStringExtra(DEBUG_PAIRING_PAYLOAD_BASE64)
        ?.let { encoded ->
            runCatching { String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8) }.getOrNull()
        }
}

private fun Intent?.debugThreadId(): String? {
    if (!BuildConfig.DEBUG) return null
    return this?.getStringExtra(DEBUG_THREAD_ID)
        ?.takeIf { it.startsWith("demo-") }
}

private fun Intent?.debugList(): Boolean =
    BuildConfig.DEBUG && this?.getBooleanExtra(DEBUG_LIST, false) == true

private const val DEBUG_PAIRING_PAYLOAD_BASE64 = "mason_debug_pairing_base64"
private const val DEBUG_THREAD_ID = "mason_debug_thread_id"
private const val DEBUG_LIST = "mason_debug_list"

private fun isForwardRemotePageTransition(
    initial: RemotePage,
    target: RemotePage,
    disconnectedAfterRetry: Boolean = false,
): Boolean = when {
    initial is RemotePage.Disconnected && target is RemotePage.Conversations -> true
    target is RemotePage.Disconnected && disconnectedAfterRetry -> false
    target is RemotePage.Disconnected -> true
    initial is RemotePage.Conversations && target is RemotePage.Detail -> true
    initial is RemotePage.Conversations && target is RemotePage.Settings -> false
    initial is RemotePage.Pairing && target is RemotePage.Settings -> false
    initial is RemotePage.Pairing && target is RemotePage.Conversations -> true
    initial is RemotePage.Detail && target is RemotePage.Conversations -> false
    initial is RemotePage.Settings && target is RemotePage.Conversations -> true
    initial is RemotePage.Settings && target is RemotePage.Pairing -> true
    initial is RemotePage.Conversations && target is RemotePage.Pairing -> false
    else -> true
}

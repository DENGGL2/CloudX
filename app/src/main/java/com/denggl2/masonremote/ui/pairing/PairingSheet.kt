package com.denggl2.masonremote.ui.pairing

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denggl2.masonremote.data.PairingStore
import com.denggl2.masonremote.transport.PairingOffer
import com.denggl2.masonremote.transport.PairingResult
import com.denggl2.masonremote.transport.PairedConnector
import com.denggl2.masonremote.transport.RealPairingTransport
import com.denggl2.masonremote.transport.RemoteAgentKind
import com.denggl2.masonremote.transport.TransportMode
import com.denggl2.masonremote.transport.displayName
import com.denggl2.masonremote.transport.parsePairingOffer
import com.denggl2.masonremote.ui.FigmaSvgAsset
import com.denggl2.masonremote.ui.theme.MasonSheetShape
import com.denggl2.masonremote.ui.theme.masonOverlayWindowInsets
import com.denggl2.masonremote.ui.theme.masonSheetContainerColor
import com.denggl2.masonremote.ui.theme.masonSheetSurface
import kotlinx.coroutines.launch

private enum class PairingStep { SCAN, CONFIRM, CONNECTING, DONE, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PairingSheet(
    onDismiss: () -> Unit,
    debugRawPayload: String? = null,
    requestedTransportMode: TransportMode? = null,
    onPaired: (PairedConnector) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pairingStore = remember(context.applicationContext) { PairingStore(context.applicationContext) }
    val transport = remember(pairingStore, context.applicationContext) {
        RealPairingTransport(pairingStore, context.applicationContext)
    }
    var step by remember { mutableStateOf(PairingStep.SCAN) }
    var offer by remember { mutableStateOf<PairingOffer?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var connectedDeviceName by remember { mutableStateOf("") }
    var pairedConnector by remember { mutableStateOf<PairedConnector?>(null) }
    var permissionGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetHeight = (LocalConfiguration.current.screenHeightDp.dp - 400.dp)
        .coerceAtLeast(400.dp)

    fun isExpectedMode(candidate: PairingOffer): Boolean =
        requestedTransportMode == null || candidate.bootstrap.transportMode == requestedTransportMode

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionGranted = it }

    LaunchedEffect(debugRawPayload, requestedTransportMode) {
        val debugOffer = debugRawPayload?.let(::parsePairingOffer)
        if (debugRawPayload != null) {
            if (debugOffer != null && isExpectedMode(debugOffer)) {
                offer = debugOffer
                step = PairingStep.CONFIRM
            } else if (debugOffer != null) {
                errorMessage = "请扫描 ${requestedTransportMode?.displayName() ?: "对应方式"}二维码"
                step = PairingStep.ERROR
            } else {
                errorMessage = "测试二维码内容无效"
                step = PairingStep.ERROR
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = masonSheetContainerColor(),
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f),
        tonalElevation = 0.dp,
        shape = MasonSheetShape,
        dragHandle = null,
        contentWindowInsets = { masonOverlayWindowInsets() },
    ) {
        PairingSheetContent(
            sheetHeight = sheetHeight,
            step = step,
            offer = offer,
            connectedDeviceName = connectedDeviceName,
            errorMessage = errorMessage,
            permissionGranted = permissionGranted,
            onBackToScan = { step = PairingStep.SCAN },
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOfferFound = { scannedOffer ->
                if (isExpectedMode(scannedOffer)) {
                    offer = scannedOffer
                    step = PairingStep.CONFIRM
                } else {
                    errorMessage = "请扫描 ${requestedTransportMode?.displayName() ?: "对应方式"}二维码"
                    step = PairingStep.ERROR
                }
            },
            onConfirm = {
                val currentOffer = offer
                step = PairingStep.CONNECTING
                scope.launch {
                    when (val result = currentOffer?.let { transport.pair(it) }) {
                        is PairingResult.Connected -> {
                            connectedDeviceName = result.deviceName
                            pairedConnector = result.connector
                            step = PairingStep.DONE
                        }
                        is PairingResult.Failed -> {
                            errorMessage = result.message
                            step = PairingStep.ERROR
                        }
                        null -> {
                            errorMessage = "二维码信息无效"
                            step = PairingStep.ERROR
                        }
                    }
                }
            },
            onEnterConversation = { pairedConnector?.let(onPaired) },
            onRetry = { step = PairingStep.SCAN },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairingSheetContent(
    sheetHeight: androidx.compose.ui.unit.Dp,
    step: PairingStep,
    offer: PairingOffer?,
    connectedDeviceName: String,
    errorMessage: String,
    permissionGranted: Boolean,
    onBackToScan: () -> Unit,
    onRequestPermission: () -> Unit,
    onOfferFound: (PairingOffer) -> Unit,
    onConfirm: () -> Unit,
    onEnterConversation: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .masonSheetSurface(
                RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                includeNavigationBarPadding = false,
                drawEdge = false,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 20.dp)
                .size(width = 30.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PairingSheetColors.Handle),
        )
        Text(
            text = when (step) {
                PairingStep.SCAN -> "扫码配对"
                PairingStep.CONFIRM -> "确认配对"
                PairingStep.CONNECTING -> "正在配对"
                PairingStep.DONE -> "已完成配对"
                PairingStep.ERROR -> "配对失败"
            },
            color = PairingSheetColors.Text,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 52.dp),
        )

        when (step) {
            PairingStep.SCAN -> {
                Text(
                    text = "扫描电脑端项目生成的二维码",
                    color = PairingSheetColors.Secondary,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 83.dp),
                )
                if (permissionGranted) {
                    QrScanner(
                        frameSize = 238.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 116.dp),
                        onOfferFound = onOfferFound,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 116.dp)
                            .size(238.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PairingSheetColors.Scanner),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = PairingSheetColors.ScannerContent,
                            )
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PairingSheetColors.Button,
                                    contentColor = PairingSheetColors.ButtonContent,
                                ),
                            ) { Text("允许相机权限") }
                        }
                    }
                }
            }
            PairingStep.CONFIRM -> {
                Text(
                    text = offer?.deviceName ?: "电脑",
                    color = PairingSheetColors.Text,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 116.dp),
                )
                Text(
                    text = buildString {
                        append("服务器\n")
                        append(offer?.serverUrl ?: "")
                        append("\n设备指纹\n")
                        append(offer?.fingerprint ?: "")
                    },
                    color = PairingSheetColors.Secondary,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 150.dp)
                        .width(210.dp),
                )
                PairingButtons(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 310.dp),
                    onCancel = onBackToScan,
                    onConfirm = onConfirm,
                    confirmLabel = "确认并配对",
                )
            }
            PairingStep.CONNECTING -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).offset(y = 35.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(color = PairingSheetColors.Text)
                    Text("正在建立安全连接…", color = PairingSheetColors.Secondary, fontSize = 14.sp)
                }
            }
            PairingStep.DONE -> {
                FigmaSvgAsset(
                    assetPath = "figma/pairing_done.svg",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 158.dp)
                        .size(30.dp),
                )
                Text(
                    text = "已连接到电脑端",
                    color = PairingSheetColors.Text,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 197.dp),
                )
                Text(
                    text = connectedDeviceName,
                    color = PairingSheetColors.Secondary,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 226.dp),
                )
                Button(
                    onClick = onEnterConversation,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 310.dp)
                        .width(232.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PairingSheetColors.Button,
                        contentColor = PairingSheetColors.ButtonContent,
                    ),
                ) { Text("进入对话", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
            PairingStep.ERROR -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 35.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = PairingSheetColors.Text)
                    Text(errorMessage, color = PairingSheetColors.Secondary, textAlign = TextAlign.Center, fontSize = 14.sp)
                    OutlinedButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PairingSheetColors.Text,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) { Text("重新扫码") }
                }
            }
        }
    }
}

@Composable
private fun PairingButtons(
    modifier: Modifier,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier.size(width = 110.dp, height = 44.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) { Text("返回", fontSize = 14.sp) }
        Button(
            onClick = onConfirm,
            modifier = Modifier.size(width = 110.dp, height = 44.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PairingSheetColors.Button,
                contentColor = PairingSheetColors.ButtonContent,
            ),
        ) { Text(confirmLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
    }
}

private object PairingSheetColors {
    val Button: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    val ButtonContent: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary
    val Handle: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    val Scanner: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val ScannerContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Secondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Text: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
}

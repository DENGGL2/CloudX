package com.denggl2.masonremote.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denggl2.masonremote.transport.displayName
import com.denggl2.masonremote.ui.theme.MasonAlertDialog

@Composable
internal fun PairingDisconnectDialog(
    stage: Int,
    state: RemoteConversationListUiState,
    onDismiss: () -> Unit,
    onFirstConfirm: () -> Unit,
    onFinalConfirm: () -> Unit,
    onBackToFirst: () -> Unit,
) {
    if (stage !in 1..2) return

    val isFinalConfirmation = stage == 2
    val connectionName = state.connector?.displayName() ?: "当前连接"
    val body = if (isFinalConfirmation) {
        "请确认断开连接"
    } else {
        "当前连接方式为${connectionName}，是否断开？"
    }

    MasonAlertDialog(
        onDismissRequest = { if (!state.isDisconnecting) onDismiss() },
        modifier = Modifier.width(328.dp),
        shape = RoundedCornerShape(30.dp),
        scrimAlpha = 0.32f,
        customContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(198.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(19.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "信息确认",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(46.dp))
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .height(50.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(23.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = if (isFinalConfirmation) onBackToFirst else onDismiss,
                        enabled = !state.isDisconnecting,
                        modifier = Modifier.size(width = 130.dp, height = 44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text("取消", fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(20.dp))
                    Button(
                        onClick = if (isFinalConfirmation) onFinalConfirm else onFirstConfirm,
                        enabled = !state.isDisconnecting,
                        modifier = Modifier.size(width = 130.dp, height = 44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (state.isDisconnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 1.5.dp,
                            )
                        } else {
                            Text("确认", fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

package com.denggl2.masonremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DisconnectedScreen(
    onRetry: () -> Unit,
    onRepair: () -> Unit,
    errorCode: String = "错误码XXXXXXXXXXXXXXXXXXX",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(195.dp)
                .height(96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FigmaSvgAsset(
                assetPath = "figma/disconnected.svg",
                modifier = Modifier.size(50.dp),
            )
            Text(
                text = "已断开连接，请重试\n$errorCode",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 46.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedButton(
                onClick = onRepair,
                modifier = Modifier
                    .width(110.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text("重新配对", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .width(110.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("重试", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

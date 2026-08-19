package com.denggl2.masonremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denggl2.masonremote.transport.TransportMode

@Composable
fun PairingLandingScreen(
    selectedMode: TransportMode,
    onModeSelected: (TransportMode) -> Unit,
    onStart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(800.dp),
        ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-50).dp)
                        .width(140.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FigmaSvgAsset(
                        assetPath = "figma/frame16.svg",
                        modifier = Modifier.size(width = 140.dp, height = 32.dp),
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = "电脑端启动后扫码配对",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        maxLines = 1,
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 46.dp)
                        .width(232.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(23.dp),
                    ) {
                        Text(
                            text = "请选择连接方式",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .offset(y = 23.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                        )
                    }
                    PairingModeOption(
                        title = "Cloudflare 隧道",
                        description = "中转，电脑端重启需要重新配对",
                        rowHeight = 33.dp,
                        titleWidth = 108.dp,
                        descriptionWidth = 168.dp,
                        titleSize = 14.sp,
                        titleLineHeight = 17.sp,
                        descriptionLineHeight = 14.sp,
                        radioTop = 9.dp,
                        selected = selectedMode == TransportMode.CLOUDFLARE_TUNNEL,
                        onClick = { onModeSelected(TransportMode.CLOUDFLARE_TUNNEL) },
                    )
                    PairingModeOption(
                        title = "WebRTC 直连",
                        description = "手机直连，信令服务器配对",
                        rowHeight = 34.dp,
                        titleWidth = 96.dp,
                        descriptionWidth = 144.dp,
                        titleSize = 15.sp,
                        titleLineHeight = 18.sp,
                        descriptionLineHeight = 14.sp,
                        radioTop = 11.dp,
                        selected = selectedMode == TransportMode.WEBRTC_DIRECT,
                        onClick = { onModeSelected(TransportMode.WEBRTC_DIRECT) },
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = ButtonDefaults.ContentPadding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("开始", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
        }
    }
}

@Composable
private fun PairingModeOption(
    title: String,
    description: String,
    rowHeight: androidx.compose.ui.unit.Dp,
    titleWidth: androidx.compose.ui.unit.Dp,
    descriptionWidth: androidx.compose.ui.unit.Dp,
    titleSize: androidx.compose.ui.unit.TextUnit,
    titleLineHeight: androidx.compose.ui.unit.TextUnit,
    descriptionLineHeight: androidx.compose.ui.unit.TextUnit,
    radioTop: androidx.compose.ui.unit.Dp,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .width(titleWidth),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = titleSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = descriptionLineHeight,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.width(descriptionWidth),
                overflow = TextOverflow.Visible,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = radioTop)
                .size(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            FigmaSvgAsset(
                assetPath = if (selected) "figma/radio_selected.svg" else "figma/radio_unselected.svg",
                modifier = Modifier.size(18.dp),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
            )
        }
    }
}

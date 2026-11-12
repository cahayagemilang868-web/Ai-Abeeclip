package com.abeeclip.app

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewSection(
    uri: Uri, ratio: String, offsetX: Float, sampleText: String,
    textColor: Color, fontItem: FontItem, hasBg: Boolean, bgColor: Color,
    onRatioChange: (String) -> Unit, onOffsetChange: (Float) -> Unit
) {
    val ctx = LocalContext.current
    val animX by animateFloatAsState(offsetX, label = "x")
    val bw = if (ratio == "9:16") 190.dp else if (ratio == "1:1") 230.dp else 280.dp
    val bh = if (ratio == "9:16") 310.dp else if (ratio == "1:1") 230.dp else 160.dp

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181822)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Rasio Bingkai:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("9:16", "1:1", "16:9").forEach { r ->
                    OptionChip(label = r, isSelected = ratio == r) { onRatioChange(r) }
                }
            }
        }
        Box(
            modifier = Modifier.width(bw).height(bh).clip(RoundedCornerShape(14.dp)).background(Color.Black)
                .border(1.5.dp, Color(0xFF7C4DFF), RoundedCornerShape(14.dp)).clipToBounds()
                .pointerInput(ratio) { detectHorizontalDragGestures { _, d -> if (ratio != "16:9") onOffsetChange((offsetX + d).coerceIn(-220f, 220f)) } },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { c ->
                    PlayerView(c).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize().graphicsLayer { translationX = if (ratio == "16:9") 0f else animX }
            )
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (hasBg) bgColor else Color(0x66000000)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(sampleText, color = textColor, fontSize = 13.sp, fontFamily = fontItem.family, fontWeight = fontItem.weight, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("Kiri", offsetX > 40f, modifier = Modifier.weight(1f)) { onOffsetChange(160f) }
            OptionChip("Tengah", offsetX in -40f..40f, modifier = Modifier.weight(1f)) { onOffsetChange(0f) }
            OptionChip("Kanan", offsetX < -40f, modifier = Modifier.weight(1f)) { onOffsetChange(-160f) }
        }
    }
}

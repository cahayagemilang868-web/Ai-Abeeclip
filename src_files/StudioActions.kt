package com.abeeclip.app

import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.launch

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
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    translationX = if (ratio == "16:9") 0f else animX
                }
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

@Composable
fun AiActionSection(
    uri: Uri?, ratio: String, offsetX: Float, upper: Boolean, lengthPrompt: String,
    textColor: Color, fontItem: FontItem
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("Pilih video dari perangkat lalu jalankan analisis AI atau Render MP4.") }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val u = uri ?: return@Button Unit.also { result = "Pilih video dulu!" }
                loading = true
                scope.launch { result = GeminiHelper.analyzeVideo(ctx, u, "Temukan 3 Golden Moment viral beserta menit mulai-selesai dan alasannya dalam bahasa Indonesia."); loading = false }
            },
            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2438))
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Momen AI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {
                val u = uri ?: return@Button Unit.also { result = "Pilih video dulu!" }
                loading = true
                scope.launch {
                    val cs = if (upper) "Gunakan HURUF KAPITAL SEMUA." else ""
                    result = GeminiHelper.analyzeVideo(ctx, u, "Buatkan Auto Subtitle SRT standar. $lengthPrompt $cs Tanpa basa-basi.")
                    loading = false
                }
            },
            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2438))
        ) {
            Icon(Icons.Default.ClosedCaption, null, tint = Color(0xFF64FFDA), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Subtitle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {
                val u = uri ?: return@Button Unit.also { result = "Pilih video dulu!" }
                loading = true
                scope.launch { result = GeminiHelper.analyzeVideo(ctx, u, "Buatkan 3 judul hook viral dan 10 hashtag relevan dalam bahasa Indonesia."); loading = false }
            },
            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2438))
        ) {
            Icon(Icons.Default.Tag, null, tint = Color(0xFFFF80AB), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Judul & Tag", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    Button(
        onClick = {
            val u = uri ?: return@Button Unit.also { Toast.makeText(ctx, "Pilih video dulu!", Toast.LENGTH_SHORT).show() }
            exporting = true; result = "Merender video MP4 rasio $ratio..."
            VideoExportHelper.exportMp4(ctx, u, offsetX, ratio) { msg -> exporting = false; result = msg; Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
        },
        enabled = !exporting, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
    ) {
        Icon(Icons.Default.MovieCreation, null, tint = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text(if (exporting) "Merender MP4..." else "Render & Simpan MP4 ke Galeri", color = Color.Black, fontWeight = FontWeight.Bold)
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181822))
            .border(1.dp, Color(0xFF2D2D3F), RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Hasil AI & Ekspor Teks", color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = Color(0xFFB388FF), strokeWidth = 2.dp)
                OptionChip("Salin", false) { clip.setText(AnnotatedString(result)); Toast.makeText(ctx, "Disalin!", Toast.LENGTH_SHORT).show() }

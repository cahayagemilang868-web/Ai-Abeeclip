package com.abeeclip.app

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    var result by remember { mutableStateOf("Pilih video lalu tekan Momen AI, Subtitle, atau Potong & Render MP4.") }
    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("00:30") }

    fun parseSec(t: String): Long {
        val p = t.trim().split(":")
        return if (p.size == 2) (p[0].toLongOrNull() ?: 0L) * 60 + (p[1].toLongOrNull() ?: 0L)
        else p[0].toLongOrNull() ?: 0L
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val u = uri ?: return@Button Unit.also { result = "Pilih video dulu!" }
                loading = true
                scope.launch {
                    result = GeminiHelper.analyzeVideo(ctx, u, "Temukan 3 Golden Moment viral beserta format waktu MM:SS mulai dan selesai serta alasannya dalam bahasa Indonesia.")
                    loading = false
                }
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
                scope.launch {
                    result = GeminiHelper.analyzeVideo(ctx, u, "Buatkan 3 judul hook viral dan 10 hashtag relevan dalam bahasa Indonesia.")
                    loading = false
                }
            },
            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2438))
        ) {
            Icon(Icons.Default.Tag, null, tint = Color(0xFFFF80AB), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Judul & Tag", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    // Panel Auto-Trim Klip (Potong Menit Mulai - Selesai)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF181822)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Potong Klip (Auto-Trim Menit)", color = Color(0xFFB388FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startTime, onValueChange = { startTime = it },
                label = { Text("Mulai (MM:SS)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = endTime, onValueChange = { endTime = it },
                label = { Text("Selesai (MM:SS)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f), singleLine = true
            )
        }
        Button(
            onClick = {
                val u = uri ?: return@Button Unit.also { Toast.makeText(ctx, "Pilih video dulu!", Toast.LENGTH_SHORT).show() }
                exporting = true; result = "Memotong & merender klip ($startTime - $endTime)..."
                VideoExportHelper.exportMp4(ctx, u, offsetX, ratio, parseSec(startTime), parseSec(endTime)) { msg ->
                    exporting = false; result = msg; Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                }
            },
            enabled = !exporting, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Icon(Icons.Default.ContentCut, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (exporting) "Memotong Klip..." else "Potong Klip & Simpan MP4", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    Button(
        onClick = {
            val u = uri ?: return@Button Unit.also { Toast.makeText(ctx, "Pilih video dulu!", Toast.LENGTH_SHORT).show() }
            exporting = true; result = "Merender video penuh rasio $ratio..."
            VideoExportHelper.exportMp4(ctx, u, offsetX, ratio, 0L, 0L) { msg ->
                exporting = false; result = msg; Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            }
        },
        enabled = !exporting, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
    ) {
        Icon(Icons.Default.MovieCreation, null, tint = Color.Black)
        Spacer(Modifier.width(8.dp))
        Text(if (exporting) "Merender MP4..." else "Render Video Penuh ke Galeri", color = Color.Black, fontWeight = FontWeight.Bold)
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
                OptionChip(".SRT", true, Color(0xFF282044), Color(0xFFB388FF)) { Toast.makeText(ctx, VideoExportHelper.saveSrt(ctx, result), Toast.LENGTH_LONG).show() }
            }
        }
        Spacer(Modifier.height(8.dp))
        val displayMsg = if (loading) "Mengompres & memproses video super cepat..." else result
        Text(displayMsg, color = textColor, fontSize = 13.sp, fontFamily = fontItem.family, fontWeight = fontItem.weight)
    }
}

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
    var result by remember { mutableStateOf("Pilih video dari perangkat lalu jalankan analisis AI atau Render MP4.") }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val u = uri ?: return@Button Unit.also { result = "Pilih video dulu!" }
                loading = true
                scope.launch {
                    result = GeminiHelper.analyzeVideo(ctx, u, "Temukan 3 Golden Moment viral beserta menit mulai-selesai dan alasannya dalam bahasa Indonesia.")
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

    Button(
        onClick = {
            val u = uri ?: return@Button Unit.also { Toast.makeText(ctx, "Pilih video dulu!", Toast.LENGTH_SHORT).show() }
            exporting = true
            result = "Merender video MP4 rasio $ratio..."
            VideoExportHelper.exportMp4(ctx, u, offsetX, ratio) { msg ->
                exporting = false
                result = msg
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            }
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
                OptionChip("Salin", false) {
                    clip.setText(AnnotatedString(result))
                    Toast.makeText(ctx, "Disalin!", Toast.LENGTH_SHORT).show()
                }
                OptionChip(".SRT", true, Color(0xFF282044), Color(0xFFB388FF)) {
                    Toast.makeText(ctx, VideoExportHelper.saveSrt(ctx, result), Toast.LENGTH_LONG).show()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val displayMsg = if (loading) "Memproses video dengan Gemini AI..." else result
        Text(displayMsg, color = textColor, fontSize = 13.sp, fontFamily = fontItem.family, fontWeight = fontItem.weight)
    }
}

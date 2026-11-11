package com.abeeclip.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0E0E14)) { AppScreen() }
            }
        }
    }
}

@Composable
fun AppScreen() {
    var uri by remember { mutableStateOf<Uri?>(null) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var ratio by remember { mutableStateOf("9:16") }

    val fonts = listOf(
        FontItem("Modern", FontFamily.SansSerif, FontWeight.SemiBold),
        FontItem("Klasik", FontFamily.Serif, FontWeight.Normal),
        FontItem("Ketik", FontFamily.Monospace, FontWeight.Medium),
        FontItem("Viral", FontFamily.SansSerif, FontWeight.ExtraBold)
    )
    var fIdx by remember { mutableIntStateOf(3) }
    val colors = listOf(Color(0xFFFFD54F), Color.White, Color(0xFF64FFDA), Color(0xFFFF5252))
    var cIdx by remember { mutableIntStateOf(0) }
    var bgColor by remember { mutableStateOf(Color.Transparent) }
    var hasBg by remember { mutableStateOf(false) }
    var upper by remember { mutableStateOf(true) }
    var tplName by remember { mutableStateOf("Hormozi") }

    val lengths = listOf(
        LengthItem("Pendek", "Pecah teks 1-3 kata per baris waktu gaya TikTok/Reels.", "GAS VIRAL SEKARANG!"),
        LengthItem("Sedang", "Buat teks 4-7 kata per baris waktu.", "Halo semuanya, selamat datang!"),
        LengthItem("Panjang", "Gabungkan ucapan jadi kalimat utuh.", "Halo semuanya, selamat datang kembali hari ini.")
    )
    var lIdx by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) { uri = u; offsetX = 0f }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("AI Abeeclip", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("AI Video Clipper & Subtitle Studio", fontSize = 12.sp, color = Color(0xFF9E9EB8))
            }
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF282044)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("PRO STUDIO", color = Color(0xFFB388FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = { picker.launch("video/*") },
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Icon(Icons.Default.VideoLibrary, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(if (uri == null) "+ Pilih Video dari Perangkat" else "Ganti Video Terpilih", fontWeight = FontWeight.Bold, color = Color.White)
        }

        uri?.let { activeUri ->
            val sample = if (upper) lengths[lIdx].sample.uppercase() else lengths[lIdx].sample
            VideoPreviewSection(
                uri = activeUri, ratio = ratio, offsetX = offsetX, sampleText = sample,
                textColor = colors[cIdx], fontItem = fonts[fIdx], hasBg = hasBg, bgColor = bgColor,
                onRatioChange = { r -> ratio = r; offsetX = 0f }, onOffsetChange = { offsetX = it }
            )
        }

        StyleControlPanel(
            tplName = tplName, fonts = fonts, fIdx = fIdx, colors = colors, cIdx = cIdx, hasBg = hasBg, lengths = lengths, lIdx = lIdx,
            onSelectTemplate = { t ->
                tplName = t
                when (t) {
                    "Hormozi" -> { fIdx = 3; cIdx = 0; hasBg = false; upper = true }
                    "Karaoke" -> { fIdx = 3; cIdx = 1; bgColor = Color(0xDD7C4DFF); hasBg = true; upper = true }
                    "Neon" -> { fIdx = 2; cIdx = 2; bgColor = Color(0xCC000000); hasBg = true; upper = false }
                    "Film" -> { fIdx = 1; cIdx = 1; bgColor = Color(0x99000000); hasBg = true; upper = false }
                }
            },
            onSelectFont = { fIdx = it; tplName = "Custom" },
            onSelectColor = { cIdx = it; tplName = "Custom" },
            onToggleBg = { hasBg = !hasBg; if (hasBg && bgColor == Color.Transparent) bgColor = Color(0xCC000000); tplName = "Custom" },
            onSelectLength = { lIdx = it }
        )

        AiActionSection(
            uri = uri, ratio = ratio, offsetX = offsetX, upper = upper,
            lengthPrompt = lengths[lIdx].prompt, textColor = colors[cIdx], fontItem = fonts[fIdx]
        )
    }
}

package com.abeeclip.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0E0E14)) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
fun AppScreen() {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("Pilih video (maks. 15 MB) lalu atur template, posisi, dan analisis AI.") }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animX by animateFloatAsState(offsetX, label = "x")
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
        if (u != null) {
            uri = u
            offsetX = 0f
            result = "Video siap diproses!"
        }
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(if (uri == null) "+ Pilih Video dari Perangkat" else "Ganti Video Terpilih", fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (uri != null) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181822)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Rasio Bingkai:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("9:16", "1:1", "16:9").forEach { r ->
                            OptionChip(label = r, isSelected = ratio == r) { ratio = r; offsetX = 0f }
                        }
                    }
                }

                val bw = if (ratio == "9:16") 190.dp else if (ratio == "1:1") 230.dp else 280.dp
                val bh = if (ratio == "9:16") 310.dp else if (ratio == "1:1") 230.dp else 160.dp

                Box(
                    modifier = Modifier.width(bw).height(bh).clip(RoundedCornerShape(14.dp)).background(Color.Black)
                        .border(1.5.dp, Color(0xFF7C4DFF), RoundedCornerShape(14.dp)).clipToBounds()
                        .pointerInput(ratio) {
                            detectHorizontalDragGestures { _, d ->
                                if (ratio != "16:9") offsetX = (offsetX + d).coerceIn(-220f, 220f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { c ->
                            VideoView(c).apply {
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        update = { view -> view.setVideoURI(uri) },
                        modifier = Modifier.fillMaxHeight().wrapContentWidth(unbounded = true).graphicsLayer {
                            translationX = if (ratio == "16:9") 0f else animX
                            val sc = if (ratio == "9:16") 1.45f else if (ratio == "1:1") 1.2f else 1f
                            scaleX = sc
                            scaleY = sc
                        }
                    )

                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (hasBg) bgColor else Color(0x66000000)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val rawSample = lengths[lIdx].sample
                        val txt = if (upper) rawSample.uppercase() else rawSample
                        val currentFont = fonts[fIdx]
                        Text(
                            text = txt,
                            color = colors[cIdx],
                            fontSize = 13.sp,
                            fontFamily = currentFont.family,
                            fontWeight = currentFont.weight,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip("Kiri", offsetX > 40f, modifier = Modifier.weight(1f)) { offsetX = 160f }
                    OptionChip("Tengah", offsetX in -40f..40f, modifier = Modifier.weight(1f)) { offsetX = 0f }
                    OptionChip("Kanan", offsetX < -40f, modifier = Modifier.weight(1f)) { offsetX = -160f }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181822)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Template Subtitle Viral", color = Color(0xFFFF80AB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Hormozi", "Karaoke", "Neon", "Film").forEach { t ->
                    OptionChip(t, tplName == t, Color(0xFFFF80AB), Color.Black, modifier = Modifier.weight(1f)) {
                        tplName = t
                        when (t) {
                            "Hormozi" -> { fIdx = 3; cIdx = 0; hasBg = false; upper = true }

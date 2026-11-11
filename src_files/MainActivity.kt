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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
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
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize(), color = Color(0xFF0E0E14)) { AppScreen() } } }
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

    val fonts = listOf(Triple("Modern", FontFamily.SansSerif, FontWeight.SemiBold), Triple("Klasik", FontFamily.Serif, FontWeight.Normal), Triple("Ketik", FontFamily.Monospace, FontWeight.Medium), Triple("Viral", FontFamily.SansSerif, FontWeight.ExtraBold))
    var fIdx by remember { mutableIntStateOf(3) }
    val colors = listOf(Color(0xFFFFD54F), Color.White, Color(0xFF64FFDA), Color(0xFFFF5252))
    var cIdx by remember { mutableIntStateOf(0) }
    var bgColor by remember { mutableStateOf(Color.Transparent) }
    var hasBg by remember { mutableStateOf(false) }
    var upper by remember { mutableStateOf(true) }
    var tplName by remember { mutableStateOf("Hormozi") }

    val lengths = listOf(
        Triple("Pendek", "Pecah teks 1-3 kata per baris waktu gaya TikTok/Reels.", "GAS VIRAL SEKARANG!"),
        Triple("Sedang", "Buat teks 4-7 kata per baris waktu.", "Halo semuanya, selamat datang!"),
        Triple("Panjang", "Gabungkan ucapan jadi kalimat utuh.", "Halo semuanya, selamat datang kembali hari ini.")
    )
    var lIdx by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) { uri = u; offsetX = 0f; result = "Video siap diproses!" } }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("AI Abeeclip", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("AI Video Clipper & Subtitle Studio", fontSize = 12.sp, color = Color(0xFF9E9EB8)) }
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF282044)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("PRO STUDIO", color = Color(0xFFB388FF), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        Button(onClick = { picker.launch("video/*") }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))) {
            Icon(Icons.Default.VideoLibrary, null, tint = Color.White); Spacer(Modifier.width(8.dp))
            Text(if (uri == null) "+ Pilih Video dari Perangkat" else "Ganti Video Terpilih", fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (uri != null) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181822)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Rasio Bingkai:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("9:16", "1:1", "16:9").forEach { r ->
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (ratio == r) Color(0xFF7C4DFF) else Color(0xFF252534)).clickable { ratio = r; offsetX = 0f }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(r, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                val bw = if (ratio == "9:16") 190.dp else if (ratio == "1:1") 230.dp else 280.dp
                val bh = if (ratio == "9:16") 310.dp else if (ratio == "1:1") 230.dp else 160.dp
                Box(Modifier.width(bw).height(bh).clip(RoundedCornerShape(14.dp)).background(Color.Black).border(1.5.dp, Color(0xFF7C4DFF), RoundedCornerShape(14.dp)).clipToBounds()
                    .pointerInput(ratio) { detectHorizontalDragGestures { _, d -> if (ratio != "16:9") offsetX = (offsetX + d).coerceIn(-220f, 220f) } }, contentAlignment = Alignment.Center) {
                    AndroidView(factory = { c -> VideoView(c).apply { setOnPreparedListener { it.isLooping = true; start() } } }, update = { it.setVideoURI(uri) },
                        modifier = Modifier.fillMaxHeight().wrapContentWidth(unbounded = true).graphicsLayer {
                            translationX = if (ratio == "16:9") 0f else animX
                            val sc = if (ratio == "9:16") 1.45f else if (ratio == "1:1") 1.2f else 1f
                            scaleX = sc; scaleY = sc
                        })
                    Box(Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(RoundedCornerShape(8.dp)).background(if (hasBg) bgColor else Color.Transparent).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        val txt = if (upper) lengths[lIdx].third.uppercase() else lengths[lIdx].third
                        Text(txt, color = colors[cIdx], fontSize = 13.sp, fontFamily = fonts[fIdx].second, fontWeight = fonts[fIdx].third, textAlign = TextAlign.Center,
                            style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)))
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("Kiri", 160f, Icons.Default.AlignHorizontalLeft), Triple("Tengah", 0f, Icons.Default.AlignHorizontalCenter), Triple("Kanan", -160f, Icons.Default.AlignHorizontalRight)).forEach { (lbl, off, ic) ->
                        val sel = (off == 0f && offsetX in -40f..40f) || (off > 0f && offsetX > 40f) || (off < 0f && offsetX < -40f)
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) Color(0xFF7C4DFF) else Color(0xFF252534)).clickable { offsetX = off }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(ic, null, tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text(lbl, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF181822)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Template Subtitle Viral", color = Color(0xFFFF80AB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Hormozi", "Karaoke", "Neon", "Film").forEach { t ->
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (tplName == t) Color(0xFFFF80AB) else Color(0xFF252534)).clickable {
                        tplName = t
                        when (t) {
                            "Hormozi" -> { fIdx = 3; cIdx = 0; hasBg = false; upper = true }
                            "Karaoke" -> { fIdx = 3; cIdx = 1; bgColor = Color(0xDD7C4DFF); hasBg = true; upper = true }
                            "Neon" -> { fIdx = 2; cIdx = 2; bgColor = Color(0xCC000000); hasBg = true; upper = false }
                            "Film" -> { fIdx = 1; cIdx = 1; bgColor = Color(0x99000000); hasBg = true; upper = false }

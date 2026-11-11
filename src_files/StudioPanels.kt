package com.abeeclip.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StyleControlPanel(
    tplName: String,
    fonts: List<FontItem>,
    fIdx: Int,
    colors: List<Color>,
    cIdx: Int,
    hasBg: Boolean,
    lengths: List<LengthItem>,
    lIdx: Int,
    onSelectTemplate: (String) -> Unit,
    onSelectFont: (Int) -> Unit,
    onSelectColor: (Int) -> Unit,
    onToggleBg: () -> Unit,
    onSelectLength: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF181822))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Template Subtitle Viral", color = Color(0xFFFF80AB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Hormozi", "Karaoke", "Neon", "Film").forEach { t ->
                OptionChip(t, tplName == t, Color(0xFFFF80AB), Color.Black, modifier = Modifier.weight(1f)) {
                    onSelectTemplate(t)
                }
            }
        }

        Text("Gaya Font & Warna", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            fonts.forEachIndexed { idx, f ->
                OptionChip(f.name, fIdx == idx, fontFamily = f.family, fontWeight = f.weight, modifier = Modifier.weight(1f)) {
                    onSelectFont(idx)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEachIndexed { idx, c ->
                    ColorCircle(c, cIdx == idx) { onSelectColor(idx) }
                }
            }
            OptionChip("Kotak Latar (BG)", hasBg, Color(0xFF00BFA5), Color.Black) { onToggleBg() }
        }

        Text("Panjang Teks Subtitle", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            lengths.forEachIndexed { idx, l ->
                OptionChip(l.name, lIdx == idx, Color(0xFF00BFA5), Color.Black, modifier = Modifier.weight(1f)) {
                    onSelectLength(idx)
                }
            }
        }
    }
}

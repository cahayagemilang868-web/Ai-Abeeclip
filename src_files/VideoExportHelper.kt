package com.abeeclip.app

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import java.io.File
import java.nio.ByteBuffer

object VideoExportHelper {

    fun saveSrt(context: Context, text: String): String = try {
        val name = "Abeeclip_${System.currentTimeMillis()}.srt"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val v = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/x-subrip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name).writeText(text)
        }
        "Tersimpan di Downloads: $name"
    } catch (e: Exception) { "Gagal SRT: ${e.localizedMessage}" }

    // Ekstrak jalur audio mentah secara instan (< 1 detik) tanpa membebani RAM/GPU
    fun extractAudioFast(context: Context, uri: Uri): File? {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }
            if (audioTrackIndex < 0) return null

            extractor.selectTrack(audioTrackIndex)
            val outFile = File(context.cacheDir, "fast_audio_${System.currentTimeMillis()}.m4a")
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val writeTrackIndex = muxer.addTrack(extractor.getTrackFormat(audioTrackIndex))
            muxer.start()

            val buffer = ByteBuffer.allocate(512 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }
            outFile
        } catch (e: Exception) {
            null
        } finally {
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    @OptIn(UnstableApi::class)
    fun exportMp4(
        context: Context, uri: Uri, offsetX: Float, ratio: String,
        startSec: Long = 0L, endSec: Long = 0L, onDone: (String) -> Unit
    ) {
        try {
            val out = File(context.cacheDir, "render_${System.currentTimeMillis()}.mp4")
            val s = (offsetX / 220f).coerceIn(-0.45f, 0.45f)
            val crop = when (ratio) {
                "9:16" -> Crop((-0.56f + s).coerceIn(-1f, 0.4f), (0.56f + s).coerceIn(-0.4f, 1f), -1f, 1f)
                "1:1" -> Crop((-0.75f + s).coerceIn(-1f, 0.2f), (0.75f + s).coerceIn(-0.2f, 1f), -1f, 1f)
                else -> Crop(-1f, 1f, -1f, 1f)
            }
            val h = if (ratio == "9:16") 1280 else if (ratio == "1:1") 1080 else 720

            val mediaBuilder = MediaItem.Builder().setUri(uri)
            if (endSec > startSec) {
                mediaBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startSec * 1000L)
                        .setEndPositionMs(endSec * 1000L)
                        .build()
                )
            }

            val item = EditedMediaItem.Builder(mediaBuilder.build())
                .setEffects(Effects(emptyList(), listOf(crop, Presentation.createForHeight(h))))
                .build()

            Transformer.Builder(context).addListener(object : Transformer.Listener {
                override fun onCompleted(c: Composition, r: ExportResult) {
                    val fn = "AI_Abeeclip_${System.currentTimeMillis()}.mp4"
                    val v = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, fn)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AI Abeeclip")
                        }
                    }
                    context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v)?.let { u ->
                        context.contentResolver.openOutputStream(u)?.use { o -> out.inputStream().use { i -> i.copyTo(o) } }
                    }
                    out.delete()
                    onDone("Sukses! Tersimpan di Galeri: Movies/AI Abeeclip/$fn")
                }
                override fun onError(c: Composition, r: ExportResult, e: ExportException) {
                    out.delete()
                    onDone("Gagal Render: ${e.localizedMessage}")
                }
            }).build().start(item, out.absolutePath)
        } catch (e: Exception) { onDone("Error Ekspor: ${e.localizedMessage}") }
    }
}

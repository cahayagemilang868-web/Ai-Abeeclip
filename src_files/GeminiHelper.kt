package com.abeeclip.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val API_KEY = "PLACEHOLDER_API_KEY"
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeVideo(context: Context, videoUri: Uri, taskPrompt: String): String = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(videoUri) ?: "video/mp4"
            val fileSize = context.contentResolver.openAssetFileDescriptor(videoUri, "r")?.use { it.length } ?: -1L
            if (fileSize <= 0L) return@withContext "Gagal membaca ukuran file video."

            // 1. Minta sesi unggah Resumable ke Gemini Files API
            val initJson = JSONObject().put("file", JSONObject().put("display_name", "abeeclip_video"))
            val initReq = Request.Builder()
                .url("https://googleapis.com")
                .addHeader("X-Goog-Upload-Protocol", "resumable")
                .addHeader("X-Goog-Upload-Command", "start")
                .addHeader("X-Goog-Upload-Header-Content-Length", fileSize.toString())
                .addHeader("X-Goog-Upload-Header-Content-Type", mimeType)
                .post(initJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val uploadUrl = client.newCall(initReq).execute().use { res ->
                if (!res.isSuccessful) return@withContext "Gagal memulai unggahan (${res.code})."
                res.header("X-Goog-Upload-URL") ?: return@withContext "URL unggah tidak ditemukan."
            }

            // 2. Unggah aliran video langsung tanpa membebani RAM HP
            val streamBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaType()
                override fun contentLength() = fileSize
                override fun writeTo(sink: BufferedSink) {
                    context.contentResolver.openInputStream(videoUri)?.use { input ->
                        sink.writeAll(input.source())
                    }
                }
            }

            val uploadReq = Request.Builder()
                .url(uploadUrl)
                .addHeader("X-Goog-Upload-Offset", "0")
                .addHeader("X-Goog-Upload-Command", "upload, finalize")
                .post(streamBody)
                .build()

            val fileInfo = client.newCall(uploadReq).execute().use { res ->
                val body = res.body?.string() ?: return@withContext "Respons unggah kosong."
                if (!res.isSuccessful) return@withContext "Gagal mengunggah video (${res.code})."
                JSONObject(body).optJSONObject("file") ?: return@withContext "Data file tidak valid."
            }

            val fileUri = fileInfo.optString("uri")
            val fileName = fileInfo.optString("name")

            // 3. Tunggu server Google selesai memproses video hingga status ACTIVE
            var state = fileInfo.optString("state")
            var attempts = 0
            while (state == "PROCESSING" && attempts < 30) {
                delay(3000)
                val checkReq = Request.Builder()
                    .url("https://googleapis.com")
                    .get()
                    .build()
                state = client.newCall(checkReq).execute().use { res ->
                    val b = res.body?.string() ?: ""
                    JSONObject(b).optString("state")
                }
                attempts++
            }

            if (state != "ACTIVE") return@withContext "Video gagal diproses server AI (Status: $state)."

            // 4. Jalankan analisis video menggunakan Gemini 2.5 Flash
            val genJson = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                .put(JSONObject().put("file_data", JSONObject().put("mime_type", mimeType).put("file_uri", fileUri)))
                .put(JSONObject().put("text", taskPrompt)))))

            val genReq = Request.Builder()
                .url("https://googleapis.com")
                .post(genJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(genReq).execute().use { res ->
                val body = res.body?.string() ?: return@withContext "Respons AI kosong."
                if (!res.isSuccessful) {
                    val err = JSONObject(body).optJSONObject("error")?.optString("message") ?: "Error ${res.code}"
                    return@withContext "Gagal AI (${res.code}): $err"
                }
                JSONObject(body)
                    .optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: "Tidak ada hasil teks dari AI."
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}

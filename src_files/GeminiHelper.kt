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
    private const val SUB = "generativelanguage"
    private const val DOM = "googleapis.com"
    private val HOST = "https://$SUB.$DOM"

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

            val uploadEndpoint = "$HOST/upload/v1beta/files?key=$API_KEY"
            val initJson = JSONObject().put("file", JSONObject().put("displayName", "abeeclip_video"))
            val initReq = Request.Builder()
                .url(uploadEndpoint)
                .addHeader("X-Goog-Upload-Protocol", "resumable")
                .addHeader("X-Goog-Upload-Command", "start")
                .addHeader("X-Goog-Upload-Header-Content-Length", fileSize.toString())
                .addHeader("X-Goog-Upload-Header-Content-Type", mimeType)
                .post(initJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val uploadUrl = client.newCall(initReq).execute().use { res ->
                if (!res.isSuccessful) {
                    val errBody = res.body?.string() ?: ""
                    return@withContext "Gagal memulai unggahan (${res.code}): $errBody"
                }
                res.header("X-Goog-Upload-URL") ?: res.header("x-goog-upload-url")
                    ?: return@withContext "URL unggah tidak ditemukan di header."
            }

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
                if (!res.isSuccessful) return@withContext "Gagal mengunggah video (${res.code}): $body"
                JSONObject(body).optJSONObject("file") ?: return@withContext "Data file tidak valid."
            }

            val fileUri = fileInfo.optString("uri")
            val fileName = fileInfo.optString("name")

            var state = fileInfo.optString("state")
            var attempts = 0
            while (state == "PROCESSING" && attempts < 30) {
                delay(3000)
                val checkEndpoint = "$HOST/v1beta/$fileName?key=$API_KEY"
                val checkReq = Request.Builder()
                    .url(checkEndpoint)
                    .get()
                    .build()
                state = client.newCall(checkReq).execute().use { res ->
                    val b = res.body?.string() ?: ""
                    JSONObject(b).optString("state")
                }
                attempts++
            }

            if (state != "ACTIVE") return@withContext "Video gagal diproses server AI (Status: $state)."

            val genEndpoint = "$HOST/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY"
            val genJson = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                .put(JSONObject().put("fileData", JSONObject().put("mimeType", mimeType).put("fileUri", fileUri)))
                .put(JSONObject().put("text", taskPrompt)))))

            val genReq = Request.Builder()
                .url(genEndpoint)
                .post(genJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
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

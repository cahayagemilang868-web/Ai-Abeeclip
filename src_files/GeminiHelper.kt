package com.abeeclip.app

import android.content.Context
import android.net.Uri
import android.util.Base64
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
import java.io.File
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val API_KEY = "PLACEHOLDER_API_KEY"
    private const val SUB = "generativelanguage"
    private const val DOM = "googleapis.com"
    private val HOST = "https://$SUB.$DOM"

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeVideo(context: Context, videoUri: Uri, taskPrompt: String): String = withContext(Dispatchers.IO) {
        var audioFile: File? = null
        try {
            audioFile = VideoExportHelper.extractAudioFast(context, videoUri)
            val useAudio = audioFile != null && audioFile.exists() && audioFile.length() > 0L
            val mimeType = if (useAudio) "audio/mp4" else (context.contentResolver.getType(videoUri) ?: "video/mp4")
            val fileSize = if (useAudio) audioFile!!.length()
            else context.contentResolver.openAssetFileDescriptor(videoUri, "r")?.use { it.length } ?: -1L

            if (fileSize <= 0L) return@withContext "Gagal membaca ukuran file media."

            if (fileSize < 10 * 1024 * 1024) {
                val bytes = if (useAudio) audioFile!!.readBytes()
                else context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
                    ?: return@withContext "Gagal membaca data media."

                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val partMedia = JSONObject().put("inlineData", JSONObject().put("mimeType", mimeType).put("data", b64))
                return@withContext callGeminiOfficial(partMedia, taskPrompt)
            }

            val uploadEndpoint = "$HOST/upload/v1beta/files?key=$API_KEY"
            val initJson = JSONObject().put("file", JSONObject().put("displayName", "abeeclip_long"))
            val initReq = Request.Builder()
                .url(uploadEndpoint)
                .addHeader("X-Goog-Upload-Protocol", "resumable")
                .addHeader("X-Goog-Upload-Command", "start")
                .addHeader("X-Goog-Upload-Header-Content-Length", fileSize.toString())
                .addHeader("X-Goog-Upload-Header-Content-Type", mimeType)
                .post(initJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val uploadUrl = client.newCall(initReq).execute().use { res ->
                if (!res.isSuccessful) return@withContext "Gagal mulai unggah (${res.code})."
                res.header("X-Goog-Upload-URL") ?: res.header("x-goog-upload-url")
                    ?: return@withContext "URL unggah tidak ditemukan."
            }

            val streamBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaType()
                override fun contentLength() = fileSize
                override fun writeTo(sink: BufferedSink) {
                    val stream = if (useAudio) audioFile!!.inputStream()
                    else context.contentResolver.openInputStream(videoUri)
                    stream?.use { input -> sink.writeAll(input.source()) }
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
                if (!res.isSuccessful) return@withContext "Gagal unggah (${res.code}): $body"
                JSONObject(body).optJSONObject("file") ?: return@withContext "Data file tidak valid."
            }

            val fileUri = fileInfo.optString("uri")
            val fileName = fileInfo.optString("name")
            var state = fileInfo.optString("state")
            var attempts = 0
            while (state == "PROCESSING" && attempts < 20) {
                delay(1500)
                val checkReq = Request.Builder().url("$HOST/v1beta/$fileName?key=$API_KEY").get().build()
                state = client.newCall(checkReq).execute().use { res ->
                    JSONObject(res.body?.string() ?: "").optString("state")
                }
                attempts++
            }

            if (state != "ACTIVE") return@withContext "Media gagal diproses server ($state)."
            val partFile = JSONObject().put("fileData", JSONObject().put("mimeType", mimeType).put("fileUri", fileUri))
            return@withContext callGeminiOfficial(partFile, taskPrompt)
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        } finally {
            audioFile?.delete()
        }
    }

    private suspend fun callGeminiOfficial(mediaPart: JSONObject, taskPrompt: String): String {
        val models = listOf("gemini-3.1-flash-lite-preview", "gemini-3-flash-preview", "gemini-3.1-pro-preview")
        var lastError = ""

        for (model in models) {
            try {
                val genEndpoint = "$HOST/v1beta/models/$model:generateContent?key=$API_KEY"
                val genJson = JSONObject()
                    .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                        .put(mediaPart)
                        .put(JSONObject().put("text", taskPrompt)))))
                    .put("generationConfig", JSONObject()
                        .put("temperature", 0.4)
                        .put("maxOutputTokens", 4096))

                val genReq = Request.Builder()
                    .url(genEndpoint)
                    .post(genJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                client.newCall(genReq).execute().use { res ->
                    val body = res.body?.string() ?: ""
                    if (res.isSuccessful) {
                        val resultText = JSONObject(body)
                            .optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")
                        if (!resultText.isNullOrBlank()) return resultText
                    } else {
                        val err = JSONObject(body).optJSONObject("error")?.optString("message") ?: "Error ${res.code}"
                        lastError = "($model: ${res.code}) $err"
                    }
                }
            } catch (e: Exception) {
                lastError = "($model) ${e.localizedMessage}"
            }
            delay(1500)
        }
        return "Gagal AI: $lastError"
    }
}

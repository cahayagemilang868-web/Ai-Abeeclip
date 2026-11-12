package com.abeeclip.app

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val API_KEY = "PLACEHOLDER_API_KEY"
    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeVideo(context: Context, videoUri: Uri, taskPrompt: String): String = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
                ?: return@withContext "Gagal baca video."
            if (bytes.size > 15 * 1024 * 1024) {
                return@withContext "Video > 15 MB. Gunakan video pendek."
            }

            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val mime = context.contentResolver.getType(videoUri) ?: "video/mp4"

            val json = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                .put(JSONObject().put("inline_data", JSONObject().put("mime_type", mime).put("data", b64)))
                .put(JSONObject().put("text", taskPrompt)))))

            val req = Request.Builder()
                .url("https://googleapis.com")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { res ->
                val body = res.body?.string() ?: return@withContext "Respons kosong"
                if (!res.isSuccessful) {
                    val errMsg = JSONObject(body).optJSONObject("error")?.optString("message") ?: "Error ${res.code}"
                    return@withContext "Gagal AI (${res.code}): $errMsg"
                }
                JSONObject(body)
                    .optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: "Gagal baca hasil AI"
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}

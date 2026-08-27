package com.moodfit.app.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject


data class MakeoverRequest(
    val identityImage: Uri,
    val lookImage: Uri,
    val transferOutfit: Boolean = true,
    val transferHair: Boolean = true,
    val transferMakeup: Boolean = true,
    val transferPose: Boolean = true,
    val transferBackground: Boolean = false,
    val prompt: String = ""
)

sealed interface MakeoverResult {
    data class Success(val imageUrl: String) : MakeoverResult
    data class Error(val message: String) : MakeoverResult
}

interface MakeoverProvider {
    suspend fun generate(request: MakeoverRequest): MakeoverResult
}

class FalFlux2Provider(
    private val context: Context,
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient()
) : MakeoverProvider {

    override suspend fun generate(request: MakeoverRequest): MakeoverResult = withContext(Dispatchers.IO) {
        try {
            val lookData = dataUri(request.lookImage)
            val identityData = dataUri(request.identityImage)
            val prompt = buildPrompt(request)

            val payload = JSONObject().apply {
                put("prompt", prompt)
                put("image_urls", JSONArray().put(lookData).put(identityData))
            }

            val submit = Request.Builder()
                .url("https://queue.fal.run/fal-ai/flux-2/edit")
                .header("Authorization", "Key $apiKey")
                .header("Content-Type", "application/json")
                .header("X-Fal-Store-IO", "0")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(submit).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@withContext MakeoverResult.Error("fal.ai Fehler ${response.code}: $body")
                val queued = JSONObject(body)
                val statusUrl = queued.optString("status_url")
                val responseUrl = queued.optString("response_url")
                if (statusUrl.isBlank() || responseUrl.isBlank()) return@withContext MakeoverResult.Error("Ungültige Antwort des KI-Dienstes.")

                repeat(120) {
                    delay(1500)
                    val statusRequest = Request.Builder().url(statusUrl).header("Authorization", "Key $apiKey").get().build()
                    client.newCall(statusRequest).execute().use { statusResponse ->
                        val statusBody = statusResponse.body?.string().orEmpty()
                        if (!statusResponse.isSuccessful) return@withContext MakeoverResult.Error("Statusfehler ${statusResponse.code}")
                        val status = JSONObject(statusBody)
                        when (status.optString("status")) {
                            "COMPLETED" -> {
                                val resultRequest = Request.Builder().url(responseUrl).header("Authorization", "Key $apiKey").get().build()
                                client.newCall(resultRequest).execute().use { resultResponse ->
                                    val resultBody = resultResponse.body?.string().orEmpty()
                                    if (!resultResponse.isSuccessful) return@withContext MakeoverResult.Error("Ergebnisfehler ${resultResponse.code}")
                                    val result = JSONObject(resultBody)
                                    val images = result.optJSONArray("images")
                                    val url = images?.optJSONObject(0)?.optString("url").orEmpty()
                                    if (url.isBlank()) return@withContext MakeoverResult.Error("Kein Ergebnisbild erhalten.")
                                    return@withContext MakeoverResult.Success(url)
                                }
                            }
                        }
                    }
                }
                MakeoverResult.Error("Zeitüberschreitung bei der Bildgenerierung.")
            }
        } catch (e: Exception) {
            MakeoverResult.Error(e.message ?: "Unbekannter Fehler")
        }
    }

    private fun buildPrompt(request: MakeoverRequest): String {
        val keep = mutableListOf<String>()
        if (request.transferOutfit) keep += "outfit and clothing"
        if (request.transferHair) keep += "hairstyle"
        if (request.transferMakeup) keep += "make-up"
        if (request.transferPose) keep += "pose and body position"
        if (request.transferBackground) keep += "background and environment"

        val requested = if (keep.isEmpty()) "overall styling" else keep.joinToString(", ")
        return """
            Image 1 is the LOOK reference. Image 2 is the identity reference.
            Create a realistic photograph based primarily on image 1. Preserve the $requested from image 1.
            Replace the face identity with the adult person from image 2. Preserve that person's recognizable facial identity, age, facial proportions and natural features.
            Keep anatomy realistic and do not blend the two identities. No text, no watermark.
            ${request.prompt}
        """.trimIndent()
    }

    private fun dataUri(uri: Uri): String {
        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Bild konnte nicht gelesen werden")
        return "data:$type;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

package com.moodfit.app.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


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

private fun makeoverPrompt(request: MakeoverRequest): String {
    val keep = mutableListOf<String>()
    if (request.transferOutfit) keep += "outfit and clothing"
    if (request.transferHair) keep += "hairstyle"
    if (request.transferMakeup) keep += "make-up"
    if (request.transferPose) keep += "pose and body position"
    if (request.transferBackground) keep += "background and environment"

    val requested = if (keep.isEmpty()) "overall styling" else keep.joinToString(", ")
    return """
        Input image 1 is the LOOK reference and should define the composition. Input image 2 is the YOU identity reference.
        Create one realistic full photograph based primarily on image 1. Preserve the $requested from image 1.
        The person in the result must have the recognizable facial identity of the adult person in image 2: preserve age, facial proportions, face shape, eyes, nose, mouth and other natural identity-defining features.
        Do not blend the identities of the two people. Keep realistic anatomy, hands, skin texture, lighting and photographic detail. No text and no watermark.
        ${request.prompt}
    """.trimIndent()
}

class OpenAiGptImageProvider(
    private val context: Context,
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient()
) : MakeoverProvider {

    override suspend fun generate(request: MakeoverRequest): MakeoverResult = withContext(Dispatchers.IO) {
        try {
            val look = imageBytes(request.lookImage)
            val identity = imageBytes(request.identityImage)

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "gpt-image-2")
                .addFormDataPart("prompt", makeoverPrompt(request))
                .addFormDataPart("size", "1024x1536")
                .addFormDataPart("quality", "medium")
                .addFormDataPart("output_format", "jpeg")
                .addFormDataPart(
                    "image[]",
                    "look.jpg",
                    look.first.toRequestBody(look.second.toMediaType())
                )
                .addFormDataPart(
                    "image[]",
                    "identity.jpg",
                    identity.first.toRequestBody(identity.second.toMediaType())
                )
                .build()

            val httpRequest = Request.Builder()
                .url("https://api.openai.com/v1/images/edits")
                .header("Authorization", "Bearer $apiKey")
                .post(multipart)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext MakeoverResult.Error(openAiError(response.code, body))
                }

                val json = JSONObject(body)
                val base64 = json.optJSONArray("data")?.optJSONObject(0)?.optString("b64_json").orEmpty()
                if (base64.isBlank()) {
                    return@withContext MakeoverResult.Error("OpenAI hat kein Ergebnisbild zurückgegeben.")
                }

                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val file = File(context.cacheDir, "moodfit-openai-${System.currentTimeMillis()}.jpg")
                file.writeBytes(bytes)
                MakeoverResult.Success(Uri.fromFile(file).toString())
            }
        } catch (e: Exception) {
            MakeoverResult.Error(e.message ?: "Unbekannter OpenAI-Fehler")
        }
    }

    private fun imageBytes(uri: Uri): Pair<ByteArray, String> {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Bild konnte nicht gelesen werden")
        return bytes to mime
    }

    private fun openAiError(code: Int, body: String): String {
        return try {
            val message = JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
            if (message.isNotBlank()) "OpenAI Fehler $code: $message" else "OpenAI Fehler $code"
        } catch (_: Exception) {
            "OpenAI Fehler $code: $body"
        }
    }
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

            val payload = JSONObject().apply {
                put("prompt", makeoverPrompt(request))
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
                        if (status.optString("status") == "COMPLETED") {
                            val resultRequest = Request.Builder().url(responseUrl).header("Authorization", "Key $apiKey").get().build()
                            client.newCall(resultRequest).execute().use { resultResponse ->
                                val resultBody = resultResponse.body?.string().orEmpty()
                                if (!resultResponse.isSuccessful) return@withContext MakeoverResult.Error("Ergebnisfehler ${resultResponse.code}")
                                val result = JSONObject(resultBody)
                                val url = result.optJSONArray("images")?.optJSONObject(0)?.optString("url").orEmpty()
                                if (url.isBlank()) return@withContext MakeoverResult.Error("Kein Ergebnisbild erhalten.")
                                return@withContext MakeoverResult.Success(url)
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

    private fun dataUri(uri: Uri): String {
        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Bild konnte nicht gelesen werden")
        return "data:$type;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

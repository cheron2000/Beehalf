package dev.beehalf.ai.groq

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal Groq chat-completions client.
 * Never log request headers, API keys, or conversation contents.
 */
class GroqClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://api.groq.com/openai/v1"
) {
    suspend fun validate(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API key is required" }
        val request = Request.Builder()
            .url("$baseUrl/models")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Groq returned HTTP ${response.code}")
            }
            val data = JSONObject(body).optJSONArray("data") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until data.length()) {
                    data.optJSONObject(index)?.optString("id")?.takeIf(String::isNotBlank)?.let(::add)
                }
            }.sorted()
        }
    }

    suspend fun complete(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double = 0.2
    ): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API key is required" }
        require(model.isNotBlank()) { "Model ID is required" }
        require(messages.isNotEmpty()) { "At least one message is required" }
        require(temperature in 0.0..2.0) { "Temperature must be between 0 and 2" }

        val payload = JSONObject()
            .put("model", model)
            .put("temperature", temperature)
            .put("messages", org.json.JSONArray().apply {
                messages.forEach { message ->
                    put(JSONObject().put("role", message.role).put("content", message.content))
                }
            })

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Groq returned HTTP ${response.code}")
            val choices = JSONObject(body).optJSONArray("choices")
                ?: throw IOException("Groq response did not contain choices")
            choices.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?.takeIf(String::isNotBlank)
                ?: throw IOException("Groq response did not contain message content")
        }
    }
}

data class ChatMessage(val role: String, val content: String) {
    init {
        require(role in setOf("system", "user", "assistant")) { "Unsupported chat role" }
    }
}

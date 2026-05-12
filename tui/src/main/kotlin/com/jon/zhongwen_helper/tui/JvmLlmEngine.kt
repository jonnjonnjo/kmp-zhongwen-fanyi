package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.engine.LlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
private data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
)

@Serializable
private data class OllamaResponse(
    val response: String,
)

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class JvmLlmEngine(
    private val modelName: String = "qwen2.5:7b",
    private val ollamaUrl: String = "http://localhost:11434"
) : LlmEngine {

    private val client = HttpClient.newHttpClient()

    override suspend fun infer(prompt: String): String = withContext(Dispatchers.IO) {
        val body = json.encodeToString(
            OllamaRequest.serializer(),
            OllamaRequest(model = modelName, prompt = prompt)
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$ollamaUrl/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        json.decodeFromString(OllamaResponse.serializer(), response.body()).response
    }
}

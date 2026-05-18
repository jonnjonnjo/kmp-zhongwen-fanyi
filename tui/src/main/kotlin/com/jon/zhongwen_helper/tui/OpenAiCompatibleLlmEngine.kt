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
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
)

@Serializable
private data class ChatCompletionChoice(val message: ChatMessage)

@Serializable
private data class ChatCompletionResponse(val choices: List<ChatCompletionChoice>)

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class OpenAiCompatibleLlmEngine(
    private val modelName: String,
    private val baseUrl: String,
    private val apiKey: String?,
) : LlmEngine {

    private val client = HttpClient.newHttpClient()
    private val endpoint = baseUrl.trimEnd('/') + "/chat/completions"

    override suspend fun infer(prompt: String): String = withContext(Dispatchers.IO) {
        val body = json.encodeToString(
            ChatCompletionRequest.serializer(),
            ChatCompletionRequest(
                model = modelName,
                messages = listOf(ChatMessage(role = "user", content = prompt)),
            ),
        )

        val builder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
        if (!apiKey.isNullOrBlank()) builder.header("Authorization", "Bearer $apiKey")

        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("OpenAI-compatible API ${response.statusCode()}: ${response.body()}")
        }
        json.decodeFromString(ChatCompletionResponse.serializer(), response.body())
            .choices.firstOrNull()?.message?.content
            ?: error("OpenAI-compatible API returned no choices: ${response.body()}")
    }
}

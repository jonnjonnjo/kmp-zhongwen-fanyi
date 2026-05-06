package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.engine.LlmEngine
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class JvmLlmEngine(
    private val modelName: String = "qwen2.5:7b",
    private val ollamaUrl: String = "http://localhost:11434"
) : LlmEngine {

    private val client = HttpClient.newHttpClient()

    override suspend fun infer(prompt: String): String {
        val body = """
            {
                "model": "$modelName",
                "prompt": "${prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}",
                "stream": false
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$ollamaUrl/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        return Regex(""""response"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            .find(response.body())
            ?.groupValues
            ?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: error("Could not parse Ollama response")
    }
}

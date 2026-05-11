package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.core.TranslationLibrary
import kotlinx.coroutines.runBlocking

private const val USAGE = "Usage: jzw [--no-llm] [--model NAME] [--ollama-url URL] [--cedict-path FILE] <text>"
private const val DEFAULT_MODEL = "qwen2.5:7b"
private const val DEFAULT_OLLAMA_URL = "http://localhost:11434"

fun main(args: Array<String>) {
    var noLlm = false
    var model = System.getenv("JZW_MODEL") ?: DEFAULT_MODEL
    var ollamaUrl = System.getenv("JZW_OLLAMA_URL") ?: DEFAULT_OLLAMA_URL
    var cedictPath: String? = System.getenv("JZW_CEDICT_PATH")
    val inputParts = mutableListOf<String>()

    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--no-llm" -> noLlm = true
            "--model" -> {
                i++
                model = args.getOrNull(i) ?: run { System.err.println("--model requires a value"); return }
            }
            "--ollama-url" -> {
                i++
                ollamaUrl = args.getOrNull(i) ?: run { System.err.println("--ollama-url requires a value"); return }
            }
            "--cedict-path" -> {
                i++
                cedictPath = args.getOrNull(i) ?: run { System.err.println("--cedict-path requires a value"); return }
            }
            else -> inputParts.add(arg)
        }
        i++
    }

    if (inputParts.isEmpty()) {
        println(USAGE)
        return
    }

    val input = inputParts.joinToString(" ")

    val library = TranslationLibrary(
        llmEngine = if (noLlm) null else JvmLlmEngine(modelName = model, ollamaUrl = ollamaUrl),
        cedictSource = JvmCedictSource(path = cedictPath),
        segmenter = HanLpSegmenter()
    )

    runBlocking {
        val result = library.translate(input)

        println("Input  : ${result.input}")
        println("Chinese: ${result.chinese ?: "—"}")
        println("English: ${result.english ?: "—"}")
        val fullPinyin = result.breakdown.mapNotNull { it.pinyin }.joinToString(" ")
        println("Pinyin : ${fullPinyin.ifEmpty { "—" }}")
        println("─".repeat(40))
        result.breakdown.forEach {
            val pinyin = if (it.pinyin != null) "[${it.pinyin}]" else ""
            println("  ${it.token} $pinyin → ${it.meaning}")
        }
    }
}

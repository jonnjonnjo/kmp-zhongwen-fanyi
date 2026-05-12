package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.core.TranslationLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

private const val VERSION = "1.0.0"
private const val DEFAULT_MODEL = "qwen2.5:7b"
private const val DEFAULT_OLLAMA_URL = "http://localhost:11434"

private val HELP = """
    jzw — Chinese translation helper

    Usage:
      jzw [options] <text>

    Options:
      --no-llm                Skip the LLM (dictionary-only mode)
      --model NAME            Ollama model name (default: $DEFAULT_MODEL)
      --ollama-url URL        Ollama server URL (default: $DEFAULT_OLLAMA_URL)
      --cedict-path FILE      External CEDICT file (default: bundled)
      --help, -h              Show this help message
      --version, -v           Show version

    Configuration precedence: CLI flags > env vars > config file > defaults

    Environment variables:
      JZW_MODEL               Same as --model
      JZW_OLLAMA_URL          Same as --ollama-url
      JZW_CEDICT_PATH         Same as --cedict-path

    Config file:
      ${'$'}XDG_CONFIG_HOME/jzw/config.toml  (defaults to ~/.config/jzw/config.toml)
      Keys: model, ollama-url, cedict-path
""".trimIndent()

fun main(args: Array<String>) {
    val config = JzwConfig.load()

    var noLlm = false
    var model = System.getenv("JZW_MODEL") ?: config.model ?: DEFAULT_MODEL
    var ollamaUrl = System.getenv("JZW_OLLAMA_URL") ?: config.ollamaUrl ?: DEFAULT_OLLAMA_URL
    var cedictPath: String? = System.getenv("JZW_CEDICT_PATH") ?: config.cedictPath
    val inputParts = mutableListOf<String>()

    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--no-llm" -> noLlm = true
            "--help", "-h" -> { println(HELP); return }
            "--version", "-v" -> { println("jzw $VERSION"); return }
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
        println(HELP)
        return
    }

    val input = inputParts.joinToString(" ")

    val library = TranslationLibrary(
        llmEngine = if (noLlm) null else JvmLlmEngine(modelName = model, ollamaUrl = ollamaUrl),
        cedictSource = JvmCedictSource(path = cedictPath),
        segmenter = HanLpSegmenter()
    )

    runBlocking {
        val start = TimeSource.Monotonic.markNow()

        val translateJob = async(Dispatchers.IO) { library.translate(input) }

        val tickerJob = launch {
            while (isActive) {
                val elapsed = start.elapsedNow().toString(DurationUnit.SECONDS, 1)
                System.err.print("\rTranslating... $elapsed ")
                System.err.flush()
                delay(100)
            }
        }

        val result = translateJob.await()
        tickerJob.cancel()
        System.err.print("\r" + " ".repeat(40) + "\r")
        System.err.flush()

        val totalElapsed = start.elapsedNow().toString(DurationUnit.SECONDS, 2)

        println("Input  : ${result.input}")
        println("Chinese: ${result.chinese ?: "—"}")
        println("English: ${result.english ?: "—"}")
        val fullPinyin = result.breakdown.mapNotNull { it.pinyin }.joinToString(" ")
        println("Pinyin : ${fullPinyin.ifEmpty { "—" }}")
        println("─".repeat(40))
        val pinyinCol = result.breakdown.map { it.pinyin?.let { p -> "[$p]" }.orEmpty() }
        val tokenWidth = result.breakdown.maxOfOrNull { displayWidth(it.token) } ?: 0
        val pinyinWidth = pinyinCol.maxOfOrNull { displayWidth(it) } ?: 0
        result.breakdown.forEachIndexed { i, b ->
            val token = padDisplay(b.token, tokenWidth)
            val pinyin = padDisplay(pinyinCol[i], pinyinWidth)
            println("  $token  $pinyin  → ${b.meaning}")
        }
        println("─".repeat(40))
        println("Elapsed: $totalElapsed")
    }
}

private fun displayWidth(s: String): Int {
    var w = 0
    s.forEach { c ->
        val cp = c.code
        w += when {
            cp in 0x1100..0x115F -> 2
            cp in 0x2E80..0x303E -> 2
            cp in 0x3041..0x33FF -> 2
            cp in 0x3400..0x4DBF -> 2
            cp in 0x4E00..0x9FFF -> 2
            cp in 0xA000..0xA4CF -> 2
            cp in 0xAC00..0xD7A3 -> 2
            cp in 0xF900..0xFAFF -> 2
            cp in 0xFE30..0xFE4F -> 2
            cp in 0xFF00..0xFF60 -> 2
            cp in 0xFFE0..0xFFE6 -> 2
            else -> 1
        }
    }
    return w
}

private fun padDisplay(s: String, width: Int): String {
    val pad = width - displayWidth(s)
    return if (pad > 0) s + " ".repeat(pad) else s
}

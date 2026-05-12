package com.jon.zhongwen_helper.tui

import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
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

    val terminal = Terminal()

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
        val fullPinyin = result.breakdown
            .mapNotNull { it.readings.firstOrNull()?.pinyin }
            .joinToString(" ")
        println("Pinyin : ${fullPinyin.ifEmpty { "—" }}")

        terminal.println(table {
            whitespace = Whitespace.NORMAL
            overflowWrap = OverflowWrap.BREAK_WORD
            column(0) { width = ColumnWidth.Auto }
            column(1) { width = ColumnWidth.Auto }
            column(2) { width = ColumnWidth.Expand() }
            header { row("Token", "Reading", "Meaning") }
            body {
                result.breakdown.forEach { b ->
                    if (b.readings.isEmpty()) {
                        row(b.token, "", b.token)
                    } else {
                        b.readings.forEachIndexed { idx, r ->
                            row {
                                if (idx == 0) {
                                    cell(b.token) {
                                        if (b.readings.size > 1) rowSpan = b.readings.size
                                    }
                                }
                                cell("[${r.pinyin}]")
                                cell(r.meanings.joinToString(" / "))
                            }
                        }
                    }
                }
            }
        })

        println("Elapsed: $totalElapsed")
    }
}

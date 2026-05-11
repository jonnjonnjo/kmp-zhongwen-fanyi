package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.core.TranslationLibrary
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: jzw <text>")
        return
    }

    val noLlm = "--no-llm" in args
    val input = args.filter { it != "--no-llm" }.joinToString(" ")

    val library = TranslationLibrary(
        llmEngine = if (noLlm) null else JvmLlmEngine(),
        cedictSource = JvmCedictSource(),
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

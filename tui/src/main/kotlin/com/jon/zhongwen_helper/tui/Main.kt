package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.core.TranslationLibrary
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: jzw <text>")
        return
    }

    val input = args.joinToString(" ")

    val library = TranslationLibrary(
        llmEngine = JvmLlmEngine(),
        cedictSource = JvmCedictSource(),
        segmenter = HanLpSegmenter()
    )

    runBlocking {
        val result = library.translate(input)

        println("Input  : ${result.input}")
        println("Meaning: ${result.fullMeaning}")
        println("─".repeat(40))
        result.breakdown.forEach {
            val pinyin = if (it.pinyin != null) "[${it.pinyin}]" else ""
            println("  ${it.token} $pinyin → ${it.meaning}")
        }
    }
}

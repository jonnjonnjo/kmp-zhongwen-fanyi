package com.jon.zhongwen_helper

import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.jon.zhongwen_helper.core.HanLpSegmenter
import com.jon.zhongwen_helper.core.TranslationLibrary
import com.jon.zhongwen_helper.engine.JvmCedictSource
import com.jon.zhongwen_helper.engine.JvmLlmEngine
import kotlinx.coroutines.runBlocking

fun main() {
    val t = Terminal()
    val library = TranslationLibrary(
        llmEngine = JvmLlmEngine(),
        cedictSource = JvmCedictSource("shared/src/commonMain/resources/cedict/cedict.txt"),
        segmenter = HanLpSegmenter()
    )

    t.println(bold(cyan("╔══════════════════════════════════╗")))
    t.println(bold(cyan("║     Zhongwen Helper  TUI         ║")))
    t.println(bold(cyan("╚══════════════════════════════════╝")))
    t.println()
    t.println(dim("Enter Chinese or English text. Type 'quit' to exit."))
    t.println()

    while (true) {
        val input = t.prompt(cyan("▶")) ?: break
        if (input.isBlank()) continue
        if (input.trim().equals("quit", ignoreCase = true)) break

        runBlocking {
            try {
                val result = library.translate(input.trim())

                t.println()
                t.println(yellow("Input  : ") + result.input)
                t.println(green("Meaning: ") + bold(result.fullMeaning))

                if (result.breakdown.isNotEmpty()) {
                    t.println()
                    t.println(table {
                        header { row(bold("Token"), bold("Pinyin"), bold("Meaning")) }
                        body {
                            result.breakdown.forEach { bd ->
                                row(cyan(bd.token), bd.pinyin ?: dim("—"), bd.meaning)
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                t.println(red("Error: ${e.message}"))
            }
        }
        t.println()
    }

    t.println()
    t.println(dim("Goodbye!"))
}

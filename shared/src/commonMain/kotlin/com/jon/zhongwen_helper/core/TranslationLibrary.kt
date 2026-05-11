package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.engine.CedictSource
import com.jon.zhongwen_helper.engine.LlmEngine
import com.jon.zhongwen_helper.model.Lang
import com.jon.zhongwen_helper.model.Token
import com.jon.zhongwen_helper.model.TokenBreakdown
import com.jon.zhongwen_helper.model.TranslationResult

class TranslationLibrary(
        private val llmEngine: LlmEngine? = null,
        private val cedictSource: CedictSource,
        private val segmenter: Segmenter
) {
  private val dictionary: CedictDictionary by lazy { CedictDictionary(cedictSource.open()) }

  suspend fun translate(input: String): TranslationResult {
    val rawTokens = tokenize(input)
    val hasChinese = rawTokens.any { it.lang == Lang.CHINESE }
    val hasEnglish = rawTokens.any { it.lang == Lang.ENGLISH }

    val chinese: String? = when {
      hasChinese && !hasEnglish -> input
      llmEngine != null -> llmEngine.infer(buildToChinesePrompt(input)).trim()
      else -> null
    }

    val english: String? = when {
      hasEnglish && !hasChinese -> input
      chinese != null && llmEngine != null -> llmEngine.infer(buildToEnglishPrompt(chinese)).trim()
      else -> null
    }

    val breakdownTokens = prepareTokens(chinese ?: input)
    val breakdowns = breakdownTokens.map { it.toBreakdown() }

    return TranslationResult(input = input, chinese = chinese, english = english, breakdown = breakdowns)
  }

  // Never invokes the LLM. chinese is set only when input is pure Chinese; english only when pure English.
  fun translateDictionaryOnly(input: String): TranslationResult {
    val rawTokens = tokenize(input)
    val hasChinese = rawTokens.any { it.lang == Lang.CHINESE }
    val hasEnglish = rawTokens.any { it.lang == Lang.ENGLISH }

    val chinese: String? = if (hasChinese && !hasEnglish) input else null
    val english: String? = if (hasEnglish && !hasChinese) input else null

    val breakdowns = prepareTokens(input).map { it.toBreakdown() }

    return TranslationResult(input = input, chinese = chinese, english = english, breakdown = breakdowns)
  }

  private fun prepareTokens(input: String): List<Token> {
    return tokenize(input).flatMap { token ->
      if (token.lang == Lang.CHINESE) {
        segmenter.segment(token.text).map { Token(it, Lang.CHINESE) }
      } else {
        listOf(token)
      }
    }
  }

  private fun Token.toBreakdown(): TokenBreakdown {
    val entry = if (lang == Lang.CHINESE) dictionary.lookup(text) else null
    return TokenBreakdown(
      token = text,
      lang = lang,
      pinyin = entry?.pinyin?.let { numericalToTone(it) },
      meaning = entry?.meanings?.joinToString(" / ") { convertPinyinInMeaning(it) } ?: text
    )
  }

  private fun buildToChinesePrompt(input: String): String {
    return """
      Translate the following text to Chinese (Mandarin).
      Rules:
      - Reply with ONLY the Chinese translation, no explanation, no pinyin
      - Translate the EXACT meaning, do NOT add or remove anything
      Text: $input
    """.trimIndent()
  }

  private fun buildToEnglishPrompt(input: String): String {
    return """
      Translate the following text to English.
      Rules:
      - Translate the EXACT meaning, do NOT add, remove, or negate anything
      - Reply with ONLY the translated text, no explanation, no punctuation changes
      Text: $input
    """.trimIndent()
  }
}

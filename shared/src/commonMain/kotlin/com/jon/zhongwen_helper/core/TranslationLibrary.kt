package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.engine.CedictSource
import com.jon.zhongwen_helper.engine.LlmEngine
import com.jon.zhongwen_helper.model.Lang
import com.jon.zhongwen_helper.model.Token
import com.jon.zhongwen_helper.model.TokenBreakdown
import com.jon.zhongwen_helper.model.TokenReading
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
        segmenter.segment(token.text)
          .flatMap { refineAgainstDictionary(it) }
          .map { Token(it, Lang.CHINESE) }
      } else {
        listOf(token)
      }
    }
  }

  // The segmenter sometimes glues verb-object compounds (e.g. "吃苹果") that aren't in CEDICT.
  // For any segment not in the dictionary, split it via greedy longest-prefix match against CEDICT.
  private fun refineAgainstDictionary(segment: String): List<String> {
    if (segment.length <= 1 || dictionary.lookup(segment).isNotEmpty()) return listOf(segment)
    for (len in segment.length - 1 downTo 2) {
      val prefix = segment.substring(0, len)
      if (dictionary.lookup(prefix).isNotEmpty()) {
        return listOf(prefix) + refineAgainstDictionary(segment.substring(len))
      }
    }
    return listOf(segment.substring(0, 1)) + refineAgainstDictionary(segment.substring(1))
  }

  private fun Token.toBreakdown(): TokenBreakdown {
    val entries = if (lang == Lang.CHINESE) dictionary.lookup(text) else emptyList()
    val readings = entries
      .groupBy { it.pinyin }
      .map { (rawPinyin, group) ->
        TokenReading(
          pinyin = numericalToTone(rawPinyin),
          meanings = group.flatMap { it.meanings }.map { convertPinyinInMeaning(it) },
        )
      }
    return TokenBreakdown(token = text, lang = lang, readings = readings)
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

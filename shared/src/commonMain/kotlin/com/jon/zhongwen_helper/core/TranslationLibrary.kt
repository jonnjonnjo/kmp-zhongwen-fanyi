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
    val tokens = prepareTokens(input)
    return when {
      llmEngine != null && route(tokens) is LookupRoute.Llm -> llmLookup(input, tokens)
      else -> dictionaryLookup(input, tokens)
    }
  }

  // Best-effort: English tokens echo back with meaning = tokenText, pinyin = null, fullMeaning = null.
  fun translateDictionaryOnly(input: String): TranslationResult {
    val tokens = prepareTokens(input)
    val breakdowns = tokens.map { token ->
      val entry = dictionary.lookup(token.text)
      TokenBreakdown(
        token = token.text,
        lang = token.lang,
        pinyin = entry?.pinyin?.let { numericalToTone(it) },
        meaning = entry?.meanings?.joinToString(" / ") { convertPinyinInMeaning(it) } ?: token.text
      )
    }
    return TranslationResult(input = input, fullMeaning = null, breakdown = breakdowns)
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

  private suspend fun dictionaryLookup(input: String, tokens: List<Token>): TranslationResult {
    val breakdowns = mutableListOf<TokenBreakdown>()

    for (token in tokens) {
      val entry = dictionary.lookup(token.text)
      if (entry == null && llmEngine != null) return llmLookup(input, tokens)
      breakdowns.add(
        TokenBreakdown(
          token = token.text,
          lang = token.lang,
          pinyin = entry?.pinyin?.let { numericalToTone(it) },
          meaning = entry?.meanings?.joinToString(" / ") { convertPinyinInMeaning(it) } ?: token.text
        )
      )
    }

    val fullMeaning = llmEngine?.infer(buildFullMeaningPrompt(input))

    return TranslationResult(
      input = input,
      fullMeaning = fullMeaning,
      breakdown = breakdowns
    )
  }

  private suspend fun llmLookup(input: String, tokens: List<Token>): TranslationResult {
    val chineseOutput = requireNotNull(llmEngine).infer(buildToChinesePrompt(input)).trim()

    val outputTokens = prepareTokens(chineseOutput)

    val breakdowns = outputTokens.map { token ->
      val entry = if (token.lang == Lang.CHINESE) dictionary.lookup(token.text) else null
      TokenBreakdown(
        token = token.text,
        lang = token.lang,
        pinyin = entry?.pinyin?.let { numericalToTone(it) },
        meaning = entry?.meanings?.joinToString(" / ") { convertPinyinInMeaning(it) } ?: token.text
      )
    }

    return TranslationResult(
      input = input,
      fullMeaning = chineseOutput,
      breakdown = breakdowns
    )
  }

  private fun buildFullMeaningPrompt(input: String): String {
    return """
      Translate the following text to English.
      Rules:
      - Translate the EXACT meaning, do NOT add, remove, or negate anything
      - Reply with ONLY the translated text, no explanation, no punctuation changes
      Text: $input
    """.trimIndent()
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
}

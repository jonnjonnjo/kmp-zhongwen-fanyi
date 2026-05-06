package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.engine.CedictSource
import com.jon.zhongwen_helper.engine.LlmEngine
import com.jon.zhongwen_helper.model.Lang
import com.jon.zhongwen_helper.model.Token
import com.jon.zhongwen_helper.model.TokenBreakdown
import com.jon.zhongwen_helper.model.TranslationResult

class TranslationLibrary(
        private val llmEngine: LlmEngine,
        private val cedictSource: CedictSource,
        private val segmenter: Segmenter
) {
  private val dictionary: CedictDictionary by lazy { CedictDictionary(cedictSource.open()) }

  suspend fun translate(input: String): TranslationResult {
    val rawTokens = tokenize(input)

    val tokens =
            rawTokens.flatMap { token ->
              if (token.lang == Lang.CHINESE) {
                segmenter.segment(token.text).map { Token(it, Lang.CHINESE) }
              } else {
                listOf(token)
              }
            }

    return when (route(tokens)) {
      is LookupRoute.Dictionary -> dictionaryLookup(input, tokens)
      is LookupRoute.Llm -> llmLookup(input, tokens)
    }
  }

  private suspend fun dictionaryLookup(input: String, tokens: List<Token>): TranslationResult {
    val token = tokens.first()
    val entry = dictionary.lookup(token.text)

    return if (entry != null) {
      TranslationResult(
              input = input,
              fullMeaning = entry.meanings.first(),
              breakdown =
                      listOf(
                              TokenBreakdown(
                                      token = token.text,
                                      lang = token.lang,
                                      pinyin = entry.pinyin,
                                      meaning = entry.meanings.joinToString(" / ")
                              )
                      )
      )
    } else {
      llmLookup(input, tokens)
    }
  }

  private suspend fun llmLookup(input: String, tokens: List<Token>): TranslationResult {
    val prompt = buildPrompt(input)
    val raw = llmEngine.infer(prompt)
    return parseResponse(input, raw)
  }

  private fun buildPrompt(input: String): String {
    return """
            You are a Chinese-English translation assistant.
            Given the input, return ONLY a JSON object in this exact format, no explanation:
            {
                "fullMeaning": "the full translation",
                "breakdown": [
                    { "token": "word", "pinyin": "pīnyīn or null", "meaning": "meaning" }
                ]
            }
            Rules:
            - fullMeaning is always in the opposite language of the input
            - breakdown reflects the fullMeaning side, always in Chinese tokens
            - pinyin is null for English tokens
            Input: $input
        """.trimIndent()
  }

  private fun parseResponse(input: String, raw: String): TranslationResult {
    val fullMeaning =
            Regex(""""fullMeaning"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1) ?: raw

    val breakdown =
            Regex(
                            """"token"\s*:\s*"([^"]+)"\s*,\s*"pinyin"\s*:\s*"?([^",}]*)"?\s*,\s*"meaning"\s*:\s*"([^"]+)""""
                    )
                    .findAll(raw)
                    .map {
                      TokenBreakdown(
                              token = it.groupValues[1],
                              lang =
                                      if (it.groupValues[1].any { c -> c.code in 0x4E00..0x9FFF })
                                              Lang.CHINESE
                                      else Lang.ENGLISH,
                              pinyin =
                                      it.groupValues[2].takeIf { p ->
                                        p.isNotBlank() && p != "null"
                                      },
                              meaning = it.groupValues[3]
                      )
                    }
                    .toList()

    return TranslationResult(input = input, fullMeaning = fullMeaning, breakdown = breakdown)
  }
}

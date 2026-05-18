package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.model.Lang
import com.jon.zhongwen_helper.model.Token

fun tokenize(input: String): List<Token> {
  val tokens = mutableListOf<Token>()
  val buffer = StringBuilder()
  var currentLang: Lang? = null

  fun flush() {
    if (buffer.isNotEmpty()) {
      tokens.add(Token(buffer.toString(), currentLang!!))
      buffer.clear()
      currentLang = null
    }
  }

  for (char in input.trim()) {
    if (char.isWhitespace() || isPunctuation(char)) {
      flush()
      continue
    }

    val charLang = if (char.code in 0x4E00..0x9FFF) Lang.CHINESE else Lang.ENGLISH

    if (currentLang == null || charLang == currentLang) {
      buffer.append(char)
      currentLang = charLang
    } else {
      flush()
      buffer.append(char)
      currentLang = charLang
    }
  }

  flush()
  return tokens
}

// Treats anything that isn't a letter, digit, or whitespace as punctuation —
// covers both ASCII (.,!?;:'"...) and CJK (。，！？「」…) punctuation in one rule.
private fun isPunctuation(char: Char): Boolean =
  !char.isLetterOrDigit() && !char.isWhitespace()

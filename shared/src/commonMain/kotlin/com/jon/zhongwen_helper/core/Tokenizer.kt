package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.model.Lang
import com.jon.zhongwen_helper.model.Token

fun tokenize(input: String): List<Token> {
  val tokens = mutableListOf<Token>()
  val buffer = StringBuilder()
  var currentLang: Lang? = null

  for (char in input.trim()) {
    if (char.isWhitespace()) {
      if (buffer.isNotEmpty()) {
        tokens.add(Token(buffer.toString(), currentLang!!))
        buffer.clear()
        currentLang = null
      }
      continue
    }

    val charLang = if (char.code in 0x4E00..0x9FFF) Lang.CHINESE else Lang.ENGLISH

    if (currentLang == null || charLang == currentLang) {
      buffer.append(char)
      currentLang = charLang
    } else {
      tokens.add(Token(buffer.toString(), currentLang!!))
      buffer.clear()
      buffer.append(char)
      currentLang = charLang
    }
  }

  if (buffer.isNotEmpty()) {
    tokens.add(Token(buffer.toString(), currentLang!!))
  }

  return tokens
}

package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.model.Token

sealed class LookupRoute {
  object Dictionary : LookupRoute()
  object Llm : LookupRoute()
}

fun route(tokens: List<Token>): LookupRoute {
  val isMixed = tokens.map { it.lang }.toSet().size > 1
  val isSentence = tokens.size > 1

  return if (!isMixed && !isSentence) LookupRoute.Dictionary else LookupRoute.Llm
}

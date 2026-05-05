package com.jon.zhongwen_helper.model

enum class Lang {
  CHINESE,
  ENGLISH
}

data class Token(
        val text: String,
        val lang: Lang,
)

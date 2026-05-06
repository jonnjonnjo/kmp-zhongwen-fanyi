package com.jon.zhongwen_helper.engine

interface LlmEngine {
  suspend fun infer(prompt: String): String
}

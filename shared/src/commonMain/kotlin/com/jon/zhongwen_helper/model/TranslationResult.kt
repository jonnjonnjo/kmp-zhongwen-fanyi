package com.jon.zhongwen_helper.model

data class TranslationResult(
        val input: String,
        val chinese: String?,
        val english: String?,
        val breakdown: List<TokenBreakdown>
)

data class TokenBreakdown(
        val token: String,
        val lang: Lang,
        val pinyin: String?,
        val meaning: String,
)

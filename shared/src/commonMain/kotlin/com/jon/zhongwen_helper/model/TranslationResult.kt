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
        val readings: List<TokenReading>,
)

data class TokenReading(
        val pinyin: String,
        val meanings: List<String>,
)

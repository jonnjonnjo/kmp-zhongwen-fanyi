package com.jon.zhongwen_helper.model

data class TranslationResult(
        val input: String,
        val fullMeaning: String?,
        val breakdown: List<TokenBreakdown>
)

data class TokenBreakdown(
        val token: String,
        val lang: Lang,
        val pinyin: String?,
        val meaning: String,
)

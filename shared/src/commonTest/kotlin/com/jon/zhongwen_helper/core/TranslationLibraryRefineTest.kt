package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.engine.CedictSource
import kotlin.test.Test
import kotlin.test.assertEquals

private class FixedSegmenter(private val map: Map<String, List<String>>) : Segmenter {
    override fun segment(text: String): List<String> = map[text] ?: listOf(text)
}

private class InMemoryCedict(private val text: String) : CedictSource {
    override fun open(): ByteArray = text.encodeToByteArray()
}

class TranslationLibraryRefineTest {

    private fun library(dict: String, segmenterMap: Map<String, List<String>>) =
        TranslationLibrary(
            llmEngine = null,
            cedictSource = InMemoryCedict(dict),
            segmenter = FixedSegmenter(segmenterMap),
        )

    @Test
    fun verbObjectCompoundIsSplitWhenNotInDictionary() {
        // HanLP groups "吃苹果" as one segment, but it isn't in CEDICT.
        // Greedy longest-prefix match should split it into ["吃", "苹果"].
        val lib = library(
            dict = """
                吃 吃 [chi1] /to eat/
                苹果 苹果 [ping2 guo3] /apple/
            """.trimIndent(),
            segmenterMap = mapOf("吃苹果" to listOf("吃苹果")),
        )
        val r = lib.translateDictionaryOnly("吃苹果")
        assertEquals(listOf("吃", "苹果"), r.breakdown.map { it.token })
    }

    @Test
    fun multiCharSegmentInDictionaryIsLeftAlone() {
        val lib = library(
            dict = "苹果 苹果 [ping2 guo3] /apple/",
            segmenterMap = mapOf("苹果" to listOf("苹果")),
        )
        val r = lib.translateDictionaryOnly("苹果")
        assertEquals(listOf("苹果"), r.breakdown.map { it.token })
    }

    @Test
    fun unsplittableSegmentPeelsOneCharAtATime() {
        // No prefix matches in CEDICT — refinement should still make progress.
        val lib = library(
            dict = "苹果 苹果 [ping2 guo3] /apple/",
            segmenterMap = mapOf("xyz苹果" to listOf("xyz苹果")),
        )
        val r = lib.translateDictionaryOnly("xyz苹果")
        // tokenize() splits at script boundary, so "xyz" goes through as English first,
        // then "苹果" arrives as a single Chinese segment.
        val chineseTokens = r.breakdown.filter { it.token.any { c -> c.code in 0x4E00..0x9FFF } }
        assertEquals(listOf("苹果"), chineseTokens.map { it.token })
    }
}

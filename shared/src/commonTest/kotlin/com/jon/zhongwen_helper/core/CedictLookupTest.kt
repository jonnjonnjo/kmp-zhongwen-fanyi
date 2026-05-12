package com.jon.zhongwen_helper.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CedictLookupTest {

    private fun dict(vararg lines: String): CedictDictionary =
        CedictDictionary(lines.joinToString("\n").encodeToByteArray())

    @Test
    fun parsesStandardLine() {
        val d = dict("你好 你好 [ni3 hao3] /hello/hi/")
        val entries = d.lookup("你好")
        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("你好", entry.traditional)
        assertEquals("你好", entry.simplified)
        assertEquals("ni3 hao3", entry.pinyin)
        assertEquals(listOf("hello", "hi"), entry.meanings)
    }

    @Test
    fun lookupByTraditionalAndSimplifiedBothWork() {
        val d = dict("學習 学习 [xue2 xi2] /to study/to learn/")
        assertEquals(1, d.lookup("學習").size)
        assertEquals(1, d.lookup("学习").size)
        assertEquals(d.lookup("學習"), d.lookup("学习"))
    }

    @Test
    fun commentLinesAreSkipped() {
        val d = dict(
            "# this is a comment",
            "你好 你好 [ni3 hao3] /hello/",
            "# trailing comment",
        )
        assertEquals(1, d.lookup("你好").size)
    }

    @Test
    fun blankLinesAreSkipped() {
        val d = dict(
            "",
            "你好 你好 [ni3 hao3] /hello/",
            "",
        )
        assertEquals(1, d.lookup("你好").size)
    }

    @Test
    fun malformedLinesAreSilentlyIgnored() {
        val d = dict(
            "this is not a CEDICT line",
            "你好 你好 [ni3 hao3] /hello/",
            "another garbage line {{",
        )
        assertEquals(1, d.lookup("你好").size)
    }

    @Test
    fun multipleMeaningsArePreserved() {
        val d = dict("研究 研究 [yan2 jiu1] /research/a study/to look into/")
        val entry = d.lookup("研究").single()
        assertEquals(listOf("research", "a study", "to look into"), entry.meanings)
    }

    @Test
    fun unknownWordReturnsEmptyList() {
        val d = dict("你好 你好 [ni3 hao3] /hello/")
        assertTrue(d.lookup("再见").isEmpty())
    }

    @Test
    fun multipleEntriesIndexedSeparately() {
        val d = dict(
            "你好 你好 [ni3 hao3] /hello/",
            "再见 再见 [zai4 jian4] /goodbye/",
        )
        assertEquals("hello", d.lookup("你好").single().meanings.first())
        assertEquals("goodbye", d.lookup("再见").single().meanings.first())
    }

    @Test
    fun multipleReadingsForSameWordAreReturnedInOrder() {
        val d = dict(
            "和 和 [He2] /surname He/",
            "和 和 [he2] /and; together with/sum/",
            "和 和 [he4] /to compose a poem in reply/",
            "龢 和 [he2] /(literary) harmonious (variant of 和[he2])/",
        )
        val entries = d.lookup("和")
        assertEquals(4, entries.size)
        assertEquals(listOf("He2", "he2", "he4", "he2"), entries.map { it.pinyin })
        assertEquals("(literary) harmonious (variant of 和[he2])", entries[3].meanings.single())
    }

    @Test
    fun traditionalVariantLookupReturnsOnlyItsOwnEntry() {
        val d = dict(
            "和 和 [he2] /and; together with/",
            "龢 和 [he2] /(literary) harmonious (variant of 和[he2])/",
        )
        val entries = d.lookup("龢")
        assertEquals(1, entries.size)
        assertEquals("(literary) harmonious (variant of 和[he2])", entries[0].meanings.single())
    }
}

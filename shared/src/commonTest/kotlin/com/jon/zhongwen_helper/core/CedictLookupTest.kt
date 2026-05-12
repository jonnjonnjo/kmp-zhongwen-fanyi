package com.jon.zhongwen_helper.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CedictLookupTest {

    private fun dict(vararg lines: String): CedictDictionary =
        CedictDictionary(lines.joinToString("\n").encodeToByteArray())

    @Test
    fun parsesStandardLine() {
        val d = dict("你好 你好 [ni3 hao3] /hello/hi/")
        val entry = d.lookup("你好")
        assertNotNull(entry)
        assertEquals("你好", entry.traditional)
        assertEquals("你好", entry.simplified)
        assertEquals("ni3 hao3", entry.pinyin)
        assertEquals(listOf("hello", "hi"), entry.meanings)
    }

    @Test
    fun lookupByTraditionalAndSimplifiedBothWork() {
        // Different traditional vs simplified form of "to study".
        val d = dict("學習 学习 [xue2 xi2] /to study/to learn/")
        assertNotNull(d.lookup("學習"))
        assertNotNull(d.lookup("学习"))
        assertEquals(d.lookup("學習"), d.lookup("学习"))
    }

    @Test
    fun commentLinesAreSkipped() {
        val d = dict(
            "# this is a comment",
            "你好 你好 [ni3 hao3] /hello/",
            "# trailing comment",
        )
        assertNotNull(d.lookup("你好"))
    }

    @Test
    fun blankLinesAreSkipped() {
        val d = dict(
            "",
            "你好 你好 [ni3 hao3] /hello/",
            "",
        )
        assertNotNull(d.lookup("你好"))
    }

    @Test
    fun malformedLinesAreSilentlyIgnored() {
        val d = dict(
            "this is not a CEDICT line",
            "你好 你好 [ni3 hao3] /hello/",
            "another garbage line {{",
        )
        assertNotNull(d.lookup("你好"))
    }

    @Test
    fun multipleMeaningsArePreserved() {
        val d = dict("研究 研究 [yan2 jiu1] /research/a study/to look into/")
        val entry = d.lookup("研究")
        assertNotNull(entry)
        assertEquals(listOf("research", "a study", "to look into"), entry.meanings)
    }

    @Test
    fun unknownWordReturnsNull() {
        val d = dict("你好 你好 [ni3 hao3] /hello/")
        assertNull(d.lookup("再见"))
    }

    @Test
    fun multipleEntriesIndexedSeparately() {
        val d = dict(
            "你好 你好 [ni3 hao3] /hello/",
            "再见 再见 [zai4 jian4] /goodbye/",
        )
        assertEquals("hello", d.lookup("你好")?.meanings?.first())
        assertEquals("goodbye", d.lookup("再见")?.meanings?.first())
    }
}

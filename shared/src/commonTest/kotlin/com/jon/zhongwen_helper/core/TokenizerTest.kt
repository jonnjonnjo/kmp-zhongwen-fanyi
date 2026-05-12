package com.jon.zhongwen_helper.core

import com.jon.zhongwen_helper.model.Lang
import com.jon.zhongwen_helper.model.Token
import kotlin.test.Test
import kotlin.test.assertEquals

class TokenizerTest {

    @Test
    fun emptyInputReturnsEmptyList() {
        assertEquals(emptyList(), tokenize(""))
    }

    @Test
    fun whitespaceOnlyReturnsEmptyList() {
        assertEquals(emptyList(), tokenize("   "))
    }

    @Test
    fun pureChinese() {
        assertEquals(listOf(Token("你好", Lang.CHINESE)), tokenize("你好"))
    }

    @Test
    fun pureEnglish() {
        assertEquals(listOf(Token("hello", Lang.ENGLISH)), tokenize("hello"))
    }

    @Test
    fun splitsOnWhitespaceBetweenEnglishWords() {
        assertEquals(
            listOf(Token("hello", Lang.ENGLISH), Token("world", Lang.ENGLISH)),
            tokenize("hello world")
        )
    }

    @Test
    fun splitsOnWhitespaceBetweenChineseWords() {
        assertEquals(
            listOf(Token("你好", Lang.CHINESE), Token("世界", Lang.CHINESE)),
            tokenize("你好 世界")
        )
    }

    @Test
    fun splitsOnScriptBoundaryWithoutWhitespace() {
        assertEquals(
            listOf(
                Token("我", Lang.CHINESE),
                Token("like", Lang.ENGLISH),
                Token("学习", Lang.CHINESE),
            ),
            tokenize("我like学习")
        )
    }

    @Test
    fun leadingAndTrailingWhitespaceIgnored() {
        assertEquals(
            listOf(Token("你好", Lang.CHINESE)),
            tokenize("   你好   ")
        )
    }

    @Test
    fun multipleConsecutiveSpacesCollapse() {
        assertEquals(
            listOf(Token("a", Lang.ENGLISH), Token("b", Lang.ENGLISH)),
            tokenize("a     b")
        )
    }

    @Test
    fun chinesePunctuationIsClassifiedAsNonChinese() {
        // 。 (U+3002) is in CJK Symbols & Punctuation, outside the 0x4E00..0x9FFF range,
        // so the tokenizer treats it as ENGLISH. Documents current behavior.
        assertEquals(
            listOf(Token("你好", Lang.CHINESE), Token("。", Lang.ENGLISH)),
            tokenize("你好。")
        )
    }

    @Test
    fun digitsAreClassifiedAsEnglish() {
        assertEquals(
            listOf(Token("abc123", Lang.ENGLISH)),
            tokenize("abc123")
        )
    }
}

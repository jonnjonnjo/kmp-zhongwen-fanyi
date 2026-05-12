package com.jon.zhongwen_helper.core

import kotlin.test.Test
import kotlin.test.assertEquals

class PinyinConverterTest {

    @Test
    fun singleSyllableTone3() {
        assertEquals("nǐ", numericalToTone("ni3"))
    }

    @Test
    fun singleSyllableTone3OnA() {
        assertEquals("hǎo", numericalToTone("hao3"))
    }

    @Test
    fun twoSyllablesSeparatedBySpace() {
        assertEquals("nǐ hǎo", numericalToTone("ni3 hao3"))
    }

    @Test
    fun allFourTonesOnA() {
        assertEquals("mā má mǎ mà", numericalToTone("ma1 ma2 ma3 ma4"))
    }

    @Test
    fun toneRuleAOrEWinsOverOtherVowels() {
        // Rule 1: if 'a' or 'e' is present, it takes the mark even if not first.
        assertEquals("lái", numericalToTone("lai2"))
        assertEquals("hěi", numericalToTone("hei3"))
    }

    @Test
    fun toneRuleOuPlacesMarkOnO() {
        assertEquals("kǒu", numericalToTone("kou3"))
        assertEquals("yǒu", numericalToTone("you3"))
    }

    @Test
    fun toneRuleLastVowelWhenNoAOrEAndNoOu() {
        // No a/e, no "ou" → last vowel takes the mark.
        assertEquals("shì", numericalToTone("shi4"))
        assertEquals("wǔ", numericalToTone("wu3"))
    }

    @Test
    fun uColonBecomesUmlautU() {
        // CEDICT writes ü as "u:". Test: 女 = "nu:3" → "nǚ".
        assertEquals("nǚ", numericalToTone("nu:3"))
    }

    @Test
    fun toneless0IsTreatedAsNoTone() {
        // Syllables ending with no digit (or with 0/5 for neutral) leave the syllable mostly intact.
        // The code's behavior: if last char isn't a digit, return as-is.
        assertEquals("de", numericalToTone("de"))
    }

    @Test
    fun convertPinyinInMeaningRewritesNumericBrackets() {
        assertEquals(
            "to like [xǐ huān]",
            convertPinyinInMeaning("to like [xi3 huan1]")
        )
    }

    @Test
    fun convertPinyinInMeaningLeavesNonNumericBracketsAlone() {
        // No digits inside brackets → leave as-is.
        assertEquals(
            "see [character]",
            convertPinyinInMeaning("see [character]")
        )
    }

    @Test
    fun convertPinyinInMeaningHandlesMultipleBrackets() {
        assertEquals(
            "[hàn] and [zì]",
            convertPinyinInMeaning("[han4] and [zi4]")
        )
    }

    @Test
    fun convertPinyinInMeaningWithNoBrackets() {
        assertEquals("plain text", convertPinyinInMeaning("plain text"))
    }
}

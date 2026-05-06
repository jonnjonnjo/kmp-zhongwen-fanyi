package com.jon.zhongwen_helper.core

private val toneMap = mapOf(
    'a' to arrayOf("ā", "á", "ǎ", "à", "a"),
    'e' to arrayOf("ē", "é", "ě", "è", "e"),
    'i' to arrayOf("ī", "í", "ǐ", "ì", "i"),
    'o' to arrayOf("ō", "ó", "ǒ", "ò", "o"),
    'u' to arrayOf("ū", "ú", "ǔ", "ù", "u"),
    'v' to arrayOf("ǖ", "ǘ", "ǚ", "ǜ", "ü"),
)

fun numericalToTone(pinyin: String): String {
    return pinyin.split(" ").joinToString(" ") { convertSyllable(it) }
}

private fun convertSyllable(syllable: String): String {
    if (syllable.isEmpty()) return syllable

    val tone = syllable.last().digitToIntOrNull() ?: return syllable
    val base = syllable.dropLast(1).replace("u:", "v")

    val markedIndex = findToneIndex(base)
    if (markedIndex == -1) return base

    val vowel = base[markedIndex]
    val replacement = toneMap[vowel]?.getOrNull(tone - 1) ?: return base
    return base.substring(0, markedIndex) + replacement + base.substring(markedIndex + 1)
}

private fun findToneIndex(syllable: String): Int {
    // Rule 1: a or e always takes the mark
    val aeIndex = syllable.indexOfFirst { it == 'a' || it == 'e' }
    if (aeIndex != -1) return aeIndex

    // Rule 2: ou → o takes the mark
    val ouIndex = syllable.indexOf("ou")
    if (ouIndex != -1) return ouIndex

    // Rule 3: last vowel takes the mark
    return syllable.indexOfLast { it in toneMap }
}

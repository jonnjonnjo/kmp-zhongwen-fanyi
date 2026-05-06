package com.jon.zhongwen_helper.core

data class CedictEntry(
        val traditional: String,
        val simplified: String,
        val pinyin: String,
        val meanings: List<String>
)

class CedictDictionary(private val source: ByteArray) {

  private val entries: Map<String, CedictEntry> by lazy { parse() }

  private fun parse(): Map<String, CedictEntry> {
    val map = mutableMapOf<String, CedictEntry>()
    val text = source.decodeToString()

    for (line in text.lines()) {
      if (line.startsWith("#") || line.isBlank()) continue

      // format: Traditional Simplified [pinyin] /meaning1/meaning2/
      val regex = Regex("""^(\S+)\s+(\S+)\s+\[([^\]]+)\]\s+/(.+)/$""")
      val match = regex.matchEntire(line.trim()) ?: continue

      val traditional = match.groupValues[1]
      val simplified = match.groupValues[2]
      val pinyin = match.groupValues[3]
      val meanings = match.groupValues[4].split("/")

      val entry = CedictEntry(traditional, simplified, pinyin, meanings)
      map[simplified] = entry
      map[traditional] = entry
    }

    return map
  }

  fun lookup(word: String): CedictEntry? = entries[word]
}

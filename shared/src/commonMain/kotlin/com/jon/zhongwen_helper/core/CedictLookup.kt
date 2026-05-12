package com.jon.zhongwen_helper.core

data class CedictEntry(
        val traditional: String,
        val simplified: String,
        val pinyin: String,
        val meanings: List<String>
)

class CedictDictionary(private val source: ByteArray) {

  private val entries: Map<String, List<CedictEntry>> by lazy { parse() }

  private fun parse(): Map<String, List<CedictEntry>> {
    val map = mutableMapOf<String, MutableList<CedictEntry>>()
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
      map.getOrPut(simplified) { mutableListOf() }.add(entry)
      if (traditional != simplified) {
        map.getOrPut(traditional) { mutableListOf() }.add(entry)
      }
    }

    return map
  }

  fun lookup(word: String): List<CedictEntry> = entries[word].orEmpty()
}

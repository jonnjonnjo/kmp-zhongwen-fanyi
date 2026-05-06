package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.engine.CedictSource

class JvmCedictSource : CedictSource {
    override fun open(): ByteArray =
        JvmCedictSource::class.java.classLoader
            .getResourceAsStream("cedict/cedict.txt")
            ?.readBytes()
            ?: error("CEDICT resource not found on classpath")
}

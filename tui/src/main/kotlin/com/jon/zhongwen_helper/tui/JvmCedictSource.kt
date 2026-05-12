package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.engine.CedictSource
import java.io.File

class JvmCedictSource(private val path: String? = null) : CedictSource {
    override fun open(): ByteArray {
        if (path != null) return File(path).readBytes()
        return JvmCedictSource::class.java.classLoader
            .getResourceAsStream("cedict/cedict.txt")
            ?.readBytes()
            ?: error("CEDICT resource not found on classpath")
    }
}

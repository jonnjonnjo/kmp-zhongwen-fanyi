package com.jon.zhongwen_helper.engine

import java.io.File

class JvmCedictSource(private val path: String) : CedictSource {
  override fun open(): ByteArray = File(path).readBytes()
}

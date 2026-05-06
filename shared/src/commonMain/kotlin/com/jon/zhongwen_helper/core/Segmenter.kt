package com.jon.zhongwen_helper.core

interface Segmenter {
  fun segment(text: String): List<String>
}

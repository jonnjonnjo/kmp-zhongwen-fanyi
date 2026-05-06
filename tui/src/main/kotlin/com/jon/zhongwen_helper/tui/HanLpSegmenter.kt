package com.jon.zhongwen_helper.tui

import com.hankcs.hanlp.HanLP
import com.jon.zhongwen_helper.core.Segmenter

class HanLpSegmenter : Segmenter {
    override fun segment(text: String): List<String> = HanLP.segment(text).map { it.word }
}

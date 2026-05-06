package com.jon.zhongwen_helper

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

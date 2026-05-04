package com.jon.zhongwen_helper

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "zhongwen-helper",
    ) {
        App()
    }
}
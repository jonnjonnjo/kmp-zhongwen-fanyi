plugins {
    alias(libs.plugins.kotlinJvm)
    kotlin("plugin.serialization") version "2.3.20"
    application
}

group = "com.jon.zhongwen_helper"
version = "1.0.0"

application {
    mainClass.set("com.jon.zhongwen_helper.tui.MainKt")
    applicationName = "jzw"
}

dependencies {
    implementation(projects.shared)
    implementation("com.hankcs:hanlp:portable-1.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

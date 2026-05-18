package com.jon.zhongwen_helper.tui

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Serializable
data class JzwConfig(
    val model: String? = null,
    @kotlinx.serialization.SerialName("base-url")
    val baseUrl: String? = null,
    @kotlinx.serialization.SerialName("api-key")
    val apiKey: String? = null,
    @kotlinx.serialization.SerialName("cedict-path")
    val cedictPath: String? = null,
) {
    companion object {
        fun configPath(): Path {
            val xdg = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
            val base = xdg?.let { Paths.get(it) }
                ?: Paths.get(System.getProperty("user.home"), ".config")
            return base.resolve("jzw").resolve("config.toml")
        }

        fun load(path: Path = configPath()): JzwConfig {
            if (!Files.isRegularFile(path)) return JzwConfig()
            val text = Files.readString(path)
            return Toml(inputConfig = TomlInputConfig(ignoreUnknownNames = true))
                .decodeFromString(serializer(), text)
        }
    }
}

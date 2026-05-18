package com.jon.zhongwen_helper.tui

import com.jon.zhongwen_helper.engine.LlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class CachedLlmEngine(
    private val inner: LlmEngine,
    private val modelName: String,
    private val cacheDir: Path = defaultCacheDir(),
) : LlmEngine {

    init {
        Files.createDirectories(cacheDir)
    }

    override suspend fun infer(prompt: String): String = withContext(Dispatchers.IO) {
        val keyFile = cacheDir.resolve(hashKey(modelName, prompt))
        if (Files.isRegularFile(keyFile)) {
            return@withContext Files.readString(keyFile)
        }
        val response = inner.infer(prompt)
        val tmp = Files.createTempFile(cacheDir, ".tmp-", "")
        Files.writeString(tmp, response)
        Files.move(tmp, keyFile, StandardCopyOption.REPLACE_EXISTING)
        response
    }

    companion object {
        fun defaultCacheDir(): Path {
            val xdg = System.getenv("XDG_CACHE_HOME")?.takeIf { it.isNotBlank() }
            val base = xdg?.let { Paths.get(it) }
                ?: Paths.get(System.getProperty("user.home"), ".cache")
            return base.resolve("jzw").resolve("llm")
        }

        private fun hashKey(model: String, prompt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(model.toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(prompt.toByteArray(Charsets.UTF_8))
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

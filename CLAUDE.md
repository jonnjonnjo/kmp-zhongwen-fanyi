# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`zhongwen-helper` is a Kotlin Multiplatform translator that turns mixed 中文/英文 input into a structured `TranslationResult` — a full-meaning translation plus a per-token breakdown (pinyin + dictionary meaning). The core logic lives in `shared` and is reused across frontends (terminal, Compose Desktop/Android, Ktor server) through a small dependency-injection contract.

`README.md` is the spec for translation behavior across the three input shapes (中文-only / 英文-only / mixed) and when AI is required vs. dictionary-only. Treat it as authoritative when changing the pipeline.

## Modules

Four Gradle modules:

- **`shared`** — KMP library (`androidTarget` + `jvm`). All translation logic lives here in `commonMain`. No third-party deps. Intentionally platform-free: all I/O flows through injected interfaces, not `expect/actual`.
- **`tui`** — Plain JVM module (`kotlinJvm`, not KMP). The only currently-working frontend: a Mordant terminal app. Provides the JVM implementations of `LlmEngine`, `CedictSource`, `Segmenter`. JVM-only because HanLP is JVM-only.
- **`composeApp`** — Compose Multiplatform UI for Android + Desktop. Currently a stub (`App()` renders one `Text`); not yet wired to `TranslationLibrary`. Note: `composeApp/src/jvmMain/.../main.kt` is mid-refactor and imports types from packages they don't live in — it will not compile until those imports are fixed or the JVM implementations are moved into `shared`/`composeApp`.
- **`server`** — Ktor (Netty, port `SERVER_PORT = 8080`). Stub. References a `Greeting()` class that doesn't exist anywhere in the repo, so it won't compile as-is.

## Translation pipeline

`shared/.../core/TranslationLibrary.kt` is the entry point. Its constructor takes three abstractions:

- `LlmEngine` — `suspend infer(prompt: String): String` (nullable; library degrades gracefully without it)
- `CedictSource` — `open(): ByteArray` returning a CEDICT-formatted file
- `Segmenter` — `segment(text: String): List<String>` for breaking continuous Chinese into words

`translate(input)` flow:
1. `tokenize()` (in `core/Tokenizer.kt`) splits on whitespace and script boundary — Chinese block `0x4E00..0x9FFF` vs everything else → `Lang.ENGLISH`.
2. Each Chinese token is further segmented via `Segmenter` (HanLP in the TUI).
3. `route(tokens)` (in `core/Router.kt`) → `LookupRoute.Llm` if both languages are present, else `LookupRoute.Dictionary`.
4. **Dictionary path**: per-token CEDICT lookup via `CedictDictionary` (`core/CedictLookup.kt`). If any lookup misses and `LlmEngine != null`, fall back to the LLM path. `fullMeaning` is asked from the LLM (Chinese→English prompt) when available, else `null`.
5. **LLM path**: prompt the LLM for the Chinese translation, then re-tokenize/segment/CEDICT-lookup the output to build the breakdown.

`core/PinyinConverter.kt`:
- `numericalToTone("ni3 hao3")` → `"nǐ hǎo"` (CEDICT stores numeric tones).
- `convertPinyinInMeaning(...)` rewrites embedded `[xxx]` pinyin inside meaning strings, since CEDICT glosses often inline pinyin like `/likes [xi3 huan1]/`.

## Frontend injection contract

Each frontend constructs `TranslationLibrary` directly with its own implementations — there is no DI framework or service locator. Current `tui` implementations:

- `JvmLlmEngine` — POSTs to Ollama (`http://localhost:11434/api/generate`, default model `qwen2.5:7b`). Hand-rolled JSON encoding + regex response extraction; no JSON library dependency.
- `JvmCedictSource` — loads `cedict/cedict.txt` via `classLoader.getResourceAsStream`. The file lives at `shared/src/commonMain/resources/cedict/cedict.txt` and reaches the TUI classpath through the `:shared` dependency.
- `HanLpSegmenter` — wraps `com.hankcs:hanlp:portable-1.8.6`.

When wiring Android, expect to write Android-side counterparts (e.g. MediaPipe `LlmEngine`, `AssetManager`-backed `CedictSource`, and an Android-friendly segmenter — HanLP portable's footprint may or may not be acceptable on mobile).

## Build & run

```shell
# TUI — the only working frontend
./gradlew :tui:run --args="我喜欢学习中文"
./gradlew :tui:run --args="--no-llm 你好"     # dictionary-only mode

# Build a runnable script at build/install/tui/bin/jzw
./gradlew :tui:installDist

# Compose Desktop (stub UI)
./gradlew :composeApp:run

# Android APK
./gradlew :composeApp:assembleDebug

# Ktor server (does not currently compile — missing Greeting class)
./gradlew :server:run

# Tests
./gradlew test
./gradlew :shared:test
```

The TUI needs a running Ollama instance unless `--no-llm` is passed; pull the model first (`ollama pull qwen2.5:7b`).

## Versions

Kotlin 2.3.20 · Compose Multiplatform 1.10.3 · Ktor 3.4.1 · HanLP portable 1.8.6 · Mordant 2.7.2 · JVM target 11 · Android `minSdk 24` / `compileSdk 36`.

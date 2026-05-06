# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`zhongwen-helper` is a Kotlin Multiplatform project for Chinese text translation and vocabulary assistance. It targets Android, Desktop (JVM), and a Ktor backend server. The project is early-stage: core data models and interfaces are defined, but the translation pipeline is not yet wired end-to-end.

## Build & Run Commands

```shell
# Run desktop app (with hot reload)
./gradlew :composeApp:run

# Build Android APK
./gradlew :composeApp:assembleDebug

# Run backend server (port 8080)
./gradlew :server:run

# Run all tests
./gradlew test

# Run tests for a single module
./gradlew :shared:test
./gradlew :server:test
```

## Module Architecture

The project has three Gradle modules:

- **`shared`** — business logic shared across all targets (Android + JVM). Contains models, interfaces, and the tokenizer. This is where the core translation logic lives.
- **`composeApp`** — Compose Multiplatform UI for Android and Desktop (JVM). The entry point for Android is `MainActivity`, for Desktop it is `main.kt` → `App()`.
- **`server`** — Ktor server (Netty, port `SERVER_PORT = 8080`). Currently a stub. Depends on `shared`.

## Key Abstractions in `shared`

- **`model/Token.kt`** — `Token(text, lang)` + `Lang` enum (`CHINESE` / `ENGLISH`). Chinese is detected via Unicode range `0x4E00..0x9FFF`.
- **`model/TranslationResult.kt`** — `TranslationResult` (full meaning + per-token breakdown) and `TokenBreakdown` (pinyin, meaning per token).
- **`core/Tokenizer.kt`** — `tokenize(input)` splits mixed Chinese/English text into `Token` segments, splitting on whitespace and script boundaries.
- **`engine/CedictSource.kt`** — `interface CedictSource { fun open(): ByteArray }` — platform-specific way to load the CEDICT dictionary bytes. Not yet implemented.
- **`engine/LlmEngine.kt`** — `interface LlmEngine { suspend fun infer(prompt: String): String }` — abstraction for LLM-based translation. Not yet implemented.

## Platform Actuals Pattern

`shared` uses Kotlin's `expect/actual` pattern. Platform-specific implementations live in:
- `shared/src/androidMain/` — Android implementations
- `shared/src/jvmMain/` — JVM/Desktop implementations

When adding a new `expect` declaration in `commonMain`, provide `actual` implementations in both `androidMain` and `jvmMain`.

## Tech Stack Versions

- Kotlin 2.3.20, Compose Multiplatform 1.10.3, Ktor 3.4.1
- Android: `minSdk 24`, `compileSdk 36`
- JVM target: 11

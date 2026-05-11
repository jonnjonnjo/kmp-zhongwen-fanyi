# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`zhongwen-helper` is a Kotlin Multiplatform **Chinese-learning aid** — not just a translator. It turns mixed 中文/英文 input into a structured `TranslationResult` carrying:

- `chinese` — canonical Chinese rendering of the input
- `english` — English gloss
- `breakdown` — per-token list (token, lang, pinyin, dictionary meaning)

The per-token breakdown is the differentiator; a pure translator wouldn't bother showing pinyin and per-word meanings. The core logic lives in `shared` and is reused across frontends (terminal, Compose Desktop/Android, Ktor server) through a small dependency-injection contract.

`README.md` is the spec for translation behavior across the three input shapes (中文-only / 英文-only / mixed) and when AI is required vs. dictionary-only. Treat it as authoritative when changing the pipeline.

## Modules

Four Gradle modules:

- **`shared`** — KMP library (`androidTarget` + `jvm`). All translation logic lives here in `commonMain`. No third-party deps. Intentionally platform-free: all I/O flows through injected interfaces, not `expect/actual`.
- **`tui`** — Plain JVM module (`kotlinJvm`, not KMP). The only currently-working frontend: a Mordant terminal app. Provides the JVM implementations of `LlmEngine`, `CedictSource`, `Segmenter`. JVM-only because HanLP is JVM-only.
- **`composeApp`** — Compose Multiplatform UI for Android + Desktop. Currently a stub (`App()` renders one `Text`); not yet wired to `TranslationLibrary`. Note: `composeApp/src/jvmMain/.../main.kt` is mid-refactor and imports types from packages they don't live in — it will not compile until those imports are fixed or the JVM implementations are moved into `shared`/`composeApp`.
- **`server`** — Ktor (Netty, port `SERVER_PORT = 8080`). Stub. References a `Greeting()` class that doesn't exist anywhere in the repo, so it won't compile as-is.

## Translation pipeline

`shared/.../core/TranslationLibrary.kt` is the entry point. Its constructor takes three abstractions:

- `LlmEngine` — `suspend infer(prompt: String): String` (nullable; `translate()` degrades gracefully without it)
- `CedictSource` — `open(): ByteArray` returning a CEDICT-formatted file
- `Segmenter` — `segment(text: String): List<String>` for breaking continuous Chinese into words

It exposes two public functions:

- `suspend fun translate(input)` — AI-routed; fills both `chinese` and `english` when LLM is available.
- `fun translateDictionaryOnly(input)` — pure CEDICT, not `suspend`. Sets only the field that matches the input language; leaves the other `null`.

`translate(input)` flow — minimum LLM calls per input shape:

| Input shape | `chinese` source | `english` source | LLM calls |
|---|---|---|---|
| Pure 中文     | `= input`            | LLM (中文→英文)              | 1 |
| Pure 英文     | LLM (英文→中文)       | `= input`                  | 1 |
| Mixed        | LLM (mixed→中文)      | LLM (中文→英文 on `chinese`) | 2 |

Steps inside `translate`:
1. `tokenize()` (in `core/Tokenizer.kt`) splits on whitespace and script boundary — Chinese block `0x4E00..0x9FFF` vs everything else → `Lang.ENGLISH`.
2. Decide `chinese` and `english` per the table above (using LLM only when needed). If LLM is unavailable, the missing field stays `null`.
3. Build the breakdown from `chinese ?: input`: re-tokenize, segment each Chinese run via `Segmenter`, look up each token in `CedictDictionary` (`core/CedictLookup.kt`), produce `TokenBreakdown(token, lang, pinyin, meaning)`. Tokens with no CEDICT entry echo back with `pinyin = null` and `meaning = tokenText`.

`core/PinyinConverter.kt`:
- `numericalToTone("ni3 hao3")` → `"nǐ hǎo"` (CEDICT stores numeric tones).
- `convertPinyinInMeaning(...)` rewrites embedded `[xxx]` pinyin inside meaning strings, since CEDICT glosses often inline pinyin like `/likes [xi3 huan1]/`.

There is no separate routing layer — the LLM-vs-dictionary decision is implicit in the nullable `llmEngine` and the input's language composition.

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

# Build a runnable script at tui/build/install/jzw/bin/jzw
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

## Configuration & packaging roadmap

Long-term goal: ship `jzw` to nixpkgs first, then other distros (AUR, Homebrew, etc).

**Causality:** the NixOS module is a *thin wrapper* — it just renders config (env vars or a file) that the app reads. So the app must accept external config before any distro packaging can be useful. Work order:

1. **CLI flags** — `--model`, `--ollama-url` (and eventually `--help`, `--version`). One-shot overrides, never persisted.
2. **Env vars** — `JZW_MODEL`, `JZW_OLLAMA_URL`. CLI flags take precedence over env vars.
3. **(Optional) Config file** — `$XDG_CONFIG_HOME/jzw/config.toml`. Lower precedence than env vars.
4. **NixOS module** — `programs.jzw = { model = ...; ollamaUrl = ...; }` that maps Nix options to step-2 env vars (or step-3 config file).
5. **Other distros** — `gradle :tui:installDist` + launcher script; basically free once steps 1–4 are settled.

Convention (don't deviate): CLI flags = temporary, config file = permanent, env vars = session-scoped permanent. Same as `git`, `kubectl`, `aws`. `jzw --model foo` runs once with `foo`; persisting means editing the config file or shell rc.

## Versions

Kotlin 2.3.20 · Compose Multiplatform 1.10.3 · Ktor 3.4.1 · HanLP portable 1.8.6 · Mordant 2.7.2 · JVM target 11 · Android `minSdk 24` / `compileSdk 36`.

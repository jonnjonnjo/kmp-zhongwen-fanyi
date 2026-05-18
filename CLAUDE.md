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

- `OpenAiCompatibleLlmEngine` — POSTs to any OpenAI-compatible `/chat/completions` endpoint (OpenAI, OpenRouter, Groq, DeepSeek, Together, Fireworks, Mistral, etc.). Single user-message request. There is no local-LLM mode — for offline use, pass `--no-llm` and run dictionary-only.
- `JvmCedictSource` — loads `cedict/cedict.txt` via `classLoader.getResourceAsStream`. The file lives at `shared/src/commonMain/resources/cedict/cedict.txt` and reaches the TUI classpath through the `:shared` dependency.
- `HanLpSegmenter` — wraps `com.hankcs:hanlp:portable-1.8.6`.

When wiring Android, expect to write Android-side counterparts (e.g. an Android HTTP-based `LlmEngine` reusing the OpenAI-compatible shape, `AssetManager`-backed `CedictSource`, and an Android-friendly segmenter — HanLP portable's footprint may or may not be acceptable on mobile).

## Build & run

```shell
# TUI — online mode (requires --base-url, --model, and usually --api-key
# from flags, JZW_* env vars, or ~/.config/jzw/config.toml)
./gradlew :tui:run --args="我喜欢学习中文"

# Offline (dictionary-only) mode — no cloud call, no config needed
./gradlew :tui:run --args="--no-llm 你好"

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

For online mode without configuring `config.toml`, pass everything inline:

```shell
./gradlew :tui:run --args="--base-url https://api.groq.com/openai/v1 --model qwen/qwen3-32b --api-key $JZW_API_KEY 我喜欢学习中文"
```

## Configuration & packaging roadmap

Long-term goal: ship `jzw` via the in-repo flake (consumed by home-manager). Possibly AUR/Homebrew later. No nixpkgs PR planned.

**Causality:** the NixOS module / flake is a *thin wrapper* — it just renders config (env vars or a file) that the app reads. So the app must accept external config before any distro packaging can be useful. Work order:

1. **CLI flags** — `--model`, `--base-url`, `--api-key`, `--no-llm` (and `--help`, `--version`). One-shot overrides, never persisted.
2. **Env vars** — `JZW_MODEL`, `JZW_BASE_URL`, `JZW_API_KEY`. CLI flags take precedence over env vars.
3. **Config file** — `$XDG_CONFIG_HOME/jzw/config.toml`. Lower precedence than env vars. **Never put secrets here** — use the env var for `api-key`.
4. **home-manager / NixOS module** — could map Nix options to step-2 env vars or step-3 config file. API keys should come from `agenix`/`sops-nix`, not be inlined in Nix config (anything in `/nix/store` is world-readable).
5. **Other distros** — `gradle :tui:installDist` + launcher script; basically free once steps 1–3 are settled.

Convention (don't deviate): CLI flags = temporary, config file = permanent, env vars = session-scoped permanent. Same as `git`, `kubectl`, `aws`. `jzw --model foo` runs once with `foo`; persisting means editing the config file or shell rc.

## Versions

Kotlin 2.3.20 · Compose Multiplatform 1.10.3 · Ktor 3.4.1 · HanLP portable 1.8.6 · Mordant 2.7.2 · JVM target 11 · Android `minSdk 24` / `compileSdk 36`.

# Documentation 

## Core Module 
So, the core module would generally accept a string (could be in 中文/英文/mixed) and then do this 

1. Find the meaning of the string 
2. Find the full 中文 of the string 
3. For each 汉字 character in the translated string, do the semantic meaning of each character. 

### When is AI required?

Step 3 is **always CEDICT** — AI is never used for the per-汉字 breakdown.
AI is only ever used to produce the Chinese string that step 3 then operates on (or to translate 中文 → English for the full meaning).

| Input shape       | Step 1 (full meaning) | Step 2 (full 中文) | Step 3 (per-汉字 breakdown) |
| ----------------- | --------------------- | ------------------ | --------------------------- |
| 中文, 1 token     | n/a (input == output) | n/a                | CEDICT                      |
| 中文, 2+ tokens   | AI                    | n/a                | CEDICT                      |
| 英文              | n/a (input == output) | AI                 | CEDICT (on AI's output)     |
| Mixed             | AI                    | AI                 | CEDICT (on AI's output)     |

Here are several test cases 

1. 中文
    - 1 Token (1 phrase/1 Word) 
        - Require AI : ✘

```
TranslationResult(
    input       = "我",
    fullMeaning = "I",
    breakdown   = [
        TokenBreakdown("我",   CHINESE, "wǒ",       "I / me"),
    ]
)
```
   - 2 Token or more
        - Require AI : Only when finding fullMeaning
```
TranslationResult(
    input       = "我喜欢学习中文",
    fullMeaning = "I like studying Chinese",
    breakdown   = [
        TokenBreakdown("我",   CHINESE, "wǒ",       "I / me"),
        TokenBreakdown("喜欢", CHINESE, "xǐ huān",  "to like / to enjoy"),
        TokenBreakdown("学习", CHINESE, "xué xí",   "to study / to learn"),
        TokenBreakdown("中文", CHINESE, "zhōng wén","Chinese language")
    ]
)
```

2. 英文
    - Require AI : ✔ When translating input to fullMeaning
```
TranslationResult(
    input       = "I like studying Chinese",
    fullMeaning = "我喜欢学习中文",
    breakdown   = [
        TokenBreakdown("我",   CHINESE, "wǒ",       "I / me"),
        TokenBreakdown("喜欢", CHINESE, "xǐ huān",  "to like / to enjoy"),
        TokenBreakdown("学习", CHINESE, "xué xí",   "to study / to learn"),
        TokenBreakdown("中文", CHINESE, "zhōng wén","Chinese language")
    ]
)
```
3. Mixed
    - Require AI : ✔ When translating input to fullMeaning
```
TranslationResult(
    input       = "我like学习中文",
    fullMeaning = "我喜欢学习中文",
    breakdown   = [
        TokenBreakdown("我",   CHINESE, "wǒ",       "I / me"),
        TokenBreakdown("喜欢", CHINESE, "xǐ huān",  "to like / to enjoy"),
        TokenBreakdown("学习", CHINESE, "xué xí",   "to study / to learn"),
        TokenBreakdown("中文", CHINESE, "zhōng wén","Chinese language")
    ]
)
```

Note that there exists 2 different version of **CORE MODULE** that is 1) **Pure** 和2) **Cache** module
where the module stores previous conversation and return the result should the user had computated it previously

## TUI 
1. Add helpful flags such as --version or --help 
2. Add the total elapsed time during the translation 

### Modes

`jzw` runs in one of two modes:

- **Offline** (`--no-llm`) — CEDICT-only. Per-token pinyin & meanings for any
  Chinese input. No translation of full sentences. No network required.
- **Online** (default) — calls any OpenAI-compatible `/chat/completions`
  endpoint (OpenAI, OpenRouter, Groq, DeepSeek, Together, Fireworks, Mistral, ...)
  for full-sentence translation, then runs the CEDICT breakdown on top.

There's no local-LLM mode — for fast inference, point at a hosted endpoint
(Groq's free tier handles personal use; DeepSeek is dirt-cheap for higher
quality on Chinese specifically).

Precedence: **CLI flags > env vars > config file.**

### CLI flags

| Flag             | Env var           | Config key     | Notes                                       |
| ---------------- | ----------------- | -------------- | ------------------------------------------- |
| `--no-llm`       | —                 | —              | Offline mode (CEDICT-only)                  |
| `--model`        | `JZW_MODEL`       | `model`        | Cloud model (e.g. `qwen/qwen3-32b`)         |
| `--base-url`     | `JZW_BASE_URL`    | `base-url`     | OpenAI-compatible base URL                  |
| `--api-key`      | `JZW_API_KEY`     | `api-key`      | Bearer token                                |
| `--cedict-path`  | `JZW_CEDICT_PATH` | `cedict-path`  | External CEDICT file (default: bundled)     |

### Examples

```sh
# Offline (no cloud call, no API key needed)
jzw --no-llm 大家好

# Online — configure once via config.toml, then just:
jzw 大家好

# Or override per-call:
jzw --base-url https://api.deepseek.com/v1 \
    --api-key $DEEPSEEK_API_KEY \
    --model deepseek-chat \
    大家好
```

### Config file

`$XDG_CONFIG_HOME/jzw/config.toml` (defaults to `~/.config/jzw/config.toml`):

```toml
base-url = "https://api.groq.com/openai/v1"
model    = "qwen/qwen3-32b"
# api-key  = "..."        # safer: leave unset, set JZW_API_KEY in env instead
```

Convention: CLI flags for one-shot overrides, env vars for session-scoped
secrets (recommended for `api-key`), config file for stable defaults.

### Picking a provider

| Provider     | Base URL                              | Notes                                         |
| ------------ | ------------------------------------- | --------------------------------------------- |
| **Groq**     | `https://api.groq.com/openai/v1`      | Free tier; sub-second response; try `qwen/qwen3-32b` |
| **DeepSeek** | `https://api.deepseek.com/v1`         | Best Chinese quality per dollar (~$0.27/M in) |
| **OpenRouter** | `https://openrouter.ai/api/v1`      | One key, ~300 models incl. Claude/GPT-4o      |
| **OpenAI**   | `https://api.openai.com/v1`           | Default model: `gpt-4o-mini`                  |


## Android 
1. Later

##  Web 
1. Later

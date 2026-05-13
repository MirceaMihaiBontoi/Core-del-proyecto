# TTS Package

## Responsibility

Implements the `core.port.TTS` port using Sherpa-ONNX and the Kokoro-82M
multilingual model. Converts LLM text output to speech with natural prosody,
streaming support, and safe handling of multilingual Unicode input.

## Structure

```
tts/
├── SherpaTTSService.java       # TTS port implementation — public API, thread orchestration
├── ProsodicLookaheadBuffer.java # Prosodic boundary detection + lookahead context
├── TTSAudioPlayer.java         # SourceDataLine management + PCM processing
├── TTSModelManager.java        # Kokoro engine lifecycle + language rebuild logic
├── TTSLogger.java              # File-based diagnostic logger (logs/voice/tts.log)
└── TtsTextSanitizer.java       # Unicode sanitizer — prevents native ONNX crashes
```

## Classes

### `SherpaTTSService`

The only public-facing class. Implements `TTS` and `AutoCloseable`.

Runs three daemon threads:

| Thread | Role |
|---|---|
| `TTS-Warmup` | Synthesizes a silent phrase at startup to pre-load ONNX weights |
| `TTS-BufferFlush` | Flushes the prosodic lookahead buffer after a 100 ms idle timeout |
| `TTS-Synthesis` | Drains the utterance queue and calls the native engine |

Thread initialization is modularized into `startWarmupThread()`, `startBufferFlushThread()`,
and `startSynthesisThread()` for clarity.

The synthesis pipeline is decomposed into:
- `extractActualContent()` — splits lookahead from actual chunk
- `generateAudioWithLock()` — thread-safe Kokoro invocation
- `truncateAudioIfNeeded()` — removes lookahead audio
- `applyAudioEffects()` — fade-in/fade-out pipeline

`ttsNativeLock` serializes all calls into the native engine because `setLanguage`
and `generate` share mutable engine state and are called from different threads.

### `ProsodicLookaheadBuffer`

Package-private class. Accumulates streaming LLM tokens until a sentence boundary
(`.!?` or `,`) is detected, then emits a `ChunkToSynthesize` record containing:
- `textWithLookahead` — the actual chunk + next 8 words for Kokoro context
- `actualWordCount` — how many words belong to the actual chunk (for audio truncation)

Kokoro uses the lookahead context to plan intonation. The generated audio is
truncated to the actual chunk length before playback.

A timeout-based flush mechanism (external to this class) calls `flush()` after
100 ms of inactivity to prevent indefinite buffering when the LLM stops mid-sentence.

### `TTSAudioPlayer`

Owns a single persistent `SourceDataLine` opened once for the lifetime of the
service. Reopening the line per utterance caused audible gaps on Windows.

Applies a 30 ms crossfade between consecutive PCM chunks to eliminate clicks
at chunk boundaries caused by phase discontinuities.

### `TTSModelManager`

Kokoro applies `setLang` at engine-build time, not per-call. If the language
changes after the initial build, the entire `OfflineTts` instance must be
replaced. `ensureEngineLanguage()` performs this check before every synthesis
call and rebuilds only when the resolved ISO code differs from the loaded one.

### `TTSLogger`

Writes to `logs/voice/tts.log`, truncated on each startup. Exists because
stdout is not useful in a desktop JavaFX application and the TTS pipeline
generates high-frequency diagnostic events that would flood the JUL log.

### `TtsTextSanitizer`

Package-private utility. Filters text before it reaches Kokoro to prevent
native C++ exceptions on mixed-script Unicode. Three modes:

| Mode | When active | What passes |
|---|---|---|
| `PERMISSIVE` | Latin-script languages | Everything except control chars |
| `JA` | `ja` language hint | Kana, Han, basic ASCII Latin |
| `ZH_HANZI_ONLY` | `zh` hint **or** text contains Han | Han only + digits + punctuation |

`ZH_HANZI_ONLY` activates on Han content regardless of the language hint because
the engine crashes on mixed garbage even when the hint is wrong.

## How the pieces connect

```
ChatController / LocalBrainService
    │
    └─ speakQueued(token)
           │
    SherpaTTSService
           │
    ProsodicLookaheadBuffer.append()  ──→  ChunkToSynthesize
           │
    TtsTextSanitizer.sanitize()
           │
    TTSModelManager.generate()   ←── TTSModelManager.ensureEngineLanguage()
           │
    TTSAudioPlayer.enqueue(pcm)
           │
    SourceDataLine (TTS-Playback thread)
```

## Design decisions

**Why a prosodic lookahead buffer instead of synthesizing each token?**
Kokoro's intonation depends on seeing complete phrases. Synthesizing individual
LLM tokens produces flat, robotic speech. Buffering to sentence boundaries and
including future context gives the engine enough information to generate natural
prosody at the cost of a small latency increase.

**Why a persistent audio line?**
Opening `SourceDataLine` per utterance introduced 50–200 ms gaps between
sentences on Windows due to driver initialization overhead. A single persistent
line eliminates those gaps.

**Why crossfade between chunks?**
PCM chunks from consecutive synthesis calls rarely end and start at zero
amplitude. Without crossfading, the discontinuity produces an audible click.
A 30 ms linear crossfade is imperceptible but eliminates the artifact.

## See also

- Streaming tokens usually originate from LLM output: `../llm/_llm.spec.md`
- Intelligence overview: `../_intelligence.spec.md`
- Speakable sentence chunks from the app layer: `../../../application/chat/_chat.spec.md` (`SentenceSplitter` → TTS)

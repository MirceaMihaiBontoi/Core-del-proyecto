# STT Package — Specification

**Location:** `infrastructure/intelligence/stt`
**Port implemented:** `com.soteria.core.port.STT`

---

## Responsibility

Fully offline speech-to-text pipeline. Captures microphone audio, pre-processes it,
gates speech segments with Silero VAD, and transcribes them with Whisper via sherpa-onnx.
No network calls are made at runtime.

---

## Structure

```
stt/
├── SherpaSTTService.java        # Public STT port implementation
├── SherpaOnnxConfigurator.java  # Package-private factory for sherpa-onnx components
└── SherpaSTTVoiceLogWriter.java # Append-only per-session trace (logs/voice/stt.log)
```

---

## Classes

### `SherpaSTTService` — public, implements `STT`, `AutoCloseable`

The only public class in the package. Callers interact exclusively via the `STT` port.

#### Threading model

Two daemon threads run concurrently, communicating through a **bounded** `audioQueue`
(`LinkedBlockingQueue`, capacity 100 frames ≈ 3.2 s at 16 kHz):

| Thread | Role |
|---|---|
| `stt-worker-capture` | Mic → AGC normalizer → PCM float conversion → `AudioPreProcessor` → `audioQueue` |
| `stt-worker-processing` | `audioQueue` → Silero VAD → Whisper decode → `STTListener` callbacks |

#### Session epoch

Every listening session carries a **monotonic epoch** (`AtomicLong sttEpoch`). It is
incremented on `stopListening()`, on restart, and on `close()`. Any transcription
result whose captured epoch no longer matches `sttEpoch.get()` is silently discarded,
preventing stale output from reaching a new or closed session's listener.

#### Partial throttle

Partial decodes are emitted **at most once every 1.2 s** and only when ≥ 9 600 samples
(≈ 0.6 s at 16 kHz) have accumulated. This ensures Whisper has enough context to avoid
common hallucinations (`[`, `]`, isolated punctuation) while keeping CPU usage bounded.

#### Contextual VAD

Raw Silero output is passed through `ContextualVAD`, which requires a **2-of-3**
consecutive-frame majority before marking audio as speech. This eliminates most
single-frame false positives (keyboard clicks, door slams) without adding perceptible
latency.

#### Spurious token filter

Inline checks in `isSpuriousToken`: single non-alphanumeric characters are suppressed,
along with known Whisper artifacts (`[`, `]`, `(`, `)`, `...`). Applied inside
`transcribeAndReport` before delivering either partials or finals to `STTListener`.

#### Warmup and Prime

To eliminate the "first-utterance lag" (latency spike on the first decode), the service
performs a **background warmup** immediately after construction:

1.  A `stt-warmup` daemon thread is spawned.
2.  It feeds a 1-second silent buffer to the Whisper engine.
3.  `warmupComplete` is set to `true` upon success (queryable via `isWarmupComplete()`).
4.  Native calls are serialized via `sttNativeLock` to ensure safety if a real session
    starts before the warmup finishes.

---

### `SherpaOnnxConfigurator` — package-private, non-instantiable

Static factory. All returned objects are **caller-owned** — callers must call
`release()` when done.

| Method | Returns | Notes |
|---|---|---|
| `createWhisperRecognizer(path, lang, useBeamSearch)` | `OfflineRecognizer` | `useBeamSearch` reserved — sherpa-onnx ignores it for Whisper (always `greedy_search`) |
| `createWhisperRecognizer(path, lang)` | `OfflineRecognizer` | Compatibility overload, delegates to the 3-arg variant |
| `createSileroVad(modelManager)` | `Vad` | Thresholds and durations sourced from `ModelManager` |
| `findFileBySuffix(dir, suffixes...)` | `Path` or `null` | Suffixes checked in order; first match wins (e.g. `.int8.onnx` before `.onnx`) |

Model file lookup: `findFileBySuffix` tries suffixes in declaration order, so quantized
(`.int8.onnx`) variants are automatically preferred over full-precision ones.

---

### `SherpaSTTVoiceLogWriter` — package-private

Append-only trace file for all partial and final transcriptions. Truncated at session
start (`setup()`). Write failures are swallowed at `FINE` level to avoid noise in
production logs.

Uses `SherpaSTTService.class.getName()` as logger name so the entire package shares a
single logger — one filter rule controls all STT diagnostic output.

---

## Data flow

```
Microphone (TargetDataLine)
  ↓  [stt-worker-capture]
AudioNormalizer (AGC)
float[] conversion  (PCM 16-bit signed → [-1.0, 1.0])
AudioPreProcessor   (noise gate → compression → high-pass → pre-emphasis)
  ↓  audioQueue  (LinkedBlockingQueue, cap=100)
  ↓  [stt-worker-processing]
Silero VAD  (createSileroVad)
ContextualVAD (2-of-3 majority)
AudioPreProcessor.hasVoiceEnergy()
  ├─ speech active ──→ handleActiveSpeech ──→ transcribeAndReport(partial, throttled)
  └─ segment done  ──→ processCompletedSegments ──→ transcribeAndReport(final)
                                                    Whisper OfflineRecognizer
                                                    isSpuriousToken filter
                                                       ↓              ↓
                                              onPartialResult    onResult
```

---

## Session lifecycle

```
new SherpaSTTService(path, lang, modelManager)
    validates recognizer + probe VAD; builds fixed-2-thread pool;
    starts background `stt-warmup` thread.

startListening(listener)
    increments epoch, submits capture + processing workers

stopListening()
    increments epoch; workers finish the current frame and exit naturally

close()
    stopListening + shutdownNow + recognizer.release()
    ⚠ Do not call startListening() after close() — the pool is terminated
```

Calling `startListening()` while already listening **tears down the previous session**
(150 ms drain wait) before starting the new one. A concurrent restart during the drain
window cancels the new call.

---

## Design decisions

### Why 9 600 samples minimum for partials?
At 16 kHz, 9 600 samples ≈ 0.6 s. Whisper generates hallucination tokens when fed
less than ~0.3–0.5 s of audio. The 0.6 s threshold provides a conservative margin.

### Why Contextual VAD (2-of-3) instead of raw Silero?
Silero occasionally fires on transient noise. Requiring a majority across 3 consecutive
frames eliminates most single-frame false positives without meaningful latency impact.

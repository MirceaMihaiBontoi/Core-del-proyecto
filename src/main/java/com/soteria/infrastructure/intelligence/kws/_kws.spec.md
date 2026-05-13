# KWS Package — Specification

**Location:** `infrastructure/intelligence/kws`

---

## Responsibility

Continuously monitors microphone audio and detects the "SoterIA" wake word using an
offline RNN-T transducer (sherpa-onnx `KeywordSpotter`). Invokes a registered
`Runnable` callback on each confirmed detection. No network calls at runtime.

---

## Structure

```
kws/
└── WakeWordService.java   # Public wake-word service (no port interface)
```

---

## `WakeWordService` — public, implements `AutoCloseable`

### Model: online RNN-T transducer

The `KeywordSpotter` is backed by three ONNX files loaded from the model directory:

| File | Role |
|---|---|
| `encoder-epoch-13-avg-2-chunk-16-left-64.onnx` | Encodes audio in chunks of 16 frames with 64-frame left context |
| `decoder-epoch-13-avg-2-chunk-16-left-64.onnx` | Predicts next token given encoder state |
| `joiner-epoch-13-avg-2-chunk-16-left-64.onnx` | Combines encoder + decoder output distributions |
| `tokens.txt` | Token vocabulary |

Configuration: `numThreads=1`, `maxActivePaths=10`, `keywordsScore=2.0`,
`keywordsThreshold=0.01`.

### Keywords file

`keywords_raw.txt` is **overwritten at every startup** with a fixed set of phonetic
variants of "SoterIA". Each variant has an individual activation threshold (`@score`):

| Variant | Format | Threshold | Rationale |
|---|---|---|---|
| `S OW1 T EH1 R IY0 AH0` | ARPAbet phonemes | 0.015 | Precise English IPA transcription |
| `s o t e r i a` | Character-level | 0.015 | Multilingual coverage |
| `s o t e l i a`, `z o t e r i a` … | Misspelling variants | 0.02–0.03 | Rotacism, ceceo, accent drift |
| `s o t e r` | Truncated | 0.04 | User cuts the word short |

Higher threshold = harder to fire. Variants that are phonetically further from the
canonical word use higher thresholds to limit false positives.

> **Note:** The keyword file must not contain comments (`#`). The native parser
> crashes silently if comments are present.

### Threading model

A single daemon thread (unnamed, managed by `newSingleThreadExecutor`) runs the
capture loop. `startListening` is idempotent: subsequent calls only swap the
`AtomicReference<Runnable>` callback — no second thread is spawned.

### Audio capture loop with exponential backoff

```
while (listening && retryCount < 10)
  ├─ AudioUtils.getResilientMic(16 kHz, 16-bit mono)
  │
  ├─ success → reset retryCount = 0
  │   read 3 200 bytes per iteration  (= 100 ms of audio)
  │   AudioNormalizer.normalize()     (AGC)
  │   convertToFloat()                (PCM 16-bit → [-1.0, 1.0])
  │   stream.acceptWaveform()
  │   while (isReady) → decode()
  │   checkKeywordResult()
  │   every 20 frames (~2 s) → logVoice(gain, maxAmp)
  │
  └─ LineUnavailableException → retryCount++
       backoff = min(1000 × 2^retryCount, 30 000) ms
```

After 10 failed attempts the loop exits and wake-word detection is disabled for the
session.

### Keyword detection and stream reset

`checkKeywordResult()` is called after each decode cycle:

1. Logs the current hypothesis tokens (all frames, even non-hits) to `kws.log`.
2. If `result.getKeyword()` is non-empty: logs the hit, calls
   `activeListener.get().run()`, then **`spotter.reset(stream)`**.

The reset is mandatory — without it the transductor accumulates context from the
previous hit and may re-fire on the same phonemes in subsequent frames.

### Callback contract

The callback (`Runnable`) is invoked **on the audio capture thread**. It must return
quickly; any blocking work (e.g. starting STT) must be dispatched to another thread
by the caller.

The callback reference is updated atomically via `AtomicReference`. A new callback
registered while the loop is running takes effect on the **next** hit.

---

## Shutdown sequence

`shutdown()` (also called by `close()`):

```
1. stopListening()        → listening = false; line.stop() + line.flush()
2. executor.shutdownNow() → interrupts the capture thread
3. shutdownLatch.await(2 s) → waits for the loop to confirm exit
4. spotter.release()      → releases native KeywordSpotter memory
```

The `CountDownLatch` prevents releasing the native `spotter` while the capture thread
is still inside `checkKeywordResult()`.

---

## Logging

| Destination | Content |
|---|---|
| `logs/voice/kws.log` | Hypothesis tokens every frame + `>>> !!! KWS TRIGGERED … !!!` on hits |
| JUL logger (`WARNING`) | Audio device unavailability with retry count |
| JUL logger (`INFO`) | Hit notification (same message as kws.log) |

`kws.log` is truncated at startup with a timestamp header. Write failures are swallowed
at `FINE` level.

---

## Lifecycle

```
new WakeWordService(modelPath)
    writes keywords_raw.txt; loads ONNX model; sets up log file
    throws IOException if model or keyword file cannot be read/written

startListening(callback)
    registers callback; starts capture loop if not already running
    idempotent — safe to call multiple times

stopListening()
    sets listening = false; flushes TargetDataLine
    loop exits gracefully after the current frame

shutdown() / close()
    stopListening + shutdownNow + latch wait + spotter.release()
    ⚠ Do not call startListening() after shutdown()
```

---

## Known issues

| Issue | Detail |
|---|---|
| `TargetDataLine line` race | Mutable field written by the capture thread and read by `stopListening()` (any thread). A concurrent restart during device acquisition could see the old reference. |
| Duplicate keyword entry | `s o t e r i a` appears twice (`@0.015` and `@0.01`). Behavior of the native parser on duplicates is unspecified. |
| Synchronous disk writes | `logVoice()` writes to disk on the audio thread every ~2 s and on every hit. May introduce jitter on slow storage. |

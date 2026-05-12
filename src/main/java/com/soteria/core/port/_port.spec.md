# Port Layer

## What is a port?

A port is an interface defined in the core that describes what the system needs from the outside world. Implementations always live in `infrastructure` — the core never depends on them directly.

This keeps the dependency arrow pointing inward: infrastructure depends on core, never the reverse.

## Ports

### `AlertService` — emergency alert dispatch

| Method | Description |
|---|---|
| `send(EmergencyEvent)` | Dispatches the primary alert; returns `false` on failure instead of throwing |
| `notifyContacts(UserData, EmergencyEvent)` | Notifies the user's emergency contacts |
| `getAlertType()` | Returns a label for the alert channel (used for logging/UI feedback) |

Implementation: `infrastructure.notification.NotificationAlertService`

---

### `Brain` + `BrainCallback` — local LLM

`Brain` is the main LLM port. Inference is asynchronous — results arrive via `BrainCallback`.

| Method | Description |
|---|---|
| `chat(history, context, profile, language, callback)` | Starts an inference turn with full conversation history and RAG context |
| `cancel()` | Interrupts an in-progress inference |

`BrainCallback` events:

| Event | When |
|---|---|
| `onPartialResponse` | Each streaming chunk, for live UI updates |
| `onFinalResponse` | Full text when generation is complete |
| `onStatusUpdate` | LLM reported a protocol state change |
| `onCommand` | LLM emitted a structured command (e.g. switch protocol, trigger alert) |

Implementation: `infrastructure.intelligence.LocalBrainService`

---

### `KnowledgeBase` — protocol retrieval

The "R" in the RAG pipeline. Retrieves protocols relevant to a query.

| Method | Description |
|---|---|
| `findProtocols(query, rejectedIds, searchPrinciplesOnly)` | Ranked retrieval; excludes already-rejected protocols; optionally restricts to principle/guide protocols |
| `getProtocolById(id)` | Direct lookup by protocol ID |

`ProtocolMatch` wraps the result with the protocol, retrieval source (`"lucene"`, `"graph"`, etc.) and relevance score.

Implementation: `infrastructure.intelligence.MedicalKnowledgeBase`

---

### `Triage` — emergency classification

Classifies free text into an emergency type and selects the best matching protocol from a pre-retrieved candidate list (separation of retrieval and classification).

`Intent` values: `MEDICAL_EMERGENCY`, `SECURITY_EMERGENCY`, `ENVIRONMENTAL_EMERGENCY`, `TRAFFIC_EMERGENCY`, `UNKNOWN`, `INACTIVE`, `GREETING_OR_CASUAL`. The last one prevents casual conversation from triggering protocols.

`TriageResult.isEmergency()` returns `true` only when a protocol matched with score ≥ 0.30.

Implementation: `infrastructure.intelligence.ClassifierService`

---

### Voice ports — `STT`, `STTListener`, `TTS`

**`STT`** — speech-to-text. Transcription results arrive asynchronously via `STTListener`.

`STTListener` callbacks:
- `onResult` — final transcription
- `onPartialResult` — intermediate transcription for live feedback
- `onError` — transcription failure

**`TTS`** — text-to-speech synthesis.

| Method | Purpose |
|---|---|
| `speak(text)` | Basic synthesis |
| `speakQueued(text)` | Queues without interrupting previous audio (for streaming) |
| `speakQueued(text, hint)` | Queued with language hint for text sanitization |
| `setLanguage(lang)` | Switches speaker voice |
| `setSpeechRate(rate)` | Speed control |
| `setVolume(vol)` | Volume control |
| `isSpeaking()` | Checks if synthesis is active |
| `setErrorCallback(cb)` | Registers error callback (default does nothing for backward compatibility) |
| `shutdown()` | Releases resources |

The three `default` methods allow simple implementations to only override `speak()`.

Implementations: `infrastructure.intelligence.SherpaSTTService`, `infrastructure.intelligence.SherpaTTSService`

---

### System ports — `LocationProvider`, `LocalizationService`

**`LocationProvider`** — GPS/location services.

**`LocalizationService`** — i18n for system messages and triage categories. Supports >50 languages.

Implementations: `infrastructure.sensor.SystemGPSLocation`, `infrastructure.localization.LocalizationServiceImpl`

### `InferenceListener` — low-level token streaming

More granular than `BrainCallback`. Used internally by the LLM implementation to separate token streaming from structured header parsing.

The LLM prefixes each response with an `[ANALYSIS]` block containing the matched protocol and emergency status. `onAnalysisComplete` fires when that block is fully parsed, before the conversational response begins.

Implementation: internal to `LocalBrainService`

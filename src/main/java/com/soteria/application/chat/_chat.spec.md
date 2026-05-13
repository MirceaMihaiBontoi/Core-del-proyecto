# Application — Chat pipeline

## Responsibility

Orchestrates **one turn of assisted chat** after the UI persists the user message: contextual query → **RAG** protocol retrieval → **triage** → **`PROTOCOL_MANIFEST`** text for the LLM → **trimmed history** → `Brain.chat`. Streams tokens back through **`Brain.BrainCallback`**, splitting partial text into **TTS-sized sentences** and forwarding **protocol status** to the safety UI.

Depends only on **`core`** ports and domain types — no FXML. UI integration: **`InferenceEngine.UIUpdateListener`** (typically `ChatInferenceUiBridge` in `com.soteria.ui.chat`).

## Structure

```
application/chat/
├── InferenceEngine.java      # End-to-end flow, retry on REJECT, UI gate, cancel
├── RAGContextBuilder.java    # Contextual query, sticky protocol, manifest text
├── HistoryManager.java       # History window + relevance filter for Brain prompt
├── SentenceSplitter.java     # Token stream → sentence chunks for streaming TTS
└── _chat.spec.md             # This document
```

## `InferenceEngine`

| Phase | Behavior |
|---|---|
| **Query** | `RAGContextBuilder.prepareContextualQuery` appends categorized turn text to the current user message. |
| **Retrieve** | `KnowledgeBase.findProtocols` with session rejected IDs. |
| **Triage** | `Triage.classifyDynamic` on query + candidates. |
| **Emergency branch** | If `triage.isEmergency()`, categorize message, set `activeEmergencyId` / lock when a protocol is chosen, `applyStickyContext`. |
| **Manifest** | `buildProtocolManifest` → prompt prefix for Brain. |
| **History** | `HistoryManager.filterRelevantHistory` → short list for `Brain.chat`. |
| **Run** | `brainService.chat(..., BrainCallbackHandler)`. |

### `UIUpdateListener`

| Callback | Typical UI use |
|---|---|
| `onSubtitleUpdate` | Partial/full assistant text in subtitle |
| `onFaceStateChange` | `SoterIAFace` state token (e.g. `SPEAKING`) |
| `onSpeakSentence` | Queue TTS per sentence |
| `onSafetyBoxUpdate` | Protocol id + status for `ChatSafetyProtocolBinder` |
| `onResponseFinalized` | Persist final bubble, idle chain |

### Cancellation and stale work

- **`runInference(..., AtomicLong inferenceGeneration, long correlationId)`** wraps the listener in **`InferenceUiGate`**: if the UI bumps `inferenceGeneration` after cancel, callbacks for the old `correlationId` are ignored.
- **`cancel()`** delegates to `Brain.cancel`.

### `BrainCallbackHandler` (internal)

| Callback | Behavior |
|---|---|
| `onPartialResponse` | Append tokens; subtitle update; `SentenceSplitter.process(…, false, …)` → `onSpeakSentence`; first sentence may flip face to speaking. |
| `onFinalResponse` | If **`REJECT`** left `dirty` and `attempt < 3`, **re-run** full flow with incremented attempt; else flush splitter with `isFinal`, `onResponseFinalized`, fallback Spanish strings if empty body. |
| `onStatusUpdate` | **`REJECT`**: reject active protocol id, clear lock → set `dirty` for retry. Else sync `activeEmergencyId` and forward safety UI. |
| `onCommand` | **`STEP`**: store requested step range in `session.getRequestedStepsMap()` for next manifest. |

## `RAGContextBuilder`

| Method | Role |
|---|---|
| `prepareContextualQuery` | Flattens `session.getCategorizedContext()` values into a prefix, then appends `message`. |
| `applyStickyContext` | If `activeEmergencyId` is set but missing from matches, prepend protocol from `KnowledgeBase.getProtocolById`. |
| `buildProtocolManifest` | Human-readable block starting with `PROTOCOL_MANIFEST:`; empty → greeting or “ask for more info”; per-protocol LOCKED/UNLOCKED and step slice from `REQUESTED_STEPS`. |
| `getEmergencyCategory` | Maps `Triage.Intent` to session category key (MEDICAL, SECURITY, …). |

## `HistoryManager`

| Rule | Detail |
|---|---|
| **Recency** | Always include the last **4** messages (index window). |
| **Relevance** | Include older **user** turns whose content equals `currentQuery` or appears in categorized context. |
| **Pairing** | After a relevant user message, include the following **model** message if present. |
| **Fallback** | If nothing selected, `[ChatMessage.user(currentQuery)]`. |

## `SentenceSplitter`

Streams **incremental** `fullText` from the LLM; emits `SentenceListener.onSentenceReady` when a boundary is found.

| Mechanism | Detail |
|---|---|
| Strong ends | `. ! ? ; :` newline, CJK full-width equivalents, `…` |
| Commas | Aggressive early TTS (lower perceived latency); `softCommaSplit` on long runs without a strong end |
| Length guard | Code-point and word-count thresholds unless `isFinal` (final flush accepts remainder) |

**`reset()`** clears cursor and sentence count (new assistant turn).

## How the pieces connect

```
com.soteria.ui.chat.ChatController
    └── runInference → InferenceEngine
            ├── RAGContextBuilder  ← KnowledgeBase, ChatSession state
            ├── HistoryManager     ← ChatMessage list
            ├── Triage.classifyDynamic
            └── Brain.chat         → BrainCallbackHandler
                    └── SentenceSplitter → UIUpdateListener.onSpeakSentence
```

## Cross-references

| Topic | Where |
|---|---|
| JavaFX chat HUD | `../../ui/chat/_chat.spec.md` |
| Port contracts (Brain, KnowledgeBase, Triage, …) | `../../core/port/_port.spec.md` |
| Core RAG overview | `../../core/_core.spec.md` |
| Infrastructure implementations | `../../infrastructure/_infrastructure.spec.md` |


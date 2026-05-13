# Chat Package

## Responsibility

Post-onboarding **JavaFX HUD**: conversation (`ChatViewManager`), animated face (`SoterIAFace`), mic / wake word, session sidebar (`SessionCoordinator`), **inference** via `InferenceEngine` + `ChatInferenceUiBridge`, **SOS dispatch**, safety protocol panel, and **settings** overlay. Orchestration only; domain rules stay in `core` / `application`.

Helpers in this package isolate STT wiring, outbound dedupe, TTS-idle sequencing, emergency thread, and safety-protocol UI binding.

## Structure

```
chat/
├── ChatController.java           # FXML controller — main HUD
├── ChatInferenceUiBridge.java    # InferenceEngine.UIUpdateListener → FX + safety binder
├── ChatSTTListenerFactory.java   # STT listener: partials, dedupe, wake echo filter
├── ChatEmergencyDispatch.java    # Background alert send + FX callbacks
├── ChatSafetyProtocolBinder.java # Loads protocol text into safetyContainer
├── ChatOutboundDedupe.java       # Rapid duplicate send guard
├── ChatInputGuards.java          # Dedupe normalization, wake-phrase echo test
├── ChatTTSIdleChain.java         # Post-TTS work serialized after speech ends
└── _chat.spec.md                 # This document
```

FXML lives under `src/main/resources` (e.g. `chat-view.fxml`).

## `ChatController` — collaborators

| Area | Main collaborators |
|---|---|
| Transcript, sheet, pills | `ChatViewManager`, `SessionCoordinator` |
| Face states | `SoterIAFace` |
| AI pipeline | `InferenceEngine`, `ChatInferenceUiBridge` (built after bootstrap ready) |
| Voice | `BootstrapService` STT/TTS, `ChatSTTListenerFactory`, `ChatTTSIdleChain` |
| SOS | `LocationProvider`, `AlertService`, `ChatEmergencyDispatch` |
| Safety panel | `ChatSafetyProtocolBinder` (via bridge) |
| Profile / i18n | `ProfileRepository`, `UiLocales`, `OnboardingLanguageCatalog` (settings language list) |

## `ChatController` — notable state

| Symbol | Role |
|---|---|
| `instanceId` | Random log suffix |
| `inferenceGeneration` / `correlationId` | Stale inference callbacks ignored when user interrupts (`InferenceEngine` coordinates) |
| `haltedAssistantOnPartial` | First non-blank STT partial stops TTS/streaming once |
| `outboundDedupe` | Blocks duplicate normalized text within `ChatInputGuards.RAPID_SUBMIT_GUARD_MS` |
| `trackedAiPillKey` / `trackedAiPillDot` | Last pill i18n + dot class; reapplied on locale change (`refreshLiveHudForLocale`) |
| AI pill CSS tokens | `ready`, `warming`, `offline`, `alert` (`STATUS_*`) |

Injected edges: `SystemGPSLocation`, `NotificationAlertService`, `BootstrapService`, `ProfileRepository`.

## `ChatController` — lifecycle

| Phase | Behavior |
|---|---|
| **`initialize()`** (FXML) | `SoterIAFace` in `faceHolder`; `ChatViewManager.UIComponents` + `SessionCoordinator`; preparing pill; **`setInputLocked(true)`**; settings UI setup; `handleNewChat()`. |
| **`init(profile, bootstrap, profiles)`** | Locale from `UiLocales.fromPreferredLanguage`; `applyChatChromeI18n`; sync language, TTS rate, wake toggle (via `settingsSyncFlag`); model combo wiring; welcome message; session list; subtitle follows `bootstrap.statusProperty()` until AI ready; **`wireBootstrapLifecycle`**. |
| **`applyBootstrapResult`** | **Error**: offline pill, offline subtitle, IDLE face, unlock input. **OK**: build `InferenceEngine`, **`ChatInferenceUiBridge.Dependencies`** → bridge; TTS language/rate; wake word if enabled; ready pill; **`setInputLocked(false)`**; `aiAvailable = true`. |

## `ChatController` — user flows

| Flow | Summary |
|---|---|
| **Send text** | Trim; `outboundDedupe.tryAccept`; user bubble; **`processMessage`** → THINKING face; `inferenceEngine.runInference` on **`soteria-inference`** with `inferenceUi` + generation guards. |
| **Voice** | Not ready → warming subtitle. Record: `prepareForInput` + **`beginVoiceCapture`** (`ChatSTTListenerFactory.create`); stop → **`stopRecording`**. Wake word → FX → `prepareForInput` + capture. |
| **Interrupt** | **`interruptOngoingGeneration`**: bump generation, `ttsService.stop()`, `inferenceEngine.cancel()`, `inferenceUi.resetBotStreamState()`. |
| **SOS** | **`handleEmergencyAlert`**: ALERT face + pill; **`ChatEmergencyDispatch.start`**; callbacks restore messages / pill / subtitle. |
| **Sessions** | Sidebar via `sessionCoordinator`; new chat, load, delete → `refreshSessionList`; titles `dd/MM/yyyy HH:mm`. |
| **TTS toggle** | **`toggleTTS`**: flag + icon; stop TTS if off. |

## `ChatController` — i18n / status chrome

| Method | Role |
|---|---|
| **`msg`** | Resolves copy via `localization`, `ResourceBundle`, or raw key fallback. |
| **`setAiStatusI18n`** | Stores `trackedAiPillKey` / dot; calls `viewManager.setAiStatusPill`. |
| **`setInputLocked`** | Enables/disables mic, chat sheet toggle, send, and text field together. |

## `ChatController` — settings overlay

| Concern | Behavior |
|---|---|
| Controls | Theme (dark only), languages `OnboardingLanguageCatalog.SUPPORTED`, model combo, speech rate 0.5–2.0, wake toggle |
| Language | Updates TTS locale, **`persistProfile(true)`**, refreshes chrome + session list |
| Speech rate | Slider + label; release → **`persistProfile(false)`** |
| Model change | Interrupt, lock, warming + loading pill, persist, **`bootstrap.startProvisioning(newModel, language)`** |
| Wake | **`applyWakeWordPreference`** when not recording |
| Close / open | **`closeSettings`** persists; **`openSettingsFromSidebar`** closes history then syncs controls |
| Chrome i18n | **`applyChatChromeI18n`** (brand, sheet/history, sidebar, settings, model cells); **`refreshLiveHudForLocale`** keeps pill/subtitle consistent with face state |

## `ChatController` — i18n / status chrome

| Method | Role |
|---|---|
| **`msg`** | Resolves copy via `localization`, `ResourceBundle`, or raw key fallback. |
| **`setAiStatusI18n`** | Stores `trackedAiPillKey` / dot; calls `viewManager.setAiStatusPill`. |
| **`setInputLocked`** | Enables/disables mic, chat sheet toggle, send, and text field together. |

## How the pieces connect

```
ChatController (FXML)
    ├── ChatViewManager / SessionCoordinator / SoterIAFace
    ├── InferenceEngine ──► ChatInferenceUiBridge(Dependencies)
    │                           └── ChatSafetyProtocolBinder.apply(Request)
    ├── ChatSTTListenerFactory.create(Params) ──► ChatOutboundDedupe
    ├── ChatEmergencyDispatch.start(...)
    └── ChatTTSIdleChain ──► enqueue FX after TTS idle (bridge onResponseFinalized)

BootstrapService
    └── STT, TTS, Brain, Triage, KnowledgeBase, WakeWord, readyProperty
```

## Helper classes (compact)

### `ChatInferenceUiBridge`

`InferenceEngine.UIUpdateListener`. Single constructor: **`ChatInferenceUiBridge.Dependencies`**. FX updates via `Platform.runLater` where needed. **`onSafetyBoxUpdate`** → `ChatSafetyProtocolBinder.Request` + **`apply`**. **`resetBotStreamState`** clears streaming bubble flag.

### `ChatEmergencyDispatch`

Thread **`soteria-alert`**: build `EmergencyEvent` (prefix `EMERGENCY: `, severity 10) → `alertService.send`. Callbacks on FX: success with location, send failure, or exception.

### `ChatInputGuards`

| Member | Role |
|---|---|
| `RAPID_SUBMIT_GUARD_MS` | 450 ms window paired with dedupe key |
| `normalizeForDedupe` | trim, lowercase, collapse whitespace |
| `isWakePhraseEchoTranscript` | Letters-only equals `soteria` |

### `ChatOutboundDedupe`

Synchronized **`tryAccept`**: key = `normalizeForDedupe`; duplicate if same key within guard → log + `false`; else update and `true`. Used from **`handleSendMessage`** and STT **`onResultFx`**.

### `ChatSafetyProtocolBinder`

FX thread only. **`apply(Request)`**: hide panel when no active protocol; else render title + bullet lines; **ACTIVE** sets session emergency id + ALERT face + alert pill. **`Request`** carries panel, `KnowledgeBase`, session, face, protocol id/status, pill applier. (Current **`apply`** does not use `viewManager` from the record; kept for caller contract.)

### `ChatSTTListenerFactory`

**`create(Params)`** → **`MicCaptureSttListener`**. **`onResult`**: empty or wake echo → stop recording; else dedupe → accept → `onTranscriptAccepted` or stop. **`onPartialResult`**: first non-blank may **`interruptOngoingGeneration`** + LISTENING; always updates partial transcript. **`onError`**: FX stop + error subtitle.

### `ChatTTSIdleChain`

**`enqueueAfterSpeechSilence`**: chained futures; after TTS `isSpeaking` loop + short sleep, runs **`Platform.runLater`** work. Consumer: bridge **`onResponseFinalized`** (IDLE face when TTS done).

## Design notes

**Why dedupe on mic and send?** Same utterance can arrive from UI and STT; guard blocks double submit.

**Why idle chain?** Face/state updates after TTS must not race `stop()`; serialize after silence detection.

See also: **`_ui.spec.md`** (app routing), **`_view.spec.md`** (bubbles, sessions, face geometry).

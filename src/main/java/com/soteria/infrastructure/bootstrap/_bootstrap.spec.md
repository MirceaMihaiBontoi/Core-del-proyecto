# Bootstrap Package

## Responsibility

Orchestrates the two-phase startup of SoterIA so that the onboarding screen
doubles as a loading screen. By the time the user finishes entering their
profile, the chat engine is typically ready to respond.

The bootstrap package owns the full lifecycle of every heavy runtime component:
native library loading, hardware detection, model downloads, engine
initialization, and LLM warmup. Nothing outside this package needs to know
the order or timing of those steps.

## Structure

```
bootstrap/
├── BootstrapService.java     # Public facade — the only entry point for the UI
├── BootstrapState.java       # Observable JavaFX properties + synchronization future
└── ProvisioningManager.java  # Background thread that runs the provisioning sequence
```

## Classes

### `BootstrapService`

The singleton facade exposed to `OnboardingController` and `ChatController`.
Holds references to every initialized service and exposes them through their
port interfaces (`Brain`, `KnowledgeBase`, `STT`, `TTS`, `Triage`).

Startup is split into two explicit phases so the UI can show meaningful
progress from the moment the application opens:

| Phase | Method | Trigger | What it does |
|---|---|---|---|
| 1 | `preInitialize()` | App launch | Native libs, hardware detection, Lucene/JGraphT index |
| 2 | `startProvisioning(profile, language)` | User confirms onboarding | Model downloads, engine init, LLM warmup |

`shutdown()` calls `System.exit(0)` after closing all services because native
ONNX and llama.cpp threads survive a normal JVM shutdown.

### `BootstrapState`

Holds the three JavaFX properties that the onboarding FXML binds to directly:

| Property | Type | Meaning |
|---|---|---|
| `statusProperty` | `ReadOnlyStringProperty` | Localized status message |
| `progressProperty` | `ReadOnlyDoubleProperty` | Progress in [0.0, 1.0] |
| `readyProperty` | `ReadOnlyBooleanProperty` | Chat UI enabled when `true` |

Also owns the `CompletableFuture<Void>` used internally to gate `ChatController`
on readiness. The future is replaced atomically when the user changes profile
or language mid-onboarding so new waiters block on the updated run.

All property mutations are marshalled to the FX application thread.
When the FX platform is not initialized (unit tests), the task runs on the
calling thread instead — this is the mechanism that makes `BootstrapServiceTest`
work without a real JavaFX environment.

### `ProvisioningManager`

Runs the provisioning sequence on a daemon thread named `soteria-provisioner`.
The sequence is fixed and timed:

```
STT download/load
  → KWS download/load
  → Brain model download
  → Triage download/load
  → TTS download/load
  → Knowledge Base wiring (embedder + centroid)
  → Brain init + silent warmup turn
```

Each step checks `Thread.isInterrupted()` before starting, so a profile change
during onboarding cancels the in-flight run cleanly without leaving
partially-initialized services behind.

Repeated calls with the same `profile|language` key are deduplicated: if
provisioning is already running or has completed successfully, the call is
a no-op.

## How the pieces connect

```
OnboardingController
    │
    ├─ preInitialize()  ──────────→  BootstrapService
    │                                      │
    └─ startProvisioning(profile, lang) ──→ ProvisioningManager (daemon thread)
                                                │
                             ┌──────────────────┼──────────────────┐
                             ▼                  ▼                  ▼
                        ModelManager      SherpaSTT/TTS      LocalBrainService
                        (downloads)       (Sherpa-ONNX)      (llama.cpp JNI)
                             │
                             └──→ BootstrapState (UI properties + future)
                                        │
                                        └──→ ChatController (awaits ready())
```

## Design decisions

**Why two phases?**
`preInitialize()` is fast (index build, no network). Running it at launch means
the onboarding form is never blocked. `startProvisioning()` is slow (model
downloads can take minutes) and depends on the user's profile choice, so it
cannot run earlier.

**Why a silent warmup turn?**
llama.cpp builds the KV cache lazily on the first inference call. Sending a
dummy message during provisioning pre-populates the cache with the system
prompt so the user's first real message gets a response without the cold-start
delay.

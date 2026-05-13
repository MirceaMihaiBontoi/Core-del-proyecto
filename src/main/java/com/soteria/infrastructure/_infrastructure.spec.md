# Infrastructure Layer

## Responsibility

The infrastructure layer is the technical shell of SoterIA. It implements every port defined in `core.port` and owns all interaction with the outside world: the filesystem, native AI engines, device sensors, and alert dispatch.

If the LLM backend, the speech stack, the storage format, or the OS APIs were replaced — only this layer changes. The core never knows.

## Structure

```
infrastructure/
├── bootstrap/       # Application initialization and capability orchestration
├── intelligence/    # Offline AI engines (LLM, STT, TTS, KWS, Triage, Knowledge Base)
├── notification/    # AlertService implementation (logs + simulated dispatch)
├── persistence/     # File-based JSON storage (profile and sessions)
└── sensor/          # Hardware/OS integration (GPS, Phone)
```

See the individual `_spec.md` files in each subpackage for detailed documentation.

## How the pieces connect

### Intelligence Pipeline (AI layer)
The `intelligence` package contains the largest and most complex subsystems, completely offline and running locally:
- **System**: Determines hardware capabilities (`SystemCapability`) and manages native JNI loading.
- **KWS**: Always-on wake word detection using openWakeWord.
- **STT**: Offline speech-to-text transcription via Sherpa-ONNX (Whisper Small).
- **Triage**: Emergency classification and intent detection using Llama embeddings (GGUF).
- **Knowledge**: RAG engine using Apache Lucene (BM25) and JGraphT to fetch and map medical protocols.
- **LLM**: Core reasoning and generation using llama.cpp (Gemma 4 GGUF) via JNI bindings.
- **TTS**: Natural text-to-speech synthesis using Sherpa-ONNX and the Kokoro-82M model.

### Data Persistence
The `persistence` package handles all state. It implements JSON-based file storage using Jackson:
- Profile (`~/.soteria/profile.json`)
- Chat sessions (`~/.soteria/sessions/{id}.json`)
No databases or ORMs are used, ensuring minimal overhead and isolated file corruption (blast radius).

### Sensor & Telemetry Data
The `sensor` package provides real-world context to the system:
- `SystemGPSLocation` invokes OS-level commands (e.g., PowerShell on Windows) or falls back to IP to get location data and primary language hints.
- `DevicePhoneDetector` attempts to extract the device's phone number.
Reads are best-effort: failures return sentinel values so the emergency flow is never halted.

### Notification & Alerts
The `notification` package acts as the emergency exit node. When `Triage` detects a crisis, `NotificationAlertService` intercepts the domain's `EmergencyEvent`, appends the payload to local text logs (`logs/emergency_alerts.log`), and simulates an emergency dispatch (blocking to simulate latency).

### Orchestration
The `bootstrap` package acts as the master coordinator. `BootstrapService` oversees the initial startup sequence: downloading required models, initializing the intelligence engines according to `SystemCapability` (e.g., Lite, Balanced, Expert), loading native libraries, and finally performing a silent LLM warmup turn. It exposes observable properties to the UI.

## Dependency rules

- All packages in `infrastructure` may import from `core` (domain, model, port, exception).
- `infrastructure` packages must **not** import from `ui` or `application`.
- Cross-package imports within `infrastructure` are minimized and strictly logical. `bootstrap` is the single assembly point that binds these services together to pass to the application context.
- The dependency arrow always points inward: `infrastructure` → `core`, never the reverse.

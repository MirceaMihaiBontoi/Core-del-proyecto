# Core Layer

## Responsibility

The core is the heart of SoterIA — contains all business logic, domain concepts, and contracts with the outside world. Zero dependencies on frameworks or infrastructure.

If you swap JavaFX for a web UI, llama.cpp for another LLM, or JSON for SQL — the core doesn't change.

## Structure

```
core/
├── domain/          # Business concepts (chat, protocols, emergencies)
├── model/           # Real-world entities (user, events)
├── port/            # Contracts with infrastructure (interfaces only)
└── exception/       # Domain exceptions
```

See `_domain.spec.md`, `_model.spec.md`, and `_port.spec.md` for details on each package.

## How the pieces connect

### The RAG pipeline

1. User sends a message → `ChatSession` stores it
2. `KnowledgeBase` retrieves relevant `Protocol` candidates (Lucene + graph)
3. `Triage` classifies the input and selects the best protocol
4. `Brain` generates a response using the protocol as context
5. Response arrives via `BrainCallback` → added to `ChatSession`

Application-layer chat turn (`InferenceEngine`, manifest, history trim, sentence splitting for TTS): `../application/chat/_chat.spec.md`.

### Emergency detection and dispatch

1. `Triage` detects an emergency (score ≥ 0.30)
2. System creates an `EmergencyEvent` with the user's `UserData`
3. `AlertService` dispatches the alert and notifies emergency contacts
4. Event is logged for audit trail

### Voice interaction

1. `STT` captures audio → transcribes via `STTListener`
2. Transcription enters the RAG pipeline (same as text input)
3. LLM response is synthesized via `TTS`
4. `TTS.speakQueued()` allows streaming responses token by token

### Localization

`LocalizationService` translates system messages and triage categories to the user's language. The LLM generates responses in the target language directly (cross-lingual reasoning).

## Dependency rules

- `domain` and `model` have zero dependencies — pure Java
- `port` can reference `domain` and `model` (ports need to know what they're transporting)
- `exception` can be used anywhere in core
- **Nothing in core imports from `infrastructure`, `application`, or `ui`**

The dependency arrow always points inward: infrastructure → core, never core → infrastructure.

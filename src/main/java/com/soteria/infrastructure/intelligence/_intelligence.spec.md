# Intelligence Layer

## Responsibility

The intelligence layer contains all AI models, hardware interaction, and inference pipelines for SoterIA. It operates entirely offline, managing real-time audio, text embeddings, and language generation under strict hardware constraints.

## Structure

```text
intelligence/
├── system/          # Hardware limits, native loading, DSP, and model provisioning
├── kws/             # Wake word detection (openWakeWord)
├── stt/             # Local speech-to-text (Whisper)
├── triage/          # Intent classification and semantic matching (Llama Embedding)
├── knowledge/       # RAG, knowledge graphs, and emergency protocols (JGraphT)
├── llm/             # Reasoning and response generation (Gemma 4)
└── tts/             # Local text-to-speech (Kokoro-82M)
```

See `_system.spec.md`, `_kws.spec.md`, `_stt.spec.md`, `_triage.spec.md`, `_knowledge.spec.md`, `_llm.spec.md`, and `_tts.spec.md` for details on each module.

## How the pieces connect

### Standby and Acquisition

1. `system` acquires the microphone and applies DSP (noise gate, AGC, pre-emphasis)
2. `kws` continuously analyzes the cleaned audio stream for the wake word
3. No other ML models run during this phase, preserving battery and CPU

### Voice Interaction

1. `kws` detects the wake word → pipeline transitions to active listening
2. `stt` takes the audio stream, applies `system`'s ContextualVAD to ignore clicks, and transcribes via Whisper
3. Partial and final transcriptions are emitted to the system

### Contextual Retrieval and Triage

1. Final transcription arrives at `knowledge` → performs fast RAG to retrieve candidate protocols
2. Transcribed text and candidates are passed to `triage`
3. `triage` performs semantic similarity (cosine distance) to select the winning emergency intent

### Reasoning and Synthesis

1. `llm` receives the transcription, protocol constraints, and emergency intent
2. `llm` formats the Gemma 4 prompt and generates response tokens
3. `tts` buffers the streaming tokens in a `ProsodicLookaheadBuffer` to synthesize natural speech
4. `system` plays back the synthesized audio, using crossfading to eliminate auditory clicks

## Dependency rules

- `intelligence` must not rely on any cloud API or external network service at runtime (100% offline)
- `stt` and `llm` must respect thread budgets assigned by `SystemCapability` to prevent thermal throttling
- `system` (`NativeLibraryLoader`) must load base C++ binaries before any other module instantiates an engine
- All ML models (Whisper, Gemma, Kokoro) must properly release native resources via `AutoCloseable` to prevent memory leaks

## See also

- Startup wiring of engines: `../../bootstrap/_bootstrap.spec.md`
- Hardware + native loading prelude: `system/_system.spec.md`

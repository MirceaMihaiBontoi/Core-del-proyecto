# LLM (Local Language Model) — Specification

## Purpose

Local LLM inference engine powered by llama.cpp. Provides conversational AI capabilities for emergency dispatch scenarios using Gemma 4 instruction-tuned models. Handles model lifecycle, prompt formatting, streaming inference, and rejection detection.

## Architecture

```
llm/
├── LocalBrainService.java      # Brain port implementation — model lifecycle + streaming orchestration
├── GemmaPromptBuilder.java     # Gemma 4 chat template formatter — system/user/model turns
├── LlamaNativeBootstrap.java   # Native library loader — points to SoterIA's jllama fork
└── LLMLogger.java              # File-based diagnostic logger (logs/ai_conversation.log, logs/raw_llm/)
```

## Component Responsibilities

### `LocalBrainService`

The only public-facing class. Implements `Brain` and `AutoCloseable`.

Orchestrates:
- Model initialization with CPU-optimized parameters
- Streaming token generation with cancellation support
- Rejection detection (REJECT: prefix in first tokens)
- Command parsing (STEP: extraction for protocol navigation)
- Callback delegation to `InferenceListener` and `BrainCallback`

Thread model: Synchronous inference on caller thread. Cancellation via `volatile boolean isCancelled`.

**Key parameters**:
- Context size: 4096 tokens
- Temperature: 0.6 (increased for faster sampling)
- Top-P: 0.95 (wider token selection)
- Repeat penalty: 1.1 (reduced to allow faster repetition)
- Max tokens: 128 per response

### `GemmaPromptBuilder`

Formats prompts for Gemma 4 instruction-tuned models using the official chat template:

```
<start_of_turn>system
[static instructions + conversation history header]
<end_of_turn>
<start_of_turn>user
[message 1]
<end_of_turn>
<start_of_turn>model
[response 1]
<end_of_turn>
...
<start_of_turn>user
[dynamic context + user profile + current message]
<end_of_turn>
<start_of_turn>model
```

**Static instructions** define the emergency dispatcher role, language requirements, brevity constraints, and Chinese script rules.

**Dynamic context** injects:
- User profile (name, medical info)
- Emergency protocol ID and manifest
- Target language reminder

**Special handling**:
- Chinese target language: enforces 汉字 only, no pinyin/romanization
- Parentheses ban: prevents TTS pronunciation artifacts
- Last user turn: appends profile + context + language reminder

Stop sequences: `<end_of_turn>`, `\end_of_turn>` (tokenizer variants)

### `LlamaNativeBootstrap`

Loads the native `jllama` library from `lib/llama/` if not already specified via `-Dde.kherud.llama.lib.path` (**directory** containing the JNI name from `System.mapLibraryName("jllama")`, e.g. `libjllama.so` on Linux).

**Critical**: The native library must be built from SoterIA's tracked fork of java-llama.cpp (see `lib/llama/BUILD.md`). The Maven artifact `de.kherud:llama` provides the Java API; the native must match that fork's llama.cpp version.

File name per OS matches `System.mapLibraryName("jllama")` (e.g. `jllama.dll` on Windows, `libjllama.so` on Linux, `libjllama.dylib` on macOS).

### `LLMLogger`

File-based diagnostic logger. Writes to:
- `logs/ai_conversation.log` — human-readable conversation flow with timestamps
- `logs/raw_llm/llm_input.log` — raw prompts sent to the model
- `logs/raw_llm/llm_output.log` — raw model responses
- `logs/raw_llm/inference_requests.log` — metadata (language, context, history size)

Logs are truncated on service startup (fresh start per session).

## Data Flow

```
ChatController / LocalBrainService.chat()
    │
GemmaPromptBuilder.preparePrompt()
    │
LLMLogger.logRaw("llm_input.log")
    │
LlamaModel.generate() ──→ streaming tokens
    │
LocalBrainService.runInferenceLoop()
    │
    ├─ detectRejection() → REJECT: prefix check
    │       │
    │       └─ listener.onAnalysisComplete("REJECT", reason)
    │
    ├─ listener.onToken(token) → TTS streaming
    │
    └─ parseAndExecuteCommands() → STEP: extraction
            │
            └─ callback.onCommand("STEP", stepId)
```

## Rejection Detection

The model can reject a request by starting its response with `REJECT:` followed by a reason. This is detected in the first ~15 tokens:

1. Buffer first tokens until `REJECT:` is confirmed or ruled out
2. If `REJECT:` detected, suppress token streaming and extract reason
3. Call `listener.onAnalysisComplete("REJECT", reason)`
4. Call `listener.onComplete("")` with empty response

If no rejection, flush buffered tokens and continue streaming.

## Command Parsing

The model can emit protocol navigation commands in its response:

- `STEP:X` — Navigate to step X in the current protocol

Commands are parsed from the final response text and delivered via `callback.onCommand(type, value)`.

## Configuration

Model parameters are hardcoded in `initializeModel()`:
- Context size: 4096 tokens
- Threads: `SystemCapability.getIdealThreadCount()` (CPU core count)
- GPU layers: 0 (force CPU mode)

Inference parameters are hardcoded in `createInferenceParameters()`:
- Temperature: 0.6
- Top-P: 0.95
- Repeat penalty: 1.1
- Max tokens: 128

## Error Handling

- Model load failure → `AIEngineException` with cause
- Inference failure → `listener.onError(throwable)`
- Cancellation → `isCancelled` flag checked per token, callbacks skipped on finalize

## Dependencies

- `de.kherud:llama` — Java API for llama.cpp (Maven artifact)
- `lib/llama/jllama.{dll|dylib|so}` — Native library from SoterIA's fork (see `lib/llama/BUILD.md`)
- `core.port.Brain` — Port interface
- `core.port.InferenceListener` — Streaming callback interface
- `infrastructure.intelligence.system.SystemCapability` — Thread count detection

## Design Rationale

**Why buffer rejection detection?**
The model must be able to refuse inappropriate requests. Detecting `REJECT:` in the first tokens prevents streaming refusal text to TTS, which would be confusing for the user.

**Why Gemma 4 chat template?**
Gemma 4 instruction-tuned models require the official chat template for optimal performance. Deviating from the template degrades response quality.

**Why separate `GemmaPromptBuilder`?**
Prompt formatting is complex and model-specific. Extracting it makes `LocalBrainService` easier to test and allows swapping prompt strategies without touching inference logic.

**Why file-based logging?**
LLM debugging requires inspecting raw prompts and responses. File logs persist across sessions and can be analyzed offline.

## See also

- Thread budgets / hardware: `../system/_system.spec.md`
- Intelligence overview: `../_intelligence.spec.md`
- `Brain` port: `../../../core/port/_port.spec.md`
- Orchestrates `Brain.chat` per turn: `../../../application/chat/_chat.spec.md` (`InferenceEngine`)

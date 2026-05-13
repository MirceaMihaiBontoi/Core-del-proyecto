# Triage Package — Specification

**Location:** `infrastructure/intelligence/triage`
**Port implemented:** `com.soteria.core.port.Triage`

---

## Responsibility

Semantic intent classifier for the emergency triage pipeline. Embeds user input and
protocol candidates as dense vectors using a local GGUF embedding model (llama.cpp),
then selects the best-matching protocol by cosine similarity. No network calls at runtime.

---

## Structure

```
triage/
└── TriageService.java   # Public Triage port implementation
```

Types defined in `com.soteria.core.port.Triage` (shared with callers):

| Type | Kind | Description |
|---|---|---|
| `Triage.Intent` | `enum` | Classifier outcome category |
| `Triage.TriageResult` | `record` | Winning protocol + score + intent |

---

## Classes

### `TriageService` — public, implements `Triage`, `AutoCloseable`

The only public class in the package. Callers interact via the `Triage` port.

#### Embedding model

Backed by a GGUF model loaded via `llama-java` in **embedding mode** (GPU layers = 0,
CPU-only). The model is initialized in the constructor and released in `close()`.
Failure to load throws `IllegalStateException`.

#### Protocol vector cache

Protocol embeddings are computed once per `Protocol.getId()` and stored in a
`ConcurrentHashMap`. This avoids re-embedding the same anchor text on every call.

The cache key is `Protocol.getId()`. The cached vector is always in the current
embedding space (with or without centroid). **`setCentroid()` clears the cache**
before updating the centroid field — entries from the old space are never reused.

#### Centroid (mean-centering)

When a centroid is configured via `setCentroid(float[])`, all vectors (input and
protocol) are **mean-centered and renormalized** before comparison:

```
centered = normalize(raw - centroid)
```

This improves discrimination when protocols cluster semantically close together (e.g.
`TRAUMA_001` vs `TRAUMA_005`). Centroid is `volatile` so visibility is guaranteed
across threads without locking; only full-array replacement is ever performed.

#### Similarity threshold

`SIMILARITY_THRESHOLD = 0.30f`. A match below this floor is treated identically to
no match — the result is `GREETING_OR_CASUAL`, not a protocol activation.

#### Preprocessing

Before embedding, the raw input goes through `preprocess()`:

1. Strips assistant name variants (`soteria`, `sotelia`, `zoteria`, `soteia`) — regex,
   case-insensitive — to avoid semantic bleed from the wake-word into the vector.
2. Collapses whitespace and trims.

Short text after preprocessing triggers an early return to `GREETING_OR_CASUAL`:
- ≥ 1 character for high-density scripts (CJK, Arabic, Hangul, Devanagari, Thai, Hebrew)
- ≥ 3 characters for all other scripts

#### Intent mapping

`mapToIntent` translates `Protocol.getCategory()` → `Triage.Intent`:

| Category (case-insensitive) | Intent |
|---|---|
| `MEDICAL`, `VITAL`, `TRAUMA` | `MEDICAL_EMERGENCY` |
| `SECURITY`, `CRIME`, `THREAT` | `SECURITY_EMERGENCY` |
| `ENVIRONMENTAL`, `FIRE`, `HAZMAT` | `ENVIRONMENTAL_EMERGENCY` |
| `TRAFFIC`, `ACCIDENT` | `TRAFFIC_EMERGENCY` |
| anything else / `null` | `UNKNOWN` |

---

## Port types (`com.soteria.core.port.Triage`)

### `Intent` enum

```
MEDICAL_EMERGENCY
SECURITY_EMERGENCY
ENVIRONMENTAL_EMERGENCY
TRAFFIC_EMERGENCY
UNKNOWN          — unrecognized category or embedding error
INACTIVE         — service not running
GREETING_OR_CASUAL — processable input, no protocol matched
```

### `TriageResult` record

```java
record TriageResult(Protocol protocol, float score, Intent intent)
```

| Field | Meaning |
|---|---|
| `protocol` | Winning `Protocol`, or `null` if no match above threshold |
| `score` | Cosine similarity of the best candidate (0.0 when no candidates) |
| `intent` | Mapped intent category |

`isEmergency()` returns `true` iff `protocol != null && score >= 0.30f`.

---

## Classification flow

```
classifyDynamic(text, candidates)
  │
  ├─ null / blank text ──→ TriageResult(null, 0.0, UNKNOWN)
  │
  ├─ preprocess(text)
  │     strip wake-word variants → collapse whitespace
  │
  ├─ candidates == null / empty ──→ TriageResult(null, 0.0, GREETING_OR_CASUAL)
  │     (RAG found nothing relevant; not an error)
  │
  ├─ script detection (CJK / Arabic / Indic …) → adjust min-length gate
  ├─ processed text too short ──→ TriageResult(null, 0.0, GREETING_OR_CASUAL)
  │
  ├─ model.embed(processedText) → inputVector
  ├─ centroid != null? center(inputVector) : inputVector
  │
  ├─ findBestProtocol(centeredInput, candidates)
  │     for each candidate:
  │       getOrCacheVector(protocol)  →  embed anchor + center
  │       cosineSimilarity(input, protocolVector)
  │     → ProtocolBestMatch(protocol, maxSimilarity)
  │
  ├─ score >= 0.30 ──→ mapToIntent(category) → TriageResult(protocol, score, intent)
  └─ score < 0.30  ──→ TriageResult(null, score, GREETING_OR_CASUAL)
```

---

## Protocol anchor text

Each protocol is embedded using a compound anchor, not just its title:

```
[ROLE] <title> <keywords…>
```

Where `[ROLE]` is `[VIC]` for IDs ending in `_VIC`, `[WIT]` for `_WIT`, and omitted
otherwise. Keywords are joined with spaces. This enriches the embedding with
discriminating signal that short titles alone lack.

---

## Preferred call flow

```
RAG retrieves candidates  →  TriageService.classifyDynamic(text, candidates)
```

`classify(text, allProtocols)` exists as a fallback that passes the full protocol
list. It should be used sparingly: without RAG pre-filtering the candidate set can be
large, increasing both embedding cost and false-positive risk.

---

## Logging

Two append-only log files under `logs/raw_classifier/`:

| File | Content |
|---|---|
| `classifier_input.log` | Raw text + candidate count for every `classifyDynamic` call |
| `classifier_output.log` | Winning intent + score + protocol ID (or threshold rejection) |

Both are **truncated at startup** (`setupClassifierLogging`). Write failures are
swallowed at `FINE` level.

---

## Lifecycle

```
new TriageService(modelFile)
    loads GGUF model in embedding mode; truncates log files
    throws IllegalStateException if the model cannot be loaded

setCentroid(float[])            // optional; clears protocol cache first
classifyDynamic(text, list)     // main call path
classify(text, allProtocols)    // fallback (no RAG pre-filtering)
embed(text)                     // expose embedding for centroid construction etc.
getModel()                      // expose LlamaModel for wiring into KnowledgeBase

close()
    releases the LlamaModel native resources
    ⚠ Do not call classifyDynamic() after close()
```

---

## Design decisions

### Why cosine similarity over cross-encoder reranking?
The candidate list coming from RAG is already small (typically ≤ 20 protocols). A
bi-encoder cosine similarity is O(n) over pre-cached protocol vectors, adding negligible
latency on the critical conversational path. A cross-encoder would need a forward pass
per candidate pair.

### Why mean-centering?
Emergency protocols in the same category are semantically close; raw cosine similarity
struggles to separate them. Subtracting the corpus centroid shifts all vectors away
from the dense region of the embedding space, spreading them and improving rank quality
for within-category disambiguation.

### Why strip the assistant name?
Users frequently prefix queries with the wake word (`"Soteria, hay fuego en la cocina"`).
Including the name in the embedding adds noise that pulls the input vector toward unrelated
documents mentioning safety/security in abstract contexts, degrading recall.

# Knowledge Package — Specification

**Location:** `infrastructure/intelligence/knowledge`

The `knowledge` package provides a hybrid Retrieval-Augmented Generation (RAG) system for emergency protocols. It combines vector similarity search with structural knowledge graph enrichment.

---

## 1. Math & Utilities

### `VectorMath` (Hardware-Accelerated Math)
Stateless utility class providing SIMD-accelerated vector operations using the JDK Vector API (`jdk.incubator.vector.FloatVector`).

*   **SIMD Acceleration:** Processes vectors in hardware-native chunks (`SPECIES_PREFERRED`) with a scalar tail loop for the remaining dimensions. Designed to minimize CPU latency during hot-path RAG cosine similarity calculations.
*   **Vector Operations:** `magnitude`, `normalize`, `subtract`, `dotProduct`, `cosineSimilarity`, and `computeCentroid`.
*   **Edge Cases:** `normalize()` handles near-zero vectors by injecting a tiny epsilon rather than throwing division-by-zero.
*   **Text Processing (`tokenize`):**
    *   Lowercases text.
    *   Decomposes (NFD) and surgically strips combining marks *only* if they follow a Latin base character, preserving structure for Arabic/CJK/Devanagari.
    *   Recomposes to NFC.
    *   Uses `BreakIterator` for multilingual word boundary detection (critical for CJK languages without spaces).

---

## 2. Semantic Engine & Knowledge Graph

### `SemanticEngine` (Unified Intelligence Wrapper)
Wraps the core `soteria-triage-v1` model (via `java-llama.cpp`) to bridge the gap between classification and retrieval.
*   **Unified Semantic Pipeline:** The engine uses the same model that classifies emergencies to generate their embeddings. This ensures that the vector space where protocols are stored is perfectly aligned with the triage logic's internal representation of emergencies.
*   **Configuration:** Loaded in CPU-only mode (`setGpuLayers(-1)`). While it enables embedding generation for RAG, the underlying model identity is shared across the entire intelligence stack. Thread count is dynamically optimized via `SystemCapability`.
*   **Centroid Persistence:** Calculates and saves the global corpus centroid to `centroid.bin` (binary format).
*   **Sanity Checks:** On startup, validates the centroid header to prevent OOM errors or `NegativeArraySizeException` from corrupted sidecars.
*   **Test-Double Support:** Exposes `setTestEmbedder()` to inject mocks during CI/CD to bypass native library dependencies.

### `KnowledgeGraphManager` (JGraphT Adjacency)
Constructs an unweighted, undirected graph of protocol relationships.
*   **Edges:** Drawn between protocols if they:
    1. Share the exact same category.
    2. Explicitly reference each other (one protocol's steps mention the first word of another's title).
*   **CJK-Aware Matching:** The cross-reference logic checks the Unicode blocks of the title word. For Latin scripts, words must be ≥3 characters to trigger a match. For CJK (where a single character has semantic meaning), 1-2 character matches are permitted.

---

## 3. RAG Indexing & Storage

### `LuceneIndexManager` (Vector Database)
Manages the persistent Lucene index for emergency protocols.
*   **Fields:** Stores ID, title, keywords, category, steps, and a dense `KnnFloatVectorField` configured for Cosine similarity.
*   **Mean-Centering at Ingest:** When `indexProtocols()` is called:
    1.  Extracts raw embeddings for all protocols using the `SemanticEngine`.
    2.  Computes the global geometric centroid of the corpus.
    3.  **Subtracts the centroid** from every protocol's embedding and normalizes it.
    4.  Stores the mean-centered vector in Lucene.
    *(This spreads the embeddings apart, improving semantic discrimination within dense clusters).*
*   **Role Anchoring:** Automatically injects role tokens (`[VIC]`, `[WIT]`) into the text representation before embedding, based on the protocol ID suffix.

### `ProtocolRegistry` (JSON Storage)
Thread-safe registry for loading emergency protocols from the classpath.
*   **Manifest Discovery:** Reads an `index.json` file to discover available protocol categories, allowing zero-code expansion.

---

## 4. RAG Retrieval & Orchestration

### `EmergencySearcher` (Hybrid Search Engine)
Executes semantic searches against the Lucene index and enriches results using the Knowledge Graph.
*   **KNN Search:** Queries the top 30 neighbors using a `KnnFloatVectorQuery`.
*   **Score Transformation:** Converts Lucene's internal score `(1 + cos)/2` back to a pure `[-1, 1]` cosine similarity score.
*   **Strict Thresholding:**
    *   `score >= 0.30`: High confidence (`SOURCE_SEMANTIC`). Acts as an "anchor".
    *   `score >= 0.20`: Doubtful candidate (`SOURCE_CANDIDATE`).
    *   `score < 0.20`: Discarded (`DROPPED`).
*   **Knowledge Graph Enrichment (`enrichWithRelated`):**
    If the search yields at least one `SEMANTIC` anchor:
    1.  **Promotion:** Evaluates all `CANDIDATE` matches. If a candidate shares an edge in the Knowledge Graph with *any* anchor, it is promoted to `GRAPH_BOOSTED`.
    2.  **Pruning:** Any remaining `CANDIDATE` that is not connected to an anchor is removed.
    3.  **Expansion:** Forcibly injects all direct graph neighbors of the *top* anchor into the results as `GRAPH_NEIGHBOR` (with a token score of `0.01`).

### `EmergencyKnowledgeBase` (Facade)
The central entry point implementing the `KnowledgeBase` port.
*   **Orchestration:** Wires the `ProtocolRegistry`, `SemanticEngine`, `KnowledgeGraphManager`, `LuceneIndexManager`, and `EmergencySearcher`.
*   **Model Cohesion:** Exposes `setEmbedder(LlamaModel)` to receive the `soteria-triage-v1` instance from the `TriageService`. This centralizes the intelligence core, ensuring that classification and retrieval are two facets of the same semantic model rather than independent processes.
*   **Auto-Recovery:** If the injected embedder detects missing vectors or a corrupted centroid, it triggers an atomic rebuild of the Lucene index.
*   **Diagnostics:** Maintains a dedicated `kb_diagnostics.log` file, gracefully cleaning up `.lck` files from crashed runs.

## See also

- Shared embedder wiring with triage: `../triage/_triage.spec.md`
- Intelligence overview: `../_intelligence.spec.md`
- `KnowledgeBase` port: `../../../core/port/_port.spec.md`
- Caller that builds the RAG manifest from search results: `../../../application/chat/_chat.spec.md` (`RAGContextBuilder`)

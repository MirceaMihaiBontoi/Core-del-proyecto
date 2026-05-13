# System Package — Specification

**Location:** `infrastructure/intelligence/system`

---

## Responsibility

Provides the low-level foundation for the SoterIA application. It handles hardware capability detection, platform-specific native binary loading, asynchronous model provisioning, audio digital signal processing (DSP), and system localization.

---

## Structure

```text
system/
├── PlatformDetector.java               # OS/Architecture discovery
├── SystemCapability.java               # Hardware resource evaluation
├── NativeLibraryLoader.java            # JNI dynamic linker
├── ModelAssets.java                    # Model ecosystem inventory
├── ModelPathResolver.java              # Filesystem router
├── ModelFileDownloader.java            # HTTP transport
├── ModelFileExtractor.java             # TAR.BZ2 archiver
├── ModelManager.java                   # Provisioning facade
├── AudioUtils.java                     # Microphone acquisition
├── AudioPreProcessor.java              # DSP Pipeline
├── AudioNormalizer.java                # Automatic Gain Control
├── ContextualVAD.java                  # Temporal Noise Suppression
├── LanguageUtils.java                  # ISO normalizer
└── ResourceLocalizationService.java    # Fallback-safe String centralizer
```

---

## Classes

### `SystemCapability` — public

Evaluates physical hardware limits to assign the appropriate AI model profile and thread budgets.

#### Robust RAM Detection
Uses a multi-tier fallback strategy to detect physical RAM:
1. `OperatingSystemMXBean` via reflection (avoids `NoClassDefFoundError` on unsupported JVMs).
2. Parsing `/proc/meminfo` (Linux/Android safe).
3. `Runtime.getRuntime().maxMemory()` fallback.

#### Model Profiles
Based on RAM, maps the machine to `LITE` (<6GB), `STABLE` (<12GB), or `EXPERT` (>=12GB).

#### Thermal & Performance Caps
Caps inference thread count (`getIdealThreadCount()`) to leave logical cores free. Forcefully limits threads on low-power devices to prevent thermal throttling.

---

### `NativeLibraryLoader` — public

Manages the manual extraction and linking of `sherpa-onnx` C++ dependencies into the JVM.

#### Strict Dependency Ordering
Forces the loading of `.so` files in a precise dependency chain (`onnxruntime` -> `cxx-api` -> `c-api` -> `jni`) on Linux to prevent `ld.so` from crashing with missing symbol errors (`UnsatisfiedLinkError`).

---

### `PlatformDetector` — public

Identifies the host OS and processor architecture.

#### Architecture Normalization
Resolves varied architecture strings (e.g., `amd64`, `x86_64`) into a unified `x64` identifier.

---

### `ModelManager` — public

The asynchronous facade for the rest of SoterIA. Exposes `CompletableFuture<Path>` endpoints that compose the logic of checking readiness, downloading, extracting, and cleaning up residual tarballs.

---

### `ModelFileDownloader` — package-private

HTTP-based downloader tailored for large binaries.

#### Resume Capability
Uses HTTP `Range: bytes=X-` headers. Interrupted downloads calculate the size of the existing `.part` file and ask the server to resume.

#### Atomic Completion
Writes to a `.part` file and only performs an `ATOMIC_MOVE` operation when the byte stream completes, ensuring the system never loads a corrupt file.

---

### `ModelPathResolver` — package-private

Calculates and validates model paths using cascading priority (`~/.soteria/models/` -> `<repo root>/models/`).

#### Structural Validation
Validating a model (`isSTTModelReady()`) scans the directory to verify required binaries (e.g., `encoder.onnx`, `decoder.onnx`, `tokens.txt`) are present, not just the directory itself.

---

### `ModelFileExtractor` — package-private

Handles `TAR.BZ2` extraction chaining multiple large-capacity (128KB) buffered streams (`BufferedInputStream`) into `commons-compress` decoders to prevent disk I/O starvation.

---

### `ModelAssets` — package-private

Static registry centralizing all URLs, directory names, and configurable constants for Unsloth GGUF models and k2-fsa ONNX releases.

---

### `AudioPreProcessor` — public

A professional-grade acoustic pipeline that mutates PCM frames **in-place** to reduce object allocation.

#### DSP Pipeline
1. **Noise Gate:** Silences frames below `-60dBFS` to eliminate static hum.
2. **Dynamic Compression:** 2:1 soft-knee compressor kicks in above `-18dBFS` to attenuate shouting without distorting the signal.
3. **High-Pass Filter:** First-order IIR filter removes low-frequency rumble (<300Hz).
4. **Pre-Emphasis Filter:** Boosts high frequencies (coeff `0.95`) to enhance intelligibility of consonants/sibilants.

---

### `AudioNormalizer` — public

Automatic Gain Control (AGC) implementation. Dynamically calculates ideal gain to reach a target RMS (`-12dBFS`). Prevents audio "popping" by applying different coefficients for attack and release.

---

### `ContextualVAD` — public

Wraps raw Voice Activity Detection (VAD) algorithms with temporal context logic to suppress false positives.

#### Sliding Window Majority Vote
Uses a deque of recent frame-level decisions (size 3) and requires at least 2 consecutive positive speech frames. Prevents transient clicks from waking up the STT engine.

---

### `AudioUtils` — public

Provides resilient microphone acquisition.

#### Mixer Fallback Strategy
Loops through fallback strategies (default line -> rigorous format matching across all mixers -> heuristic keyword matching) to bypass common Java Sound API locks (e.g., when PulseAudio is exclusively locked).

---

### `LanguageUtils` — public

Bridges the gap between unconstrained language inputs and strict ISO-639-1 requirements.

#### Resolution Chain
Implements a 4-tier fallback matrix (Static fast-map -> BCP 47 parsing -> Java Locale matching -> auto-detection).

#### Engine Auto-Detection Contract
Returns an empty string `""` when a language is unresolvable, signaling the STT engine to rely on dynamic language auto-detection.

---

### `ResourceLocalizationService` — public, implements `LocalizationService`

Centralizes UI and Text-to-Speech (TTS) strings via standard Java `ResourceBundle`.

#### Crash Prevention
Implements a strict safe-fallback contract. If a translation key is missing in the target locale, it degrades to the default locale, and ultimately returns the raw string key rather than throwing a `MissingResourceException`.

---

## Data flow

```
Model Provisioning Flow:
ModelManager.downloadSTTModel()
  ├─ ModelPathResolver (check ~/.soteria/models/)
  │   └─ isSTTModelReady? ──→ Complete(Path)
  │
  ├─ ModelAssets (fetch URLs)
  ├─ ModelFileDownloader.downloadFile(URL, ~/.soteria/models/.part)
  │   └─ Resumes from partial if exists → ATOMIC_MOVE to .tar.bz2
  │
  └─ ModelFileExtractor.extractTarBz2()
      └─ Stream unpack → Delete .tar.bz2 → Complete(Path)
```

---

## Lifecycle

```
NativeLibraryLoader.load()
    extracts libsherpa-onnx-c-api.so + deps to temp dir
    System.load(tempDir/onnxruntime)
    System.load(tempDir/cxx-api)
    System.load(tempDir/c-api)
    System.load(tempDir/jni)
    must happen BEFORE any sherpa-onnx instantiation

ModelManager (Provisioning)
    Stateless beyond executor thread pool. Threads terminated on app exit.
```

---

## Design decisions

### Why ATOMIC_MOVE for model downloads?
Downloading gigabyte-sized GGUF models can take minutes. If the application crashes mid-download and the file is written directly to the target path, the semantic engine will attempt to load a corrupted `.bin` file on the next startup, leading to a fatal native crash. Writing to `.part` and moving atomically guarantees that only 100% complete files exist in the model directory.

### Why absolute paths for `System.load()` instead of `java.library.path`?
Modifying `java.library.path` at runtime via reflection is unsupported in modern JVMs (Java 9+) and highly unstable across OSes. Absolute path resolution ensures the JVM reliably maps to the correct `.so`/`.dll` even in containerized or strictly-sandboxed environments.

### Why in-place array mutation in `AudioPreProcessor`?
The capture thread generates a new 16kHz audio frame every few milliseconds. Allocating new `float[]` arrays for every DSP step (gate, compression, EQ) would flood the Garbage Collector, causing micro-stutters that desynchronize the audio stream. In-place mutation ensures zero allocations in the hot path.

## See also

- Provisioning and `SystemCapability` consumer: `../../bootstrap/_bootstrap.spec.md`
- Voice pipeline overview: `../_intelligence.spec.md`

package com.soteria.infrastructure.intelligence.triage;

import com.soteria.core.domain.emergency.Protocol;
import com.soteria.core.port.Triage;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.ModelParameters;
import com.soteria.infrastructure.intelligence.knowledge.VectorMath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intent classifier for the triage pipeline. Uses a local GGUF model (llama.cpp, embedding mode)
 * to represent input text and protocol candidates as dense vectors, then selects the best match
 * by cosine similarity.
 *
 * <p>The preferred call flow is RAG-first: RAG retrieves a pre-filtered candidate list, which
 * {@link #classifyDynamic} then ranks. {@link #classify} (full protocol list) is a fallback.</p>
 *
 * <p>Protocol embeddings are computed once and cached. When a {@linkplain #setCentroid centroid}
 * is configured, all vectors are mean-centered and renormalized before comparison, improving
 * discrimination across semantically clustered protocol sets.</p>
 */
public class TriageService implements AutoCloseable, Triage {
    private static final Logger logger = Logger.getLogger(TriageService.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String LOG_INPUT = "classifier_input.log";
    private static final String LOG_OUTPUT = "classifier_output.log";
    private static final float SIMILARITY_THRESHOLD = 0.30f;

    private final Path modelFile;
    private LlamaModel model;
    private final Map<String, float[]> protocolVectorCache = new ConcurrentHashMap<>();
    @SuppressWarnings("java:S3077") // volatile on reference is correct: we always assign a new array, never mutate elements in-place
    private volatile float[] centroid;

    public TriageService(Path modelFile) {
        this.modelFile = modelFile;
        initializeModel();
        setupClassifierLogging();
    }

    private void initializeModel() {
        try {
            ModelParameters params = new ModelParameters()
                    .setModel(modelFile.toAbsolutePath().toString())
                    .setGpuLayers(0)
                    .enableEmbedding();
            this.model = new LlamaModel(params);
            logger.info("TriageService: Semantic model initialized successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "TriageService: Failed to initialize semantic model", e);
            throw new IllegalStateException("TriageService: embedding model could not be loaded: " + modelFile, e);
        }
    }

    private void setupClassifierLogging() {
        try {
            Path dir = Paths.get("logs", "raw_classifier");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Files.writeString(dir.resolve(LOG_INPUT),
                    "--- SoterIA Raw Classifier Input (Autocleaned) ---\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(dir.resolve(LOG_OUTPUT),
                    "--- SoterIA Raw Classifier Output (Autocleaned) ---\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            logger.log(Level.WARNING, "TriageService: Failed to initialize logging system", e);
        }
    }

    /**
     * Quick classification against all protocols. 
     * Use sparingly; preferred flow is RAG-first (Retrieve and Rerank).
     */
    public String classify(String text, List<Protocol> allProtocols) {
        TriageResult result = classifyDynamic(text, allProtocols);
        return result.intent().name();
    }

    private static final String NAME_EXCLUSION_REGEX = "(?i)\\b(soteria|sotelia|zoteria|soteia)\\b";

    /**
     * Ranks {@code candidates} by semantic similarity to {@code text} and returns the best match if
     * its cosine similarity score meets {@value #SIMILARITY_THRESHOLD}. When {@code candidates} is
     * empty, returns {@link Intent#GREETING_OR_CASUAL} — RAG determined nothing is relevant, not an
     * error condition.
     *
     * @param text       raw user input; {@code null} or blank returns {@link Intent#UNKNOWN}
     * @param candidates pre-filtered protocol list from RAG; {@code null} treated as empty
     * @return triage result with the winning protocol (or {@code null}), its score, and the mapped intent
     */
    public TriageResult classifyDynamic(String text, List<Protocol> candidates) {
        logRaw(LOG_INPUT, "DYNAMIC_TRIAGE: " + (text == null ? "NULL" : text)
                + " | candidates=" + (candidates != null ? candidates.size() : 0));

        if (text == null || text.isBlank()) {
            return new TriageResult(null, 0.0f, Intent.UNKNOWN);
        }

        // PRE-PROCESSING: Remove assistant name variations to avoid semantic noise/false positives
        String processedText = preprocess(text);

        // No candidates = RAG found nothing relevant; the input was processable but
        // doesn't map to any protocol. That's "casual / not an emergency", not an error.
        if (candidates == null || candidates.isEmpty()) {
            return new TriageResult(null, 0.0f, Intent.GREETING_OR_CASUAL);
        }

        // High information density script detection (CJK, Arabic, Indic, etc.)
        boolean isHighDensity = processedText.codePoints().anyMatch(cp -> {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
            return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                   block == Character.UnicodeBlock.HIRAGANA ||
                   block == Character.UnicodeBlock.KATAKANA ||
                   block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                   block == Character.UnicodeBlock.ARABIC ||
                   block == Character.UnicodeBlock.HEBREW ||
                   block == Character.UnicodeBlock.DEVANAGARI ||
                   block == Character.UnicodeBlock.THAI;
        });

        if (processedText.trim().length() < (isHighDensity ? 1 : 3)) {
            logRaw(LOG_OUTPUT, "Result: GREETING_OR_CASUAL (too short after filtering: '" + processedText.trim() + "')");
            return new TriageResult(null, 0.0f, Intent.GREETING_OR_CASUAL);
        }

        try {
            float[] inputVector = model.embed(processedText);
            float[] centeredInput = (centroid != null) ? center(inputVector) : inputVector;

            ProtocolBestMatch bestMatch = findBestProtocol(centeredInput, candidates);

            if (bestMatch.protocol != null && bestMatch.score >= SIMILARITY_THRESHOLD) {
                Intent intent = mapToIntent(bestMatch.protocol.getCategory());
                TriageResult result = new TriageResult(bestMatch.protocol, bestMatch.score, intent);
                logRaw(LOG_OUTPUT, String.format("Result: %s (Score: %.4f, ID: %s)", intent, bestMatch.score, bestMatch.protocol.getId()));
                return result;
            }

            // Below threshold = no protocol matched, but the input was processable.
            logRaw(LOG_OUTPUT, String.format("Result: GREETING_OR_CASUAL (Score: %.4f, no match above threshold)", bestMatch.score));
            return new TriageResult(null, bestMatch.score, Intent.GREETING_OR_CASUAL);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Dynamic triage failed", e);
            return new TriageResult(null, 0.0f, Intent.UNKNOWN);
        }
    }

    private String preprocess(String text) {
        if (text == null) return null;
        // Remove names and extra spaces
        return text.replaceAll(NAME_EXCLUSION_REGEX, "").replaceAll("\\s+", " ").trim();
    }

    private record ProtocolBestMatch(Protocol protocol, float score) {}

    private ProtocolBestMatch findBestProtocol(float[] centeredInput, List<Protocol> candidates) {
        Protocol bestProtocol = null;
        float maxSimilarity = -1.0f;

        for (Protocol protocol : candidates) {
            float[] protocolVector = getOrCacheVector(protocol);
            float similarity = VectorMath.cosineSimilarity(centeredInput, protocolVector);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestProtocol = protocol;
            }
        }
        return new ProtocolBestMatch(bestProtocol, maxSimilarity);
    }

    private float[] getOrCacheVector(Protocol protocol) {
        return protocolVectorCache.computeIfAbsent(protocol.getId(), id -> {
            String role = "";
            if (id.endsWith("_VIC")) {
                role = "[VIC]";
            } else if (id.endsWith("_WIT")) {
                role = "[WIT]";
            }
            String keywordsStr = protocol.getKeywords() == null ? "" : String.join(" ", protocol.getKeywords());
            String anchorText = String.format("%s %s %s", role, protocol.getTitle(), keywordsStr).trim();
            
            float[] raw = model.embed(anchorText);
            return (centroid != null) ? center(raw) : raw;
        });
    }

    private Intent mapToIntent(String category) {
        if (category == null)
            return Intent.UNKNOWN;
        
        
        // Map based on the original category logic (internal)
        return switch (category.toUpperCase()) {
            case "MEDICAL", "VITAL", "TRAUMA" -> Intent.MEDICAL_EMERGENCY;
            case "SECURITY", "CRIME", "THREAT" -> Intent.SECURITY_EMERGENCY;
            case "ENVIRONMENTAL", "FIRE", "HAZMAT" -> Intent.ENVIRONMENTAL_EMERGENCY;
            case "TRAFFIC", "ACCIDENT" -> Intent.TRAFFIC_EMERGENCY;
            default -> Intent.UNKNOWN;
        };
    }

    private void logRaw(String filename, String content) {
        try {
            Path path = Paths.get("logs", "raw_classifier", filename);
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String entry = String.format("%s | %s%n%s%n", timestamp, content, "-".repeat(50));
            Files.writeString(path, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            logger.log(Level.FINE, e, () -> "Failed to write raw classifier log: " + filename);
        }
    }

    /**
     * Configures mean-centering for the embedding space. All subsequent comparisons will subtract
     * this centroid from input and protocol vectors before normalizing, improving discrimination
     * when protocols cluster semantically close together.
     * <p>Invalidates the protocol vector cache — cached vectors were built in the old space and
     * must be recomputed for the new one.</p>
     */
    public void setCentroid(float[] centroid) {
        this.protocolVectorCache.clear(); // clear BEFORE updating: entries are valid only in the old space
        this.centroid = centroid;
    }

    private float[] center(float[] vector) {
        if (centroid == null) return vector;
        return VectorMath.normalize(VectorMath.subtract(vector, centroid));
    }

    /**
     * Embeds {@code text} using the service's local model. Prefer this over exposing the
     * underlying {@code LlamaModel} — for example, to build centroids from protocol corpora
     * without leaking the llama.cpp implementation type.
     *
     * @return raw embedding vector; length depends on the model's hidden dimension
     */
    public float[] embed(String text) {
        return model.embed(text);
    }

    /**
     * Returns the underlying {@link LlamaModel} for callers that need to wire it into
     * components expecting a {@code LlamaModel} directly (e.g. {@code EmergencyKnowledgeBase#setEmbedder}).
     * Prefer {@link #embed(String)} for single-text embedding.
     */
    public LlamaModel getModel() {
        return model;
    }

    @Override
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}

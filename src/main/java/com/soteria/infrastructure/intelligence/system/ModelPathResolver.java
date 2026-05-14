package com.soteria.infrastructure.intelligence.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.soteria.infrastructure.intelligence.system.ModelAssets.*;

/**
 * Resolves local file system paths for AI models and performs structural validation.
 * <p>
 * Uses a cascading priority (checking {@code ~/.soteria} then the local repo) to
 * support both portable development and installed environments. Validation checks
 * ensure all required internal files (e.g., encoder/decoder ONNX graphs) are present.
 * </p>
 */
public class ModelPathResolver {
    private static final Logger logger = Logger.getLogger(ModelPathResolver.class.getName());

    private final SystemCapability capability;
    private final Path modelBasePath;

    public ModelPathResolver(SystemCapability capability) {
        this(capability, Paths.get(System.getProperty("user.home"), ".soteria", MODEL_DIR));
    }

    public ModelPathResolver(SystemCapability capability, Path modelBasePath) {
        this.capability = capability;
        this.modelBasePath = modelBasePath;
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(modelBasePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not create models directory", e);
        }
    }

    public SystemCapability getCapability() {
        return capability;
    }

    public Path getModelBasePath() {
        return modelBasePath;
    }

    public Path getBrainModelPath() {
        return getBrainModelPath(capability.getRecommendedProfile());
    }

    public Path getBrainModelPath(SystemCapability.AIModelProfile profile) {
        String fileName = getBrainModelFileName(profile);

        // 1. ~/.soteria/models/<file>
        Path userPath = modelBasePath.resolve(fileName);
        if (Files.exists(userPath)) {
            return userPath;
        }

        // 2. <repo root>/models/<file>  (útil en desarrollo / distribución local)
        Path repoModels = Paths.get(System.getProperty("user.dir"), "models", fileName);
        if (Files.exists(repoModels)) {
            logger.log(Level.INFO, "Brain model found in repo models/: {0}", repoModels);
            return repoModels;
        }

        // 3. No encontrado → devolver path en ~/.soteria/models/ para que el downloader descargue ahí
        return userPath;
    }

    public Path getSTTModelPath() {
        return resolveModelPath(STT_MODEL_NAME, true);
    }

    public Path getVADModelPath() {
        return resolveModelPath(VAD_MODEL_NAME, false);
    }

    public Path getKWSModelPath() {
        return resolveModelPath(KWS_MODEL_NAME, true);
    }

    public Path getTriageModelPath() {
        return resolveModelPath(TRIAGE_MODEL_NAME, false);
    }

    public Path getTTSModelPath() {
        return resolveModelPath(TTS_MODEL_NAME, true);
    }

    /**
     * Resolves the highest priority path for a model.
     * <p>Checks {@code ~/.soteria/models/} first, then falls back to the repository root.
     * If missing in both, returns the {@code ~/.soteria} path as the target for the downloader.</p>
     */
    private Path resolveModelPath(String name, boolean isDir) {
        // 1. ~/.soteria/models/
        Path userPath = modelBasePath.resolve(name);
        if (isDir ? (Files.isDirectory(userPath)) : Files.exists(userPath)) {
            return userPath;
        }

        // 2. <repo>/models/
        Path repoPath = Paths.get(System.getProperty("user.dir"), "models", name);
        if (isDir ? (Files.isDirectory(repoPath)) : Files.exists(repoPath)) {
            logger.log(Level.INFO, "Model found in repo models/: {0}", repoPath);
            return repoPath;
        }

        // 3. Fallback → ~/.soteria/models/ para descarga
        return userPath;
    }

    public Path getEmbeddingModelPath() {
        return getTriageModelPath();
    }

    public Path getKBIndexPath() {
        Path indexPath = modelBasePath.getParent().resolve("index");
        try {
            Files.createDirectories(indexPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not create index directory", e);
        }
        return indexPath;
    }

    public boolean isBrainModelReady(SystemCapability.AIModelProfile profile) {
        return Files.exists(getBrainModelPath(profile));
    }

    public boolean isSTTModelReady() {
        Path path = getSTTModelPath();
        if (!Files.exists(path) || !Files.isDirectory(path)) return false;

        try (var stream = Files.list(path)) {
            List<Path> files = stream.toList();
            boolean hasEncoder = files.stream().anyMatch(p -> p.getFileName().toString().endsWith("-encoder.onnx") || p.getFileName().toString().endsWith("-encoder.int8.onnx"));
            boolean hasDecoder = files.stream().anyMatch(p -> p.getFileName().toString().endsWith("-decoder.onnx") || p.getFileName().toString().endsWith("-decoder.int8.onnx"));
            boolean hasTokens = files.stream().anyMatch(p -> p.getFileName().toString().endsWith("-tokens.txt") || p.getFileName().toString().equals("tokens.txt"));

            return hasEncoder && hasDecoder && hasTokens;
        } catch (IOException _) {
            return false;
        }
    }

    public boolean isVADModelReady() {
        return Files.exists(getVADModelPath());
    }

    public boolean isKWSModelReady() {
        Path path = getKWSModelPath();
        return Files.exists(path) && Files.isDirectory(path)
            && Files.exists(path.resolve("encoder-epoch-13-avg-2-chunk-16-left-64.onnx"));
    }

    public boolean isTriageModelReady() {
        return Files.exists(getTriageModelPath());
    }

    public boolean isTTSModelReady() {
        Path path = getTTSModelPath();
        return Files.exists(path) && Files.isDirectory(path)
            && Files.exists(path.resolve("model.onnx"))
            && Files.exists(path.resolve("voices.bin"));
    }

    public boolean isEmbeddingModelReady() {
        return Files.exists(getEmbeddingModelPath());
    }

    public String getBrainModelFileName(SystemCapability.AIModelProfile profile) {
        return switch (profile) {
            case LITE -> "gemma4-soteria.gguf";  // Fine-tuned E2B for emergency protocols
            case STABLE -> "gemma4-E4B-soteria.gguf";  // Fine-tuned E4B for emergency protocols
            case EXPERT -> "gemma-4-E4B-it-Q8_0.gguf";
        };
    }
}

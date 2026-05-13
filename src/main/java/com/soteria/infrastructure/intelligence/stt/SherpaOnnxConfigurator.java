package com.soteria.infrastructure.intelligence.stt;

import com.k2fsa.sherpa.onnx.FeatureConfig;
import com.k2fsa.sherpa.onnx.OfflineModelConfig;
import com.k2fsa.sherpa.onnx.OfflineRecognizer;
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig;
import com.k2fsa.sherpa.onnx.SileroVadModelConfig;
import com.k2fsa.sherpa.onnx.Vad;
import com.k2fsa.sherpa.onnx.VadModelConfig;
import com.soteria.infrastructure.intelligence.system.ModelManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Factory for sherpa-onnx components used by {@link SherpaSTTService}: Whisper offline ASR and Silero VAD.
 * <p>
 * Package-private: configuration details stay next to the service without exposing sherpa types publicly.
 * </p>
 */
final class SherpaOnnxConfigurator {

    static {
        Logger.getLogger(SherpaSTTService.class.getName()).fine("STT: Loading native libraries...");
        com.soteria.infrastructure.intelligence.system.NativeLibraryLoader.load();
        Logger.getLogger(SherpaSTTService.class.getName()).fine("STT: Native libraries ready.");
    }

    private SherpaOnnxConfigurator() {
    }

    /**
     * Locates encoder, decoder, and token files under {@code modelPath} and builds an offline Whisper recognizer
     * for {@link ModelManager#STT_SAMPLE_RATE}.
     * <p>{@code useBeamSearch} is accepted for API symmetry but currently has no effect: sherpa-onnx does not
     * support {@code beam_search} for Whisper models and always falls back to {@code greedy_search}.</p>
     *
     * @param modelPath     directory containing Whisper ONNX and token assets
     * @param language      Whisper language code; caller is responsible for ISO normalization
     * @param useBeamSearch reserved — currently ignored
     * @return a new recognizer; caller must {@link OfflineRecognizer#release()} when done
     * @throws IOException if required model files are missing or {@code modelPath} is unreadable
     */
    static OfflineRecognizer createWhisperRecognizer(Path modelPath, String language, boolean useBeamSearch) throws IOException {
        Path encoderPath = findFileBySuffix(modelPath, "-encoder.int8.onnx", "-encoder.onnx");
        Path decoderPath = findFileBySuffix(modelPath, "-decoder.int8.onnx", "-decoder.onnx");
        Path tokensPath = findFileBySuffix(modelPath, "-tokens.txt", "tokens.txt");

        if (encoderPath == null || decoderPath == null || tokensPath == null) {
            throw new IOException("Mandatory Whisper model files missing in: " + modelPath);
        }

        OfflineWhisperModelConfig whisperConfig = OfflineWhisperModelConfig.builder()
                .setEncoder(encoderPath.toString())
                .setDecoder(decoderPath.toString())
                .setLanguage(language)
                .setTask("transcribe")
                .build();

        OfflineModelConfig modelConfig = OfflineModelConfig.builder()
                .setWhisper(whisperConfig)
                .setTokens(tokensPath.toString())
                .setNumThreads(2)
                .build();

        String decodingMethod = useBeamSearch ? "beam_search" : "greedy_search";
        
        OfflineRecognizerConfig config = OfflineRecognizerConfig.builder()
                .setOfflineModelConfig(modelConfig)
                .setFeatureConfig(FeatureConfig.builder()
                        .setSampleRate(ModelManager.STT_SAMPLE_RATE)
                        .setFeatureDim(80)
                        .build())
                .setDecodingMethod(decodingMethod)
                .setMaxActivePaths(useBeamSearch ? 4 : 1) // Beam size of 4 for beam search
                .build();

        return new OfflineRecognizer(config);
    }
    
    static OfflineRecognizer createWhisperRecognizer(Path modelPath, String language) throws IOException {
        return createWhisperRecognizer(modelPath, language, false);
    }

    /**
     * @param modelManager supplies VAD model path, readiness check, and all timing/threshold parameters
     * @return a new {@link Vad}; caller must {@link Vad#release()} when done
     * @throws IOException if the VAD model is not available or not yet downloaded
     */
    static Vad createSileroVad(ModelManager modelManager) throws IOException {
        Path vadPath = modelManager.getVADModelPath();
        if (!modelManager.isVADModelReady()) {
            throw new IOException("Silero VAD model not found. Please ensure ModelManager has downloaded it.");
        }

        SileroVadModelConfig sileroConfig = SileroVadModelConfig.builder()
                .setModel(vadPath.toString())
                .setThreshold(modelManager.getSTTVadThreshold())
                .setMinSilenceDuration(modelManager.getSTTMinSilenceDuration())
                .setMinSpeechDuration(modelManager.getSTTMinSpeechDuration())
                .setWindowSize(ModelManager.VAD_WINDOW_SIZE)
                .build();

        VadModelConfig config = VadModelConfig.builder()
                .setSileroVadModelConfig(sileroConfig)
                .setSampleRate(ModelManager.STT_SAMPLE_RATE)
                .setNumThreads(1)
                .build();

        return new Vad(config);
    }

    /**
     * Returns the first file in {@code directory} whose name ends with one of {@code suffixes}, trying suffixes in
     * order (e.g. prefer quantized {@code .int8.onnx}) when multiple patterns are listed first.
     *
     * @param directory model directory to scan
     * @param suffixes    candidate filename suffixes; order defines preference per iteration
     * @return matching path, or {@code null} if none match
     * @throws IOException if the directory cannot be listed
     */
    static Path findFileBySuffix(Path directory, String... suffixes) throws IOException {
        try (var stream = Files.list(directory)) {
            List<Path> files = stream.toList();
            for (String suffix : suffixes) {
                for (Path file : files) {
                    if (file.getFileName().toString().endsWith(suffix)) {
                        return file;
                    }
                }
            }
        }
        return null;
    }
}

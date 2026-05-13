package com.soteria.infrastructure.intelligence.tts;

import com.k2fsa.sherpa.onnx.*;
import com.soteria.infrastructure.intelligence.system.NativeLibraryLoader;
import com.soteria.infrastructure.intelligence.system.LanguageUtils;

import java.nio.file.Path;

/**
 * Manages the lifecycle of the Sherpa-ONNX {@link OfflineTts} (Kokoro) engine.
 *
 * <p>Kokoro applies {@code setLang} at engine-build time, not per-call. If the
 * UI language changes after the initial build (e.g. Spanish at install, English
 * in settings), the entire engine must be rebuilt — updating the speaker ID
 * alone produces incorrect pronunciation. {@link #ensureEngineLanguage(String)}
 * performs this check before every synthesis call.</p>
 */
public class TTSModelManager implements AutoCloseable {

    private final Path modelPath;
    private final TTSLogger ttsLogger;
    private OfflineTts offlineTts;
    /** ISO-639-1 code last passed to Kokoro {@code setLang}. */
    private String loadedEngineLang = "";

    public TTSModelManager(Path modelPath, TTSLogger ttsLogger, String language) {
        this.modelPath = modelPath;
        this.ttsLogger = ttsLogger;
        rebuildOfflineTts(language);
    }

    /**
     * Rebuilds the native engine if the resolved language code differs from the
     * currently loaded one.
     *
     * <p>Called under {@code ttsNativeLock} in {@link SherpaTTSService} before
     * every {@link #generate} call.</p>
     */
    public synchronized void ensureEngineLanguage(String lang) {
        String code = resolveLanguageCode(lang);
        if (offlineTts != null && code.equals(loadedEngineLang)) return;
        ttsLogger.info("TTS: rebuilding Kokoro engine for language code: " + code);
        rebuildOfflineTts(lang);
    }

    private void rebuildOfflineTts(String language) {
        try {
            NativeLibraryLoader.load();

            String lexiconPath = modelPath.resolve("lexicon-zh.txt") + "," + modelPath.resolve("lexicon-us-en.txt");
            String fstPath = modelPath.resolve("phone-zh.fst") + "," + modelPath.resolve("date-zh.fst") + ","
                    + modelPath.resolve("number-zh.fst");

            String langCode = resolveLanguageCode(language);

            if (offlineTts != null) {
                offlineTts.release();
                offlineTts = null;
            }

            OfflineTtsKokoroModelConfig kokoroConfig = OfflineTtsKokoroModelConfig.builder()
                    .setModel(modelPath.resolve("model.onnx").toString())
                    .setVoices(modelPath.resolve("voices.bin").toString())
                    .setTokens(modelPath.resolve("tokens.txt").toString())
                    .setDataDir(modelPath.resolve("espeak-ng-data").toString())
                    .setDictDir(modelPath.resolve("dict").toString())
                    .setLexicon(lexiconPath)
                    .setLang(langCode)
                    .build();

            OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                    .setKokoro(kokoroConfig)
                    .setNumThreads(4)
                    .setDebug(false)
                    .build();

            OfflineTtsConfig config = OfflineTtsConfig.builder()
                    .setModel(modelConfig)
                    .setRuleFsts(fstPath)
                    .setMaxNumSentences(10)
                    .build();

            this.offlineTts = new OfflineTts(config);
            this.loadedEngineLang = langCode;
            ttsLogger.logInitialization(modelPath, 4);
        } catch (Exception e) {
            ttsLogger.error("Failed to initialize Sherpa-ONNX model", e);
            throw new IllegalStateException("TTS Model initialization failed", e);
        }
    }

    public synchronized GeneratedAudio generate(String text, int speakerId, float rate) {
        try {
            return offlineTts != null ? offlineTts.generate(text, speakerId, rate) : null;
        } catch (Exception e) {
            ttsLogger.error("Error generating audio for text: " + text, e);
            return null;
        }
    }

    public String resolveLanguageCode(String lang) {
        String code = LanguageUtils.isoCode(lang);
        return code.isEmpty() ? "en" : code;
    }

    @Override
    public synchronized void close() {
        if (offlineTts != null) {
            offlineTts.release();
            offlineTts = null;
            loadedEngineLang = "";
            ttsLogger.info("TTS Model released");
        }
    }
}

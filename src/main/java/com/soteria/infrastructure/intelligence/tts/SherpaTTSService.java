package com.soteria.infrastructure.intelligence.tts;

import com.soteria.core.port.TTS;
import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.soteria.infrastructure.intelligence.system.LanguageUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link TTS} implementation backed by Sherpa-ONNX and the Kokoro-82M model.
 *
 * <p>Synthesis is decoupled from playback via three daemon threads:
 * {@code TTS-Synthesis} drains the utterance queue and calls the native engine,
 * {@code TTS-BufferFlush} flushes the prosodic lookahead buffer on timeout,
 * and {@code TTS-Playback} (owned by {@link TTSAudioPlayer}) writes PCM to the
 * audio line.</p>
 *
 * <p>The prosodic lookahead buffer accumulates streaming LLM tokens until a
 * sentence boundary is detected, then synthesizes the current chunk with the
 * next {@value #LOOKAHEAD_WORDS} words appended as context. This gives Kokoro
 * enough future text to plan intonation correctly. The generated audio is
 * truncated to the actual chunk length before playback.</p>
 *
 * <p>{@link #ttsNativeLock} serializes all calls into the native Kokoro engine
 * because {@code setLanguage} and {@code generate} share mutable engine state
 * and are called from different threads.</p>
 *
 * @see TTSModelManager
 * @see TTSAudioPlayer
 * @see TtsTextSanitizer
 */
public class SherpaTTSService implements TTS, AutoCloseable {
    private static final int SAMPLE_RATE = 24000;

    private final TTSLogger ttsLogger;
    private final TTSModelManager modelManager;
    private final TTSAudioPlayer audioPlayer;

    private float speechRate = 1.44f;
    private volatile String language = "en";
    private volatile int cachedSpeakerId = 0;
    /** Serializes Kokoro native rebuild + generate — inference thread was calling setLanguage during worker generate. */
    private final Object ttsNativeLock = new Object();
    private volatile boolean muted = false;
    private volatile boolean running = false;
    private final java.util.concurrent.atomic.AtomicReference<TTSErrorCallback> errorCallback = new java.util.concurrent.atomic.AtomicReference<>(null);
    private volatile boolean warmupComplete = false;

    private final LinkedBlockingQueue<QueuedUtterance> synthesisQueue = new LinkedBlockingQueue<>(100);
    private final AtomicInteger pendingSynthesis = new AtomicInteger(0);
    private final AtomicBoolean interruptRequested = new AtomicBoolean(false);
    private Thread synthesisThread;

    private final ProsodicLookaheadBuffer prosodicBuffer = new ProsodicLookaheadBuffer();
    private static final long BUFFER_FLUSH_TIMEOUT_MS = 100;
    private static final String QUEUE_FULL_ERROR = "TTS queue full";
    private static final int SILENCE_MS = 0;

    /**
     * Speaker IDs for the female voice in each supported language.
     * The {@code zh} entry must not use sid 0 — the native ONNX engine crashes
     * with that combination on Windows.
     */
    private static final Map<String, Integer> FEMALE_VOICE_MAP = Map.of(
            "en", 0,  // af_bella
            "es", 31, // ef_mariela
            "ca", 31,
            "fr", 35, // ff_siwis
            "it", 37, // if_sarah
            "pt", 40, // pf_dora
            "zh", 45  // zf_xiaobei — sid 0 crashes ONNX on Windows with zh
    );

    public SherpaTTSService(Path modelPath) {
        this(modelPath, "en");
    }

    /**
     * Initializes the TTS engine for the given language and starts all worker threads.
     *
     * <p>A silent warmup synthesis runs in the background immediately after construction
     * to pre-load the ONNX model weights into memory, reducing first-utterance latency.
     * {@link #warmupComplete} becomes {@code true} when it finishes.</p>
     *
     * @param modelPath directory containing {@code model.onnx}, {@code voices.bin}, etc.
     * @param language  initial language display name or ISO code (e.g. {@code "Spanish"}, {@code "es"})
     * @throws IllegalStateException if the native Kokoro engine fails to initialize
     */
    public SherpaTTSService(Path modelPath, String language) {
        this.ttsLogger = new TTSLogger();
        this.ttsLogger.setup();

        this.modelManager = new TTSModelManager(modelPath, ttsLogger, language);
        this.audioPlayer = new TTSAudioPlayer(ttsLogger);
        this.language = language;
        this.cachedSpeakerId = resolveSpeakerId(language);

        startWorkerThreads();
    }

    private void startWorkerThreads() {
        running = true;
        audioPlayer.start();
        startWarmupThread();
        startBufferFlushThread();
        startSynthesisThread();
    }

    private void startWarmupThread() {
        Thread warmupThread = new Thread(() -> {
            try {
                ttsLogger.info("TTS warmup...");
                synchronized (ttsNativeLock) {
                    modelManager.generate(warmupPhrase(language), cachedSpeakerId, speechRate);
                }
                ttsLogger.info("TTS warmup complete");
            } catch (Exception e) {
                ttsLogger.warn("TTS warmup failed: " + e.getMessage());
            } finally {
                warmupComplete = true;
            }
        }, "TTS-Warmup");
        warmupThread.setDaemon(true);
        warmupThread.start();
    }

    private void startBufferFlushThread() {
        Thread bufferFlushThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(100);
                    if (prosodicBuffer.getBufferLength() > 0) {
                        long timeSinceLastAdd = System.currentTimeMillis() - prosodicBuffer.getLastAppendTime();
                        if (timeSinceLastAdd >= BUFFER_FLUSH_TIMEOUT_MS) {
                            prosodicBuffer.flush().ifPresent(chunk -> enqueueChunk(chunk, this.language));
                        }
                    }
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "TTS-BufferFlush");
        bufferFlushThread.setDaemon(true);
        bufferFlushThread.start();
    }

    private void startSynthesisThread() {
        synthesisThread = new Thread(this::processSynthesisQueue, "TTS-Synthesis");
        synthesisThread.setDaemon(true);
        synthesisThread.start();
    }

    /**
     * Returns a short warmup phrase in the target language.
     *
     * <p>Latin-script phrases crash the ONNX engine when the loaded language is
     * {@code zh} or {@code ja} — those languages require their native scripts.</p>
     */
    private static String warmupPhrase(String uiLanguage) {
        String code = LanguageUtils.isoCode(uiLanguage);
        if (code.isEmpty()) code = "en";
        return switch (code) {
            case "zh" -> "你好";
            case "ja" -> "こんにちは";
            case "es" -> "Hola.";
            case "fr" -> "Bonjour.";
            case "de" -> "Hallo.";
            case "it" -> "Ciao.";
            case "pt" -> "Olá.";
            case "ro" -> "Bună.";
            case "ru" -> "Здравствуйте.";
            case "ar" -> "مرحبا.";
            default -> "Hello.";
        };
    }

    private void processSynthesisQueue() {
        while (running) {
            try {
                QueuedUtterance item = synthesisQueue.poll(100, TimeUnit.MILLISECONDS);
                if (item != null && !interruptRequested.get()) {
                    synthesizeText(item.text(), item.sanitizeLanguageHint(), item.actualWordCount());
                } else if (item != null) {
                    pendingSynthesis.decrementAndGet();
                }
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void synthesizeText(String text, String sanitizeLanguageHint, int actualWordCount) {
        try {
            String trimmedText = TtsTextSanitizer.sanitize(text, sanitizeLanguageHint);
            if (trimmedText.isEmpty()) return;

            String actualContent = extractActualContent(trimmedText, actualWordCount);
            float currentSpeechRate = calculateSpeechRate(actualContent);

            GeneratedAudio audio = generateAudioWithLock(trimmedText, currentSpeechRate);

            if (audio != null && audio.getSamples() != null && audio.getSamples().length > 0) {
                float[] samples = truncateAudioIfNeeded(audio.getSamples(), trimmedText, actualContent, actualWordCount);
                processAndEnqueueAudio(samples, actualContent, currentSpeechRate);
            } else {
                ttsLogger.warn("TTS: empty audio for: " + trimmedText);
                notifyError(text, new IllegalStateException("Empty audio generated"));
            }
        } catch (Exception ex) {
            ttsLogger.error("TTS synthesis error", ex);
            notifyError(text, ex);
        } finally {
            pendingSynthesis.decrementAndGet();
        }
    }

    private String extractActualContent(String trimmedText, int actualWordCount) {
        if (actualWordCount <= 0) return trimmedText;
        String[] words = trimmedText.split("\\s+");
        return actualWordCount < words.length
                ? String.join(" ", java.util.Arrays.copyOfRange(words, 0, actualWordCount))
                : trimmedText;
    }

    private GeneratedAudio generateAudioWithLock(String text, float rate) {
        synchronized (ttsNativeLock) {
            modelManager.ensureEngineLanguage(this.language);
            return modelManager.generate(text, this.cachedSpeakerId, rate);
        }
    }

    private float[] truncateAudioIfNeeded(float[] samples, String fullText, String actualContent, int actualWordCount) {
        if (actualWordCount <= 0 || actualContent.length() >= fullText.length()) {
            return samples;
        }
        float ratio = (float) actualContent.length() / fullText.length();
        int truncateLength = (int) (samples.length * ratio);
        return java.util.Arrays.copyOfRange(samples, 0, Math.min(truncateLength, samples.length));
    }

    private float calculateSpeechRate(String trimmedText) {
        // Slow down slightly for questions to match natural interrogative prosody
        if (trimmedText.endsWith("?") || trimmedText.endsWith("\uFF1F")) {
            return this.speechRate * 0.90f;
        }
        return this.speechRate;
    }

    private void processAndEnqueueAudio(float[] samples, String text, float rate) {
        float[] trimmedSamples = trimSilence(samples);
        if (trimmedSamples.length == 0) return;

        byte[] pcm = applyAudioEffects(trimmedSamples);
        ttsLogger.logSynthesis(this.language, text, (trimmedSamples.length * 1000L) / SAMPLE_RATE, rate);

        if (!interruptRequested.get()) {
            audioPlayer.enqueue(pcm);
            audioPlayer.enqueue(audioPlayer.generateSilence(SILENCE_MS));
        }
    }

    private byte[] applyAudioEffects(float[] samples) {
        byte[] pcm = audioPlayer.floatToPcm16(samples);
        audioPlayer.applyFadeIn(pcm);
        audioPlayer.applyFadeOut(pcm);
        return pcm;
    }



    private float[] trimSilence(float[] samples) {
        int start = 0;
        while (start < samples.length && Math.abs(samples[start]) < 0.012f) start++;
        int end = samples.length;
        while (end > start && Math.abs(samples[end - 1]) < 0.012f) end--;

        if (start >= end) return new float[0];
        if (start == 0 && end == samples.length) return samples;

        float[] result = new float[end - start];
        System.arraycopy(samples, start, result, 0, result.length);
        return result;
    }

    private int resolveSpeakerId(String lang) {
        String baseLang = modelManager.resolveLanguageCode(lang);
        int speakerId = FEMALE_VOICE_MAP.getOrDefault(baseLang, 0);
        ttsLogger.info("Resolved speaker ID " + speakerId + " for language: " + lang);
        return speakerId;
    }

    @Override
    public void speak(String text) {
        if (muted || text == null || text.trim().isEmpty()) return;
        stop();
        pendingSynthesis.set(1);
        int wordCount = text.trim().split("\\s+").length;
        if (!synthesisQueue.offer(new QueuedUtterance(text, this.language, wordCount))) {
            ttsLogger.warn("TTS synthesis queue full, dropping utterance");
            pendingSynthesis.decrementAndGet();
            notifyError(text, new IllegalStateException(QUEUE_FULL_ERROR));
        }
    }

    @Override
    public void speakQueued(String text) {
        speakQueued(text, this.language);
    }

    @Override
    public void speakQueued(String text, String sanitizeLanguageHint) {
        if (muted || text == null || text.trim().isEmpty()) return;
        String hint = (sanitizeLanguageHint == null || sanitizeLanguageHint.isBlank())
                ? this.language
                : sanitizeLanguageHint;

        long receiveTime = System.currentTimeMillis();
        ttsLogger.info(String.format("[SPEAKQUEUED] Received: \"%s\" at %d", text, receiveTime));

        prosodicBuffer.append(text).ifPresent(chunk -> {
            long queueTime = System.currentTimeMillis();
            ttsLogger.info(String.format("[QUEUE] Queuing chunk at %d (delay from receive: %dms)",
                    queueTime, queueTime - receiveTime));
            enqueueChunk(chunk, hint);
        });
    }

    private void enqueueChunk(ProsodicLookaheadBuffer.ChunkToSynthesize chunk, String languageHint) {
        pendingSynthesis.incrementAndGet();
        if (!synthesisQueue.offer(new QueuedUtterance(chunk.textWithLookahead(), languageHint, chunk.actualWordCount()))) {
            ttsLogger.warn("TTS synthesis queue full, dropping utterance");
            pendingSynthesis.decrementAndGet();
            notifyError(chunk.textWithLookahead(), new IllegalStateException(QUEUE_FULL_ERROR));
        }
    }

    @Override
    public void stop() {
        interruptRequested.set(true);
        synthesisQueue.clear();
        pendingSynthesis.set(0);
        prosodicBuffer.clear();
        audioPlayer.stop();
        interruptRequested.set(false);
    }

    @Override
    public void setSpeechRate(float rate) {
        this.speechRate = Math.clamp(rate, 0.5f, 2.0f);
    }

    @Override
    public void setVolume(float volume) {
        audioPlayer.setVolume(Math.clamp(volume, 0.0f, 1.0f));
    }

    @Override
    public boolean isSpeaking() {
        return pendingSynthesis.get() > 0 || !synthesisQueue.isEmpty() || !audioPlayer.isQueueEmpty();
    }

    @Override
    public void setLanguage(String language) {
        synchronized (ttsNativeLock) {
            this.language = language;
            this.cachedSpeakerId = resolveSpeakerId(language);
            modelManager.ensureEngineLanguage(language);
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) stop();
    }

    public boolean isWarmupComplete() {
        return warmupComplete;
    }

    @Override
    public void setErrorCallback(TTSErrorCallback callback) {
        this.errorCallback.set(callback);
    }

    private void notifyError(String text, Throwable error) {
        TTSErrorCallback callback = this.errorCallback.get();
        if (callback != null) {
            try {
                callback.onError(text, error);
            } catch (Exception e) {
                ttsLogger.error("Error in TTS error callback", e);
            }
        }
    }

    @Override
    public void shutdown() {
        stop();
        running = false;
        if (synthesisThread != null) {
            synthesisThread.interrupt();
            try {
                synthesisThread.join(2000);
            } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
        }
        audioPlayer.close();
        modelManager.close();
        ttsLogger.info("TTS Service shut down");
    }

    @Override
    public void close() {
        shutdown();
    }

    private record QueuedUtterance(String text, String sanitizeLanguageHint, int actualWordCount) {}
}

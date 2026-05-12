package com.soteria.core.port;

/**
 * Port for text-to-speech synthesis.
 */
public interface TTS {

    void speak(String text);

    /**
     * Queues speech without interrupting previous audio.
     * Used for streaming TTS where sentences arrive progressively.
     * Default implementation falls back to {@link #speak(String)}.
     */
    default void speakQueued(String text) {
        speak(text);
    }

    /**
     * Queues speech with a language hint for text sanitization (e.g. CJK filtering).
     * Implementations that ignore hints may delegate to {@link #speakQueued(String)}.
     */
    default void speakQueued(String text, String sanitizeLanguageHint) {
        speakQueued(text);
    }

    void stop();

    void setLanguage(String language);

    void setSpeechRate(float rate);

    void setVolume(float volume);

    boolean isSpeaking();

    /** Default does nothing — exists for backward compatibility with implementations that predate this method. */
    default void setErrorCallback(TTSErrorCallback callback) {}

    void shutdown();

    @FunctionalInterface
    interface TTSErrorCallback {
        void onError(String text, Throwable error);
    }
}

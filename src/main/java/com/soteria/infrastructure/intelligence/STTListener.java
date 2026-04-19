package com.soteria.infrastructure.intelligence;

/**
 * Interface for receiving STT events.
 */
public interface STTListener {
    void onResult(String text);
    void onPartialResult(String text);
    void onError(Throwable t);
}

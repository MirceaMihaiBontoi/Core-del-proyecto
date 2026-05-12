package com.soteria.core.port;

/**
 * Callback for speech-to-text events.
 */
public interface STTListener {

    void onResult(String text);

    void onPartialResult(String text);

    void onError(Throwable t);
}

package com.soteria.core.port;

/**
 * Port for speech-to-text.
 * Transcription results arrive asynchronously via {@link STTListener}.
 */
public interface STT {

    void startListening(STTListener listener);

    void stopListening();
}

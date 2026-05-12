package com.soteria.core.port;

/**
 * Low-level listener for a single LLM inference turn.
 * Separates token streaming from structured header parsing ({@code [ANALYSIS]} block).
 */
public interface InferenceListener {

    void onToken(String token);

    /**
     * Called when the {@code [ANALYSIS]} header has been fully parsed.
     *
     * @param protocolId matched protocol ID (e.g. {@code "FIRE_001"})
     * @param status     detected emergency status ({@code "TRIAGE"}, {@code "ACTIVE"}, {@code "RESOLVED"})
     */
    void onAnalysisComplete(String protocolId, String status);

    /**
     * Called when the full inference turn is complete.
     *
     * @param fullText the conversational response, excluding the {@code [ANALYSIS]} header
     */
    void onComplete(String fullText);

    void onError(Throwable t);
}

package com.soteria.infrastructure.intelligence.tts;

import java.util.Optional;

/**
 * Accumulates streaming LLM tokens and emits synthesis chunks at prosodic boundaries.
 *
 * <p>Buffers incoming text until a sentence boundary ({@code .!?}) or comma is
 * detected, then returns the current chunk with the next {@value #LOOKAHEAD_WORDS}
 * words appended as context. This gives the TTS engine enough future text to
 * plan natural intonation. The caller is responsible for truncating the generated
 * audio to the actual chunk length.</p>
 *
 * <p>A timeout-based flush mechanism (external to this class) should call
 * {@link #flush()} after a period of inactivity to prevent indefinite buffering
 * when the LLM stops mid-sentence.</p>
 */
public final class ProsodicLookaheadBuffer {

    private static final int LOOKAHEAD_WORDS = 8;
    private static final int MAX_BUFFER_LENGTH = 300;

    private final StringBuilder lookaheadBuffer = new StringBuilder();
    private volatile long lastAppendTime = 0;

    /**
     * Appends text to the buffer and returns a chunk ready for synthesis if a
     * prosodic boundary is detected.
     *
     * @param text streaming token from the LLM; must not be {@code null}
     * @return chunk with lookahead context if a boundary was found; empty otherwise
     */
    synchronized Optional<ChunkToSynthesize> append(String text) {
        if (text == null || text.trim().isEmpty()) return Optional.empty();

        lookaheadBuffer.append(text);
        if (!text.endsWith(" ")) lookaheadBuffer.append(" ");
        lastAppendTime = System.currentTimeMillis();

        if (containsBoundary(text)) {
            Optional<ChunkToSynthesize> chunk = extractBoundaryChunk(text);
            if (chunk.isPresent()) {
                return chunk;
            }
        }

        // Safety flush when buffer grows too large
        if (lookaheadBuffer.length() > MAX_BUFFER_LENGTH) {
            return flush();
        }

        return Optional.empty();
    }

    private boolean containsBoundary(String text) {
        return text.matches(".*[.!?。！？,，]\\s*$");
    }

    private Optional<ChunkToSynthesize> extractBoundaryChunk(String lastText) {
        boolean hasMajorBoundary = lastText.matches(".*[.!?。！？]\\s*$");
        String accumulated = lookaheadBuffer.toString();
        int lastBoundaryIdx = hasMajorBoundary
                ? findLastProsodyBoundary(accumulated)
                : findLastCommaBoundary(accumulated);

        if (lastBoundaryIdx <= 0) {
            return Optional.empty();
        }

        String currentChunk = accumulated.substring(0, lastBoundaryIdx).trim();
        String remainingText = accumulated.substring(lastBoundaryIdx).trim();

        String lookaheadContext = buildLookaheadContext(remainingText);
        String textWithLookahead = lookaheadContext.isEmpty()
                ? currentChunk
                : currentChunk + " " + lookaheadContext;

        lookaheadBuffer.setLength(0);
        if (!remainingText.isEmpty()) lookaheadBuffer.append(remainingText).append(" ");

        return Optional.of(new ChunkToSynthesize(textWithLookahead, currentChunk.split("\\s+").length));
    }

    private String buildLookaheadContext(String remainingText) {
        if (remainingText.isEmpty()) return "";
        String[] remainingWords = remainingText.split("\\s+");
        int lookaheadCount = Math.min(LOOKAHEAD_WORDS, remainingWords.length);
        if (lookaheadCount <= 0) return "";
        return String.join(" ", java.util.Arrays.copyOfRange(remainingWords, 0, lookaheadCount));
    }

    /**
     * Flushes any buffered text as a final chunk.
     *
     * <p>Called by the timeout-based flush thread when the buffer has been idle
     * for too long, or when the buffer exceeds {@value #MAX_BUFFER_LENGTH} characters.</p>
     *
     * @return the buffered text as a chunk; empty if the buffer is empty
     */
    synchronized Optional<ChunkToSynthesize> flush() {
        if (lookaheadBuffer.isEmpty()) return Optional.empty();

        String bufferedText = lookaheadBuffer.toString().trim();
        lookaheadBuffer.setLength(0);

        if (bufferedText.isEmpty()) return Optional.empty();

        int wordCount = bufferedText.split("\\s+").length;
        return Optional.of(new ChunkToSynthesize(bufferedText, wordCount));
    }

    synchronized void clear() {
        lookaheadBuffer.setLength(0);
    }

    synchronized long getLastAppendTime() {
        return lastAppendTime;
    }

    synchronized int getBufferLength() {
        return lookaheadBuffer.length();
    }

    private int findLastProsodyBoundary(String text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？') return i + 1;
        }
        return -1;
    }

    private int findLastCommaBoundary(String text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ',' || c == '，') return i + 1;
        }
        return -1;
    }

    /**
     * A chunk of text ready for synthesis, with lookahead context appended.
     *
     * @param textWithLookahead the text to pass to the TTS engine, including future context
     * @param actualWordCount   the number of words in the actual chunk (before lookahead)
     */
    public record ChunkToSynthesize(String textWithLookahead, int actualWordCount) {}
}

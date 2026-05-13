package com.soteria.ui.chat;

/**
 * Text normalization and wake-phrase echo filtering for keyboard and STT sends.
 *
 * <p>Used with the controller outbound dedupe path: {@code _chat.spec.md}.</p>
 */
final class ChatInputGuards {

    /** Ignore repeated sends within this window after normalization (Enter + STT duplicates). */
    static final long RAPID_SUBMIT_GUARD_MS = 450;

    private ChatInputGuards() {
    }

    /**
     * Collapses whitespace for trivial duplicate detection (rapid double Enter, echo submits).
     *
     * @param text raw user or STT string
     * @return normalized key, or empty when null/blank
     */
    static String normalizeForDedupe(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * @param text STT transcript or user text
     * @return {@code true} if, after stripping non-letters, the value is only {@code soteria} (assistant wake echo)
     */
    static boolean isWakePhraseEchoTranscript(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String clean = text.toLowerCase().replaceAll("[^a-z]", "");
        return clean.equals("soteria");
    }
}

package com.soteria.ui.chat;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Blocks a second submit of the same normalized text within {@link ChatInputGuards#RAPID_SUBMIT_GUARD_MS} (keyboard and
 * STT).
 *
 * <p>Details: {@code _chat.spec.md} ({@code ChatOutboundDedupe} section).</p>
 */
final class ChatOutboundDedupe {

    private String lastKey = "";
    private long lastAtMs = 0;

    /**
     * @param rawText    user or STT string
     * @param logger     log target when a duplicate is rejected
     * @param instanceId replaces {@code {0}} in {@code fineLog}
     * @param fineLog    {@link Level#FINE} template when rejecting (one parameter = {@code instanceId})
     * @return {@code true} if the submit should proceed; {@code false} for a rapid duplicate
     */
    synchronized boolean tryAccept(String rawText, Logger logger, String instanceId, String fineLog) {
        String key = ChatInputGuards.normalizeForDedupe(rawText);
        long now = System.currentTimeMillis();
        if (key.equals(lastKey) && now - lastAtMs < ChatInputGuards.RAPID_SUBMIT_GUARD_MS) {
            logger.log(Level.FINE, fineLog, instanceId);
            return false;
        }
        lastKey = key;
        lastAtMs = now;
        return true;
    }
}

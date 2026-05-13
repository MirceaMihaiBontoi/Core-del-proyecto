package com.soteria.ui.chat;

import com.soteria.core.port.TTS;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serialized {@link CompletableFuture} chain that waits until {@link TTS} stops speaking before running work on the
 * JavaFX thread, avoiding races across overlapping completions.
 *
 * <p>Typical use from {@link ChatInferenceUiBridge#onResponseFinalized}: {@code _chat.spec.md}.</p>
 */
final class ChatTTSIdleChain {

    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    private final Object lock = new Object();

    /**
     * Chains after the prior step: polls {@link TTS#isSpeaking()} every 80 ms (no-op if {@code ttsService} is {@code null}),
     * then {@link Platform#runLater(Runnable)} with {@code javafxWork}.
     *
     * @param ttsService TTS backend; may be {@code null}
     * @param log        FINE-level log when a chain step fails (failure is ignored functionally)
     * @param javafxWork runnable on the application thread (e.g. face / subtitle)
     */
    void enqueueAfterSpeechSilence(TTS ttsService, Logger log, Runnable javafxWork) {
        synchronized (lock) {
            tail = tail
                    .handle((ok, err) -> null)
                    .thenRunAsync(() -> {
                        TTS active = ttsService;
                        if (active == null) {
                            return;
                        }
                        try {
                            while (active.isSpeaking()) {
                                Thread.sleep(80);
                            }
                        } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                        }
                    })
                    .thenRun(() -> Platform.runLater(javafxWork))
                    .whenComplete((r, err) -> {
                        if (err != null) {
                            log.log(Level.FINE, "TTS idle chain step failed (ignored)", err.getCause());
                        }
                    });
        }
    }
}

package com.soteria.infrastructure.bootstrap;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the observable UI state and synchronization futures for the bootstrap process.
 *
 * <p>Extracted from {@link BootstrapService} so that JavaFX property management
 * does not clutter the main orchestration logic. All property mutations are
 * marshalled to the FX application thread; when the FX platform is not
 * initialized (e.g. unit tests) the task runs on the calling thread instead.</p>
 *
 * @see BootstrapService
 */
public class BootstrapState {

    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Idle");
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0.0);
    private final ReadOnlyBooleanWrapper readyToChat = new ReadOnlyBooleanWrapper(false);

    // Replaced atomically when the user changes profile/language mid-onboarding.
    private final AtomicReference<CompletableFuture<Void>> readyFuture =
            new AtomicReference<>(new CompletableFuture<>());

    /**
     * Updates the status message and progress value visible in the onboarding UI.
     *
     * @param text localized status string to display
     * @param pct  progress in the range [0.0, 1.0]
     */
    public void update(String text, double pct) {
        executeInFxThread(() -> {
            status.set(text);
            progress.set(pct);
        });
    }

    /**
     * Marks provisioning as finished: sets the final status text, locks progress
     * at 1.0, and unlocks the chat UI.
     *
     * <p>Separate from {@link #completeReadyFuture()} so the UI reflects
     * completion before internal waiters are unblocked.</p>
     *
     * @param localizedStatusText localized "ready" message to display
     */
    public void signalProvisioningComplete(String localizedStatusText) {
        executeInFxThread(() -> {
            status.set(localizedStatusText);
            progress.set(1.0);
            readyToChat.set(true);
        });
    }

    public void setReadyToChat(boolean ready) {
        executeInFxThread(() -> readyToChat.set(ready));
    }

    /**
     * Marshals {@code task} to the FX application thread.
     *
     * <p>Falls back to direct execution when the FX platform is not initialized,
     * which is the normal case in unit tests.</p>
     */
    private void executeInFxThread(Runnable task) {
        try {
            if (Platform.isFxApplicationThread()) {
                task.run();
            } else {
                Platform.runLater(task);
            }
        } catch (IllegalStateException _) {
            // JavaFX Platform not initialized (common in unit tests)
            task.run();
        }
    }

    /** Signals all waiters on {@link #getReadyFuture()} that provisioning succeeded. */
    public void completeReadyFuture() {
        readyFuture.get().complete(null);
    }

    /**
     * Signals all waiters on {@link #getReadyFuture()} that provisioning failed.
     *
     * @param t the cause of the failure
     */
    public void completeReadyFutureExceptionally(Throwable t) {
        readyFuture.get().completeExceptionally(t);
    }

    /**
     * Replaces the current future with a new incomplete one, but only if the
     * existing future is already done.
     *
     * <p>Called when the user changes profile or language so that new waiters
     * block on the updated provisioning run rather than the stale result.</p>
     */
    public void resetReadyFuture() {
        if (readyFuture.get().isDone()) {
            readyFuture.set(new CompletableFuture<>());
        }
    }

    public CompletableFuture<Void> getReadyFuture() {
        return readyFuture.get();
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty readyProperty() {
        return readyToChat.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }

    public double getProgress() {
        return progress.get();
    }
}

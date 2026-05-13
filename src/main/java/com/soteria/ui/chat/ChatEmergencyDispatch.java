package com.soteria.ui.chat;

import com.soteria.core.model.EmergencyEvent;
import com.soteria.core.model.UserData;
import com.soteria.core.port.AlertService;
import com.soteria.core.port.LocationProvider;

import javafx.application.Platform;

/**
 * Runs SOS alert delivery on a background thread ({@code soteria-alert}) and marshals results to the JavaFX thread with
 * {@link Platform#runLater(Runnable)}.
 *
 * <p>{@link EmergencyEvent} and callback details: {@code _chat.spec.md}.</p>
 */
final class ChatEmergencyDispatch {

    /** Invoked on the JavaFX application thread after attempting {@link AlertService#send}. */
    interface Callbacks {
        void onSuccess(String location);

        void onSendFailed();

        void onDispatchError();
    }

    private ChatEmergencyDispatch() {
    }

    /**
     * @param reason           human-readable reason (prefixed with {@code EMERGENCY: } on the event message)
     * @param locationProvider source of the location description
     * @param alertService     delivery channel for the {@link EmergencyEvent}
     * @param currentUser      full name on the event; if {@code null}, a default literal is used
     * @param callbacks        invoked on the JavaFX thread for success, send failure, or exception
     */
    static void start(
            String reason,
            LocationProvider locationProvider,
            AlertService alertService,
            UserData currentUser,
            Callbacks callbacks) {
        new Thread(() -> {
            try {
                String location = locationProvider.getLocationDescription();
                EmergencyEvent event = new EmergencyEvent(
                        "EMERGENCY: " + reason,
                        location,
                        10,
                        currentUser != null ? currentUser.fullName() : "Usuario desconocido");

                boolean success = alertService.send(event);
                Platform.runLater(() -> {
                    if (success) {
                        callbacks.onSuccess(location);
                    } else {
                        callbacks.onSendFailed();
                    }
                });
            } catch (Exception _) {
                Platform.runLater(callbacks::onDispatchError);
            }
        }, "soteria-alert").start();
    }
}

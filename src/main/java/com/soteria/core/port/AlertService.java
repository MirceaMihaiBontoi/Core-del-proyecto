package com.soteria.core.port;

import com.soteria.core.model.EmergencyEvent;
import com.soteria.core.model.UserData;

/**
 * Port for dispatching emergency alerts.
 * Implementations live in {@code infrastructure.notification}.
 */
public interface AlertService {

    /**
     * Dispatches the primary alert for an emergency event.
     *
     * @return {@code true} if the alert was sent successfully, {@code false} otherwise
     */
    boolean send(EmergencyEvent event);

    void notifyContacts(UserData userData, EmergencyEvent event);

    String getAlertType();
}

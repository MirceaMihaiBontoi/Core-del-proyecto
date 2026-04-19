package com.soteria.infrastructure.notification;

import com.soteria.core.model.EmergencyEvent;
import com.soteria.core.model.UserData;
import com.soteria.core.interfaces.AlertService;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of AlertService for standard phone alerts.
 * Demonstrates the use of interfaces and Strategy pattern implementation.
 */
public class CallAlert implements AlertService {
    private static final String EMERGENCY_NUMBER = "112";
    private static final String ALERTS_FILE = "logs/emergency_alerts.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean send(EmergencyEvent event) {
        if (event == null) {
            System.err.println("❌ Error: Cannot send a null alert");
            return false;
        }

        String alertMessage = formatAlertMessage(event);
        
        System.out.println("\n=== CALL ALERT SENT ===");
        System.out.println(alertMessage);
        
        try (FileWriter writer = new FileWriter(ALERTS_FILE, true)) {
            writer.write("-".repeat(80) + "\n");
            writer.write("[CALL] " + alertMessage + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("❌ Error saving alert: " + e.getMessage());
            return false;
        }
        
        return simulateEmergencyCall(event);
    }

    @Override
    public void notifyContacts(UserData userData, EmergencyEvent event) {
        System.out.println("\nNotifying contacts via call...");
        if (userData == null || userData.emergencyContact() == null || userData.emergencyContact().isEmpty()) {
            System.out.println("⚠️ No emergency contacts configured.");
            return;
        }
        
        System.out.println("✅ Call sent to contact: " + userData.emergencyContact());
    }

    @Override
    public String getAlertType() {
        return "Phone Call";
    }

    private String formatAlertMessage(EmergencyEvent event) {
        return String.format(
            "[%s] EMERGENCY ALERT\nType: %s\nLocation: %s\nSeverity: %d/10",
            event.timestamp().format(TIMESTAMP_FORMAT),
            event.emergencyType(),
            event.location(),
            event.severityLevel()
        );
    }

    private boolean simulateEmergencyCall(EmergencyEvent event) {
        System.out.println("\nConnecting to " + EMERGENCY_NUMBER + "...");
        try {
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(500);
            }
            System.out.println("\n✅ Connection established!");
            System.out.println("Emergency: " + event.emergencyType());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

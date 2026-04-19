package com.soteria.infrastructure.notification;

import com.soteria.core.model.EmergencyEvent;
import com.soteria.core.model.UserData;
import com.soteria.core.interfaces.AlertService;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Class responsible for sending emergency notifications to the corresponding services.
 * Implements the AlertService interface, allowing for polymorphism and easy extension.
 */
public class AlertSender implements AlertService {
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
        
        System.out.println("\n=== ALERT SENT ===");
        System.out.println(alertMessage);
        
        try (FileWriter writer = new FileWriter(ALERTS_FILE, true)) {
            writer.write("-".repeat(80) + "\n");
            writer.write(alertMessage + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("❌ Error saving alert to file: " + e.getMessage());
            return false;
        }
        
        return simulateEmergencyServiceCall(event);
    }

    @Override
    public void notifyContacts(UserData userData, EmergencyEvent event) {
        System.out.println("\nNotifying emergency contacts...");
        
        if (userData == null || userData.emergencyContact() == null || userData.emergencyContact().isEmpty()) {
            System.out.println("⚠️ No emergency contacts configured.");
            return;
        }
        
        System.out.println("✅ A notification has been sent to emergency contacts with the following data:");
        System.out.println("Emergency Type: " + event.emergencyType());
        System.out.println("Location: " + event.location());
        System.out.println("Event Time: " + event.timestamp().format(TIMESTAMP_FORMAT));
    }

    @Override
    public String getAlertType() {
        return "Emergency Alert System";
    }

    private String formatAlertMessage(EmergencyEvent event) {
        return String.format(
            "[%s] EMERGENCY ALERT\n" +
            "Type: %s\n" +
            "Location: %s\n" +
            "Severity Level: %d/10\n" +
            "Event Time: %s\n" +
            "\nUSER INFORMATION:\n%s",
            event.timestamp().format(TIMESTAMP_FORMAT),
            event.emergencyType(),
            event.location(),
            event.severityLevel(),
            event.timestamp().format(TIMESTAMP_FORMAT),
            event.userData()
        );
    }

    private boolean simulateEmergencyServiceCall(EmergencyEvent event) {
        System.out.println("\nConnecting to emergency service " + EMERGENCY_NUMBER + "...");
        
        try {
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(500);
            }
            System.out.println("\n\n✅ Connection established with emergency services!");
            System.out.println("Operator: What is your emergency?");
            System.out.println("System: An emergency of type " + event.emergencyType() + " has been detected.");
            System.out.println("Location: " + event.location());
            System.out.println("\n✅ Help is on the way!");
            
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("\n❌ Error connecting to emergency services: " + e.getMessage());
            return false;
        }
    }
}

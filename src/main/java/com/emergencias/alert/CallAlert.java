package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import com.emergencias.services.IAlert;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Implementación de IAlert para alertas telefónicas estándar.
 * Demuestra el uso de interfaces e implementación del patrón Strategy.
 */
public class CallAlert implements IAlert {
    private static final String EMERGENCY_NUMBER = "112";
    private static final String ALERTS_FILE = "emergency_alerts.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean send(EmergencyEvent event) {
        if (event == null) {
            System.err.println("❌ Error: No se puede enviar una alerta nula");
            return false;
        }

        String alertMessage = formatAlertMessage(event);
        
        System.out.println("\n=== ALERTA DE LLAMADA ENVIADA ===");
        System.out.println(alertMessage);
        
        try (FileWriter writer = new FileWriter(ALERTS_FILE, true)) {
            writer.write("-".repeat(80) + "\n");
            writer.write("[LLAMADA] " + alertMessage + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("❌ Error al guardar la alerta: " + e.getMessage());
            return false;
        }
        
        return simulateEmergencyCall(event);
    }

    @Override
    public void notifyContacts(UserData userData, EmergencyEvent event) {
        System.out.println("\nNotificando a contactos por llamada...");
        if (userData == null || userData.getEmergencyContact().isEmpty()) {
            System.out.println("⚠️  No hay contactos de emergencia configurados.");
            return;
        }
        
        System.out.println("✅ Llamada enviada al contacto: " + userData.getEmergencyContact());
    }

    @Override
    public String getAlertType() {
        return "Llamada Telefónica";
    }

    private String formatAlertMessage(EmergencyEvent event) {
        return String.format(
            "[%s] ALERTA DE EMERGENCIA\nTipo: %s\nUbicación: %s\nGravedad: %d/10",
            event.getTimestamp().format(TIMESTAMP_FORMAT),
            event.getEmergencyType(),
            event.getLocation(),
            event.getSeverityLevel()
        );
    }

    private boolean simulateEmergencyCall(EmergencyEvent event) {
        System.out.println("\nConectando con " + EMERGENCY_NUMBER + "...");
        try {
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(500);
            }
            System.out.println("\n✅ ¡Conexión establecida!");
            System.out.println("Emergencia: " + event.getEmergencyType());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

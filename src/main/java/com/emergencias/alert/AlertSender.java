package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Clase encargada de enviar notificaciones de emergencia a los servicios correspondientes.
 */
public class AlertSender {
    // Constantes de configuración
    private static final String EMERGENCY_NUMBER = "112";  // Número de emergencias estándar
    private static final String ALERTS_FILE = "emergency_alerts.log";  // Archivo de registro de alertas
    
    // Formato para las marcas de tiempo en los registros
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
      Envía una alerta de emergencia a los servicios correspondientes.
     */
    public boolean sendAlert(EmergencyEvent event) {
        // Validar entrada
        if (event == null) {
            System.err.println("Error: No se puede enviar una alerta nula");
            return false;
        }

        // Formatear el mensaje de alerta
        String alertMessage = formatAlertMessage(event);
        
        // 1. Mostrar en consola para confirmación inmediata
        System.out.println("\n=== ALERTA ENVIADA ===");
        System.out.println(alertMessage);
        
        // 2. Guardar en archivo de registro para auditoría
        try (FileWriter writer = new FileWriter(ALERTS_FILE, true)) {
            writer.write("-".repeat(80) + "\n");
            writer.write(alertMessage + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error al guardar la alerta en el archivo: " + e.getMessage());
            return false;
        }
        
        // 3. Simular envío a servicios de emergencia
        return simulateEmergencyServiceCall(event);
    }

    /**
     * Formatea el mensaje de alerta con los detalles de la emergencia.
     */
    private String formatAlertMessage(EmergencyEvent event) {
        return String.format(
            "[%s] ALERTA DE EMERGENCIA\n" +
            "Tipo: %s\n" +
            "Ubicación: %s\n" +
            "Nivel de gravedad: %d/10\n" +
            "Hora del evento: %s\n" +
            "\nINFORMACIÓN DEL USUARIO:\n%s",
            event.getTimestamp().format(TIMESTAMP_FORMAT),
            event.getEmergencyType(),
            event.getLocation(),
            event.getSeverityLevel(),
            event.getTimestamp().format(TIMESTAMP_FORMAT),
            event.getUserData()
        );
    }

    /**
     * Simula una llamada al servicio de emergencias.
     */
    private boolean simulateEmergencyServiceCall(EmergencyEvent event) {
        System.out.println("\nConectando con el servicio de emergencias " + EMERGENCY_NUMBER + "...");
        
        // Simular tiempo de conexión
        try {
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(500);
            }
            System.out.println("\n\n¡Conexión establecida con el servicio de emergencias!");
            System.out.println("Operador: ¿Cuál es su emergencia?");
            System.out.println("Sistema: Se ha detectado una emergencia de tipo: " + event.getEmergencyType());
            System.out.println("Ubicación: " + event.getLocation());
            System.out.println("\n¡Ayuda en camino! Se ha notificado a los servicios de emergencia.");
            
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("\nError al conectar con el servicio de emergencias: " + e.getMessage());
            return false;
        }
    }

    /**
     * Notifica a los contactos de emergencia del usuario.
     *
     */
    public void notifyEmergencyContacts(String userData, EmergencyEvent event) {
        // Implementación básica que podría extenderse para enviar SMS/emails reales
        System.out.println("\nNotificando a contactos de emergencia...");
        
        if (userData == null || userData.isEmpty()) {
            System.out.println("No hay contactos de emergencia configurados.");
            return;
        }
        
        System.out.println("Se ha enviado una notificación a los contactos de emergencia con los siguientes datos:");
        System.out.println("Tipo de emergencia: " + event.getEmergencyType());
        System.out.println("Ubicación: " + event.getLocation());
        System.out.println("Hora del evento: " + event.getTimestamp().format(TIMESTAMP_FORMAT));

    }
}

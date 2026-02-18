package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import com.emergencias.services.IAlert;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * <h1>Gestor de Envío de Alertas</h1>
 *
 * <p>
 * Esta clase es una implementación concreta de la interfaz {@link IAlert}.
 * Su responsabilidad es gestionar el proceso de notificación cuando se
 * produce una emergencia.
 * </p>
 *
 * <p>
 * El proceso incluye:
 * <ol>
 *     <li>Formatear y mostrar la alerta en consola.</li>
 *     <li>Guardar un registro de la alerta en un archivo de log para auditoría.</li>
 *     <li>Simular la llamada a los servicios de emergencia (ej. 112).</li>
 *     <li>Notificar a los contactos de emergencia del usuario.</li>
 * </ol>
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class AlertSender implements IAlert {
    // --- CONSTANTES DE CONFIGURACIÓN ---
    private static final String EMERGENCY_NUMBER = "112";
    private static final String ALERTS_FILE = "logs/emergency_alerts.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Envía la alerta principal de la emergencia.
     * <p>
     * Este método simula el envío de la información a un centro de coordinación
     * de emergencias. Muestra la información, la registra en un archivo y
     * simula la llamada.
     * </p>
     *
     * @param event El evento de emergencia que contiene todos los detalles.
     * @return {@code true} si la alerta se envió (simuló) con éxito,
     *         {@code false} si ocurrió algún error.
     */
    @Override
    public boolean send(EmergencyEvent event) {
        if (event == null) {
            System.err.println("❌ Error: No se puede enviar una alerta para un evento nulo.");
            return false;
        }

        String alertMessage = formatAlertMessage(event);
        
        // 1. Muestra la alerta en consola para feedback inmediato al usuario.
        System.out.println("\n=== ALERTA ENVIADA A SERVICIOS DE EMERGENCIA ===");
        System.out.println(alertMessage);
        
        // 2. Registra la alerta en un archivo de log para persistencia y auditoría.
        logAlertToFile(alertMessage);
        
        // 3. Simula la comunicación con el servicio de emergencias.
        return simulateEmergencyServiceCall(event);
    }

    /**
     * Notifica a los contactos de emergencia del usuario.
     * <p>
     * Simula el envío de un mensaje o notificación a los contactos personales
     * configurados en {@link UserData}.
     * </p>
     *
     * @param userData Los datos del usuario que contienen la información del contacto.
     * @param event    El evento de emergencia con los detalles a notificar.
     */
    @Override
    public void notifyContacts(UserData userData, EmergencyEvent event) {
        System.out.println("\n--- Notificando a Contactos de Emergencia ---");
        
        if (userData == null || userData.getEmergencyContact() == null || userData.getEmergencyContact().isEmpty()) {
            System.out.println("⚠️  No hay contactos de emergencia configurados para notificar.");
            return;
        }
        
        System.out.println("✅ Notificación enviada a: " + userData.getEmergencyContact());
        System.out.println("   Detalles enviados:");
        System.out.println("   - Tipo de emergencia: " + event.getEmergencyType());
        System.out.println("   - Ubicación: " + event.getLocation());
        System.out.println("-------------------------------------------");
    }

    /**
     * Devuelve el tipo de sistema de alerta que representa esta clase.
     *
     * @return Una cadena que describe el sistema de alerta.
     */
    @Override
    public String getAlertType() {
        return "Sistema de Alertas de Emergencia Estándar";
    }

    /**
     * Formatea un mensaje de alerta legible a partir de un evento de emergencia.
     *
     * @param event El evento de emergencia.
     * @return Una cadena de texto con todos los detalles formateados.
     */
    private String formatAlertMessage(EmergencyEvent event) {
        return String.format(
            "[%s] ALERTA DE EMERGENCIA\n" +
            "Tipo: %s\n" +
            "Ubicación: %s\n" +
            "Nivel de gravedad: %d/10\n" +
            "\n--- INFORMACIÓN DEL USUARIO ---\n%s",
            event.getTimestamp().format(TIMESTAMP_FORMAT),
            event.getEmergencyType(),
            event.getLocation(),
            event.getSeverityLevel(),
            event.getUserData()
        );
    }

    /**
     * Escribe el mensaje de alerta en un archivo de log.
     *
     * @param alertMessage El mensaje de alerta formateado.
     */
    private void logAlertToFile(String alertMessage) {
        try (FileWriter writer = new FileWriter(ALERTS_FILE, true)) {
            writer.write("=".repeat(80) + "\n");
            writer.write(alertMessage + "\n\n");
        } catch (IOException e) {
            System.err.println("❌ Error crítico: No se pudo guardar la alerta en el archivo de log: " + e.getMessage());
        }
    }

    /**
     * Simula una llamada a un servicio de emergencias como el 112.
     * <p>
     * Utiliza {@code Thread.sleep} para dar la sensación de un proceso que
     * toma tiempo, como establecer una conexión.
     * </p>
     *
     * @param event El evento de emergencia a reportar.
     * @return {@code true} si la simulación se completa, {@code false} si es interrumpida.
     */
    private boolean simulateEmergencyServiceCall(EmergencyEvent event) {
        System.out.println("\nConectando con el servicio de emergencias " + EMERGENCY_NUMBER + "...");
        
        try {
            // Simula un pequeño retardo en la conexión.
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(400);
            }
            System.out.println("\n\n✅ ¡Conexión establecida con el centro de emergencias!");
            System.out.println("   Operador: \"Emergencias, ¿cuál es su situación?\"");
            System.out.println("   Sistema: \"Se reporta una emergencia automatizada.\"");
            System.out.println("   - Tipo: " + event.getEmergencyType());
            System.out.println("   - Ubicación: " + event.getLocation());
            System.out.println("\n✅ ¡Ayuda en camino! Los servicios de emergencia han sido despachados.");
            
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura el estado de interrupción.
            System.err.println("\n❌ Error: La conexión con el servicio de emergencias fue interrumpida.");
            return false;
        }
    }
}

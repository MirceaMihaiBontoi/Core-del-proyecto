package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import com.emergencias.services.IAlert;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * <h1>Implementación de Alerta por Llamada Telefónica</h1>
 *
 * <p>
 * Esta clase es una implementación específica de la interfaz {@link IAlert},
 * diseñada para simular el envío de una alerta de emergencia a través de una
 * llamada telefónica.
 * </p>
 *
 * <p>
 * Como parte del <b>Patrón Strategy</b>, esta clase podría ser reemplazada por
 * otras implementaciones (como {@code SMSAlert}) sin que el resto del sistema
 * se vea afectado.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class CallAlert implements IAlert {
    // --- CONSTANTES ---
    private static final String EMERGENCY_NUMBER = "112";
    private static final String ALERTS_FILE = "logs/call_alerts.log"; // Archivo de log específico para llamadas
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Envía una alerta simulando una llamada a los servicios de emergencia.
     * <p>
     * El proceso incluye formatear el mensaje, registrarlo en un archivo de log
     * y simular la conexión y comunicación con el operador del 112.
     * </p>
     *
     * @param event El evento de emergencia con los detalles a comunicar.
     * @return {@code true} si la simulación de la llamada fue exitosa, {@code false} si no.
     */
    @Override
    public boolean send(EmergencyEvent event) {
        if (event == null) {
            System.err.println("❌ Error: No se puede realizar una llamada para un evento nulo.");
            return false;
        }

        String alertMessage = formatAlertMessage(event);
        
        System.out.println("\n=== INICIANDO LLAMADA DE EMERGENCIA ===");
        System.out.println(alertMessage);
        
        // Registrar la alerta de llamada en su propio archivo de log.
        logAlertToFile(alertMessage);
        
        // Simular la llamada al 112.
        return simulateEmergencyCall(event);
    }

    /**
     * Notifica a los contactos personales simulando una llamada.
     *
     * @param userData Los datos del usuario que contienen el contacto de emergencia.
     * @param event    El evento de emergencia (no se usa en esta implementación simple).
     */
    @Override
    public void notifyContacts(UserData userData, EmergencyEvent event) {
        System.out.println("\n--- Realizando llamada a contactos de emergencia... ---");
        if (userData == null || userData.getEmergencyContact() == null || userData.getEmergencyContact().isEmpty()) {
            System.out.println("⚠️  No hay contactos de emergencia para llamar.");
            return;
        }
        
        System.out.println("✅ Llamada de notificación realizada con éxito a: " + userData.getEmergencyContact());
        System.out.println("----------------------------------------------------");
    }

    /**
     * Devuelve el tipo de alerta que representa esta clase.
     *
     * @return La cadena "Llamada Telefónica".
     */
    @Override
    public String getAlertType() {
        return "Llamada Telefónica";
    }

    /**
     * Formatea el mensaje que se comunicaría en la llamada de emergencia.
     *
     * @param event El evento de emergencia.
     * @return Una cadena de texto formateada con los detalles clave.
     */
    private String formatAlertMessage(EmergencyEvent event) {
        return String.format(
            "[%s] ALERTA DE EMERGENCIA\n" +
            "  - Tipo: %s\n" +
            "  - Ubicación: %s\n" +
            "  - Gravedad: %d/10",
            event.getTimestamp().format(TIMESTAMP_FORMAT),
            event.getEmergencyType(),
            event.getLocation(),
            event.getSeverityLevel()
        );
    }
    
    /**
     * Escribe el mensaje de la alerta de llamada en un archivo de log.
     */
    private void logAlertToFile(String alertMessage) {
        try (FileWriter writer = new FileWriter(ALERTS_FILE, true)) {
            writer.write("=".repeat(80) + "\n");
            writer.write(alertMessage + "\n\n");
        } catch (IOException e) {
            System.err.println("❌ Error crítico: No se pudo registrar la llamada en el log: " + e.getMessage());
        }
    }

    /**
     * Simula el proceso de una llamada al 112, con retardos para el realismo.
     *
     * @param event El evento a reportar.
     * @return {@code true} si la simulación es exitosa, {@code false} si es interrumpida.
     */
    private boolean simulateEmergencyCall(EmergencyEvent event) {
        System.out.println("\nMarcando " + EMERGENCY_NUMBER + "...");
        try {
            // Simula el tiempo de espera mientras suena el teléfono.
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(500);
            }
            System.out.println("\n✅ ¡Conexión establecida con el operador!");
            System.out.println("   Sistema: \"Se reporta una emergencia de tipo " + event.getEmergencyType() + " en " + event.getLocation() + ".\"");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Buena práctica: restaurar el estado de interrupción.
            System.err.println("\n❌ La llamada de emergencia fue interrumpida.");
            return false;
        }
    }
}

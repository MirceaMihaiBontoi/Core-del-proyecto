package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserFeedback;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Scanner;

/**
 * Clase encargada de registrar y almacenar el historial de emergencias y feedback del usuario.
 * 
 * Proporciona funcionalidad para:
 * - Guardar cada interacción de emergencia
 * - Registrar feedback del usuario
 * - Mantener un historial de operaciones
 */
public class EmergencyLogger {
    private static final String HISTORY_FILE = "emergency_history.log";
    private static final String FEEDBACK_FILE = "user_feedback.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Registra una emergencia en el historial.
     * 
     * @param event Evento de emergencia a registrar
     * @return ID único de la emergencia
     */
    public String logEmergency(EmergencyEvent event) {
        String emergencyId = UUID.randomUUID().toString();
        String logEntry = String.format(
            "[%s] ID: %s | Tipo: %s | Ubicación: %s | Gravedad: %s%n",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            emergencyId,
            event.getEmergencyType(),
            event.getLocation(),
            event.getSeverityLevel()
        );

        try (FileWriter writer = new FileWriter(HISTORY_FILE, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Error al registrar emergencia: " + e.getMessage());
        }

        return emergencyId;
    }

    /**
     * Solicita y registra el feedback del usuario sobre una emergencia.
     * 
     * @param emergencyId ID de la emergencia
     * @param scanner Scanner compartido para entrada del usuario
     * @return UserFeedback con la evaluación del usuario
     */
    public UserFeedback collectAndLogFeedback(String emergencyId, Scanner scanner) {
        System.out.println("\n--- Solicitud de Feedback ---");
        System.out.println("¿Cómo fue tu experiencia? (1-5, donde 5 es excelente): ");
        int rating = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer

        System.out.println("¿Tienes algún comentario adicional?");
        String comments = scanner.nextLine();

        UserFeedback feedback = new UserFeedback(emergencyId, rating, comments);
        logFeedback(feedback);

        return feedback;
    }

    /**
     * Registra el feedback en el archivo de log.
     * 
     * @param feedback Feedback del usuario a registrar
     */
    private void logFeedback(UserFeedback feedback) {
        String logEntry = String.format(
            "[%s] ID Emergencia: %s | Puntuación: %d/5 | Comentarios: %s%n",
            feedback.getFeedbackTime().format(TIMESTAMP_FORMAT),
            feedback.getEmergencyId(),
            feedback.getSatisfactionRating(),
            feedback.getComments()
        );

        try (FileWriter writer = new FileWriter(FEEDBACK_FILE, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Error al registrar feedback: " + e.getMessage());
        }
    }
}

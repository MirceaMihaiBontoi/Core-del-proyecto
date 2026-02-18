package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserFeedback;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Clase encargada de registrar y almacenar el historial de emergencias y feedback del usuario.
 */
public class EmergencyLogger {
    // Cambiamos el archivo de historial a formato JSON
    private static final String HISTORY_FILE = "logs/emergency_history.json";
    private static final String FEEDBACK_FILE = "logs/user_feedback.log";
    private static final String LOGS_DIR = "logs";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    /**
     * Constructor que asegura que la carpeta de logs existe y configura Jackson.
     */
    public EmergencyLogger() {
        createLogsDirectoryIfNotExists();
        
        // Configurar ObjectMapper para que funcione con JSON
        this.objectMapper = new ObjectMapper();
        // Para que el JSON de salida sea legible (pretty print)
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Para que Jackson entienda y formatee correctamente las fechas (LocalDateTime)
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Crea la carpeta logs/ si no existe.
     */
    private static void createLogsDirectoryIfNotExists() {
        File logsDir = new File(LOGS_DIR);
        if (!logsDir.exists()) {
            if (logsDir.mkdirs()) {
                System.out.println("ℹ️  Carpeta 'logs' creada automáticamente.");
            } else {
                System.err.println("⚠️  No se pudo crear la carpeta 'logs'.");
            }
        }
    }

    /**
     * Registra una emergencia en el historial JSON.
     * 
     * @param event Evento de emergencia a registrar
     * @return ID único de la emergencia
     */
    public String logEmergency(EmergencyEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento de emergencia no puede ser nulo");
        }
        
        // 1. Generar un ID único y asignarlo al evento
        String emergencyId = UUID.randomUUID().toString();
        event.setId(emergencyId);

        try {
            File file = new File(HISTORY_FILE);
            List<EmergencyEvent> emergencies;

            // 2. Si el archivo existe y no está vacío, leer la lista actual de emergencias
            if (file.exists() && file.length() > 0) {
                emergencies = objectMapper.readValue(file, new TypeReference<List<EmergencyEvent>>() {});
            } else {
                // Si no, crear una nueva lista
                emergencies = new ArrayList<>();
            }

            // 3. Añadir el nuevo evento a la lista
            emergencies.add(event);

            // 4. Escribir la lista completa de nuevo en el archivo JSON
            objectMapper.writeValue(file, emergencies);

        } catch (IOException e) {
            System.err.println("❌ Error al registrar emergencia en JSON: " + e.getMessage());
            throw new RuntimeException("Error al escribir en el archivo de historial JSON", e);
        }

        return emergencyId;
    }

    /**
     * Solicita y registra el feedback del usuario sobre una emergencia.
     * (Este método no se modifica, sigue guardando en un .log simple)
     */
    public UserFeedback collectAndLogFeedback(String emergencyId, Scanner scanner) {
        if (emergencyId == null || emergencyId.isEmpty()) {
            throw new IllegalArgumentException("El ID de emergencia no puede ser nulo o vacío");
        }
        
        try {
            System.out.println("\n--- Solicitud de Feedback ---");
            System.out.println("¿Cómo fue tu experiencia? (1-5, donde 5 es excelente): ");
            
            int rating = -1;
            while (rating < 1 || rating > 5) {
                try {
                    rating = scanner.nextInt();
                    if (rating < 1 || rating > 5) {
                        System.out.println("⚠️  Por favor, ingrese un valor entre 1 y 5.");
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Entrada inválida. Intente nuevamente.");
                    scanner.nextLine(); // Limpiar buffer
                    rating = -1;
                }
            }
            
            scanner.nextLine(); // Limpiar buffer

            System.out.println("¿Tienes algún comentario adicional?");
            String comments = scanner.nextLine().trim();
            if (comments.isEmpty()) {
                comments = "Sin comentarios";
            }

            UserFeedback feedback = new UserFeedback(emergencyId, rating, comments);
            logFeedback(feedback);

            return feedback;
            
        } catch (Exception e) {
            System.err.println("❌ Error al recopilar feedback: " + e.getMessage());
            throw new RuntimeException("Error al procesar el feedback del usuario", e);
        }
    }

    /**
     * Registra el feedback en el archivo de log.
     */
    private void logFeedback(UserFeedback feedback) {
        if (feedback == null) {
            System.err.println("❌ Error: No se puede registrar un feedback nulo");
            return;
        }
        
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
            System.err.println("❌ Error al registrar feedback: " + e.getMessage());
        }
    }
}

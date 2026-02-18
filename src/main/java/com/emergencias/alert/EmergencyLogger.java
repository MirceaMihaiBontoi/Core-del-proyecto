package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserFeedback;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Clase encargada de registrar y almacenar el historial de emergencias y feedback del usuario en formato JSON.
 */
public class EmergencyLogger {
    private static final String HISTORY_FILE = "logs/emergency_history.json";
    // CAMBIO: El archivo de feedback ahora también es JSON
    private static final String FEEDBACK_FILE = "logs/user_feedback.json";
    private static final String LOGS_DIR = "logs";

    private final ObjectMapper objectMapper;

    /**
     * Constructor que asegura que la carpeta de logs existe y configura Jackson.
     */
    public EmergencyLogger() {
        createLogsDirectoryIfNotExists();
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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
     */
    public String logEmergency(EmergencyEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento de emergencia no puede ser nulo");
        }
        
        String emergencyId = UUID.randomUUID().toString();
        event.setId(emergencyId);

        try {
            File file = new File(HISTORY_FILE);
            List<EmergencyEvent> emergencies;

            if (file.exists() && file.length() > 0) {
                emergencies = objectMapper.readValue(file, new TypeReference<List<EmergencyEvent>>() {});
            } else {
                emergencies = new ArrayList<>();
            }

            emergencies.add(event);
            objectMapper.writeValue(file, emergencies);

        } catch (IOException e) {
            System.err.println("❌ Error al registrar emergencia en JSON: " + e.getMessage());
            throw new RuntimeException("Error al escribir en el archivo de historial JSON", e);
        }

        return emergencyId;
    }

    /**
     * Solicita y registra el feedback del usuario sobre una emergencia.
     */
    public UserFeedback collectAndLogFeedback(String emergencyId, Scanner scanner) {
        if (emergencyId == null || emergencyId.isEmpty()) {
            throw new IllegalArgumentException("El ID de emergencia no puede ser nulo o vacío");
        }
        
        try {
            System.out.println("\n--- Solicitud de Feedback ---");
            System.out.print("¿Cómo fue tu experiencia? (1-5, donde 5 es excelente): ");
            
            int rating = -1;
            while (rating < 1 || rating > 5) {
                try {
                    // Usamos nextLine para evitar problemas con el buffer del scanner
                    String line = scanner.nextLine();
                    rating = Integer.parseInt(line);
                    if (rating < 1 || rating > 5) {
                        System.out.print("⚠️  Por favor, ingrese un valor entre 1 y 5: ");
                    }
                } catch (NumberFormatException e) {
                    System.out.print("⚠️  Entrada inválida. Por favor, ingrese un número: ");
                    rating = -1;
                }
            }

            System.out.print("¿Tienes algún comentario adicional? (Opcional): ");
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
     * Registra el feedback en el archivo JSON.
     */
    private void logFeedback(UserFeedback feedback) {
        if (feedback == null) {
            System.err.println("❌ Error: No se puede registrar un feedback nulo");
            return;
        }
        
        try {
            File file = new File(FEEDBACK_FILE);
            List<UserFeedback> feedbacks;

            if (file.exists() && file.length() > 0) {
                feedbacks = objectMapper.readValue(file, new TypeReference<List<UserFeedback>>() {});
            } else {
                feedbacks = new ArrayList<>();
            }

            feedbacks.add(feedback);
            objectMapper.writeValue(file, feedbacks);

        } catch (IOException e) {
            System.err.println("❌ Error al registrar feedback en JSON: " + e.getMessage());
        }
    }
}

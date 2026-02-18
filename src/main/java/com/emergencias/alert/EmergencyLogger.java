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
 * <h1>Gestor de Registros de Emergencias y Feedback</h1>
 *
 * <p>
 * Esta clase se encarga de la persistencia de los datos generados durante la
 * ejecución del programa. Guarda tanto los eventos de emergencia como el
 * feedback de los usuarios en archivos JSON estructurados.
 * </p>
 *
 * <p>
 * Utiliza la librería Jackson para la serialización y deserialización de objetos Java a JSON.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class EmergencyLogger {
    // --- RUTAS DE LOS ARCHIVOS DE LOG ---
    private static final String HISTORY_FILE = "logs/emergency_history.json";
    private static final String FEEDBACK_FILE = "logs/user_feedback.json";
    private static final String LOGS_DIR = "logs";

    // ObjectMapper de Jackson, configurado para ser reutilizado.
    private final ObjectMapper objectMapper;

    /**
     * Constructor que inicializa el logger.
     * <p>
     * Se encarga de crear el directorio de logs si no existe y de configurar
     * el {@link ObjectMapper} para que funcione con fechas de Java 8 y genere
     * un JSON formateado (legible).
     * </p>
     */
    public EmergencyLogger() {
        createLogsDirectory();
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // JSON legible
        this.objectMapper.registerModule(new JavaTimeModule()); // Soporte para LocalDateTime
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Usar formato de texto para fechas
    }

    /**
     * Registra un evento de emergencia en el archivo {@code emergency_history.json}.
     * <p>
     * El método lee el archivo JSON existente, lo convierte en una lista de eventos,
     * añade el nuevo evento y vuelve a escribir la lista completa en el archivo.
     * Asigna un ID único al evento antes de guardarlo.
     * </p>
     *
     * @param event El evento de emergencia a registrar.
     * @return El ID único generado para esta emergencia.
     * @throws RuntimeException si ocurre un error de I/O al escribir el archivo.
     */
    public String logEmergency(EmergencyEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento de emergencia no puede ser nulo.");
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
            System.err.println("❌ Error al registrar la emergencia en JSON: " + e.getMessage());
            throw new RuntimeException("Error al escribir en el archivo de historial JSON.", e);
        }

        return emergencyId;
    }

    /**
     * Recopila y registra el feedback del usuario para una emergencia específica.
     *
     * @param emergencyId El ID de la emergencia sobre la que se está dando feedback.
     * @param scanner     La instancia compartida de Scanner para leer la entrada.
     * @return El objeto {@link UserFeedback} creado.
     * @throws RuntimeException si ocurre un error durante la recopilación.
     */
    public UserFeedback collectAndLogFeedback(String emergencyId, Scanner scanner) {
        if (emergencyId == null || emergencyId.isEmpty()) {
            throw new IllegalArgumentException("El ID de emergencia no puede ser nulo o vacío.");
        }
        
        try {
            System.out.println("\n--- Solicitud de Feedback ---");
            
            // Recopilar puntuación
            System.out.print("Para mejorar, ¿cómo valoraría la experiencia (1-5)? ");
            int rating = getRatingFromUser(scanner);

            // Recopilar comentarios
            System.out.print("¿Algún comentario adicional? (Opcional): ");
            String comments = scanner.nextLine().trim();
            if (comments.isEmpty()) {
                comments = "Sin comentarios.";
            }

            UserFeedback feedback = new UserFeedback(emergencyId, rating, comments);
            logFeedback(feedback);

            return feedback;
            
        } catch (Exception e) {
            System.err.println("❌ Error al recopilar el feedback: " + e.getMessage());
            throw new RuntimeException("Error al procesar el feedback del usuario.", e);
        }
    }

    /**
     * Registra un objeto de feedback en el archivo {@code user_feedback.json}.
     * <p>
     * Sigue la misma lógica que {@code logEmergency}: lee la lista, añade el
     * nuevo feedback y reescribe el archivo.
     * </p>
     *
     * @param feedback El objeto de feedback a registrar.
     */
    private void logFeedback(UserFeedback feedback) {
        if (feedback == null) {
            System.err.println("❌ Error: No se puede registrar un feedback nulo.");
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
            System.err.println("❌ Error al registrar el feedback en JSON: " + e.getMessage());
        }
    }

    /**
     * Crea el directorio 'logs' si no existe.
     */
    private void createLogsDirectory() {
        File logsDir = new File(LOGS_DIR);
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            System.err.println("⚠️  Advertencia: No se pudo crear el directorio 'logs'.");
        }
    }

    /**
     * Método auxiliar para leer y validar la puntuación del usuario.
     */
    private int getRatingFromUser(Scanner scanner) {
        int rating = -1;
        while (rating < 1 || rating > 5) {
            try {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) { // Permite al usuario presionar Enter para saltar
                    System.out.println("Feedback omitido.");
                    return -1; // Valor para indicar que se omitió
                }
                rating = Integer.parseInt(line);
                if (rating < 1 || rating > 5) {
                    System.out.print("⚠️  Por favor, ingrese un valor entre 1 y 5: ");
                }
            } catch (NumberFormatException e) {
                System.out.print("⚠️  Entrada inválida. Por favor, ingrese un número: ");
                rating = -1;
            }
        }
        return rating;
    }
}

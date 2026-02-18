package com.emergencias.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * <h1>Entidad de Feedback del Usuario</h1>
 *
 * <p>
 * Esta clase (POJO) encapsula la información de feedback proporcionada por un
 * usuario después de un evento de emergencia. Se utiliza para registrar la
 * satisfacción y los comentarios para futuras mejoras del sistema.
 * </p>
 *
 * <p>
 * Está diseñada para ser serializada a JSON por la librería Jackson.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class UserFeedback {
    // --- ATRIBUTOS ---
    private String emergencyId;      // ID de la emergencia a la que se refiere este feedback
    private int satisfactionRating;  // Puntuación de satisfacción (ej. 1-5)
    private String comments;         // Comentarios adicionales del usuario
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime feedbackTime; // Marca de tiempo de cuándo se dio el feedback

    /**
     * Constructor sin argumentos, requerido por Jackson para la deserialización.
     */
    public UserFeedback() {}

    /**
     * Constructor principal para crear un nuevo objeto de feedback.
     *
     * @param emergencyId        El ID de la emergencia asociada.
     * @param satisfactionRating La puntuación dada por el usuario.
     * @param comments           Los comentarios textuales del usuario.
     */
    public UserFeedback(String emergencyId, int satisfactionRating, String comments) {
        this.emergencyId = emergencyId;
        this.satisfactionRating = satisfactionRating;
        this.comments = comments;
        this.feedbackTime = LocalDateTime.now(); // Se asigna la fecha y hora actual.
    }

    // --- GETTERS Y SETTERS ---
    // Necesarios para que Jackson pueda acceder y modificar los campos.
    public String getEmergencyId() { return emergencyId; }
    public void setEmergencyId(String emergencyId) { this.emergencyId = emergencyId; }
    public int getSatisfactionRating() { return satisfactionRating; }
    public void setSatisfactionRating(int satisfactionRating) { this.satisfactionRating = satisfactionRating; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getFeedbackTime() { return feedbackTime; }
    public void setFeedbackTime(LocalDateTime feedbackTime) { this.feedbackTime = feedbackTime; }

    /**
     * Devuelve una representación en cadena del feedback.
     *
     * @return Una cadena formateada con los detalles del feedback.
     */
    @Override
    public String toString() {
        return String.format(
            "Feedback para Emergencia ID: %s [Puntuación: %d/5, Comentarios: \"%s\", Fecha: %s]",
            emergencyId, satisfactionRating, comments, feedbackTime
        );
    }
}

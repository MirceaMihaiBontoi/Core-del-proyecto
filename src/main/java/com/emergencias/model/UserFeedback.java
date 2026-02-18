package com.emergencias.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Clase que representa el feedback del usuario sobre una emergencia.
 */
public class UserFeedback {
    private String emergencyId;
    private int satisfactionRating;  // 1-5
    private String comments;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime feedbackTime;

    /**
     * Constructor sin argumentos, requerido por Jackson para la deserialización.
     */
    public UserFeedback() {}

    public UserFeedback(String emergencyId, int satisfactionRating, String comments) {
        this.emergencyId = emergencyId;
        this.satisfactionRating = satisfactionRating;
        this.comments = comments;
        this.feedbackTime = LocalDateTime.now();
    }

    // Getters y Setters
    public String getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(String emergencyId) {
        this.emergencyId = emergencyId;
    }

    public int getSatisfactionRating() {
        return satisfactionRating;
    }

    public void setSatisfactionRating(int satisfactionRating) {
        this.satisfactionRating = satisfactionRating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getFeedbackTime() {
        return feedbackTime;
    }

    public void setFeedbackTime(LocalDateTime feedbackTime) {
        this.feedbackTime = feedbackTime;
    }

    @Override
    public String toString() {
        return "UserFeedback{" +
                "emergencyId='" + emergencyId + '\'' +
                ", satisfactionRating=" + satisfactionRating +
                ", comments='" + comments + '\'' +
                ", feedbackTime=" + feedbackTime +
                '}';
    }
}

package com.emergencias.model;

import java.time.LocalDateTime;

/**
 * Clase que representa el feedback del usuario sobre una emergencia.
 * 
 * Almacena la evaluación y comentarios del usuario después de una emergencia,
 * permitiendo mejorar el sistema basándose en la experiencia del usuario.
 */
public class UserFeedback {
    private String emergencyId;
    private int satisfactionRating;  // 1-5
    private String comments;
    private LocalDateTime feedbackTime;

    public UserFeedback(String emergencyId, int satisfactionRating, String comments) {
        this.emergencyId = emergencyId;
        this.satisfactionRating = satisfactionRating;
        this.comments = comments;
        this.feedbackTime = LocalDateTime.now();
    }

    public String getEmergencyId() {
        return emergencyId;
    }

    public int getSatisfactionRating() {
        return satisfactionRating;
    }

    public String getComments() {
        return comments;
    }

    public LocalDateTime getFeedbackTime() {
        return feedbackTime;
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

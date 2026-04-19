package com.soteria.core.model;

import java.time.LocalDateTime;

/**
 * Record representing user feedback on an emergency event.
 */
public record UserFeedback(
    String emergencyId,
    int satisfactionRating,
    String comments,
    LocalDateTime feedbackTime
) {
    public UserFeedback(String emergencyId, int satisfactionRating, String comments) {
        this(emergencyId, satisfactionRating, comments, LocalDateTime.now());
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

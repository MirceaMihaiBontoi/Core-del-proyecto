package com.soteria.core.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An immutable snapshot of a detected emergency event.
 *
 * <p>The compact constructor enforces invariants at creation time — no null fields,
 * and {@code severityLevel} must be in [1, 10]. A valid instance is always consistent.
 */
public record EmergencyEvent(
    String emergencyType,
    String location,
    int severityLevel,
    LocalDateTime timestamp,
    String userData
) {
    public EmergencyEvent {
        Objects.requireNonNull(emergencyType, "Emergency type cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(userData, "User data cannot be null");

        if (severityLevel < 1 || severityLevel > 10) {
            throw new IllegalArgumentException("Severity level must be between 1 and 10, got: " + severityLevel);
        }
    }

    /** Convenience constructor that sets {@code timestamp} to the current date-time. */
    public EmergencyEvent(String emergencyType, String location, int severityLevel, String userData) {
        this(emergencyType, location, severityLevel, LocalDateTime.now(), userData);
    }

    @Override
    public String toString() {
        return String.format(
            "[%s] Emergency: %s%nLocation: %s%nSeverity: %d%nUser: %s",
            timestamp, emergencyType, location, severityLevel, userData
        );
    }
}

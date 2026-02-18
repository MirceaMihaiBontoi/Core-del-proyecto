package com.emergencias.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Clase que representa un evento de emergencia en el sistema.
 */
public class EmergencyEvent {
    private String id;
    private String emergencyType;
    private String location;
    private int severityLevel;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String userData;

    /**
     * Constructor sin argumentos, requerido por Jackson para la deserialización.
     */
    public EmergencyEvent() {
        // El campo 'timestamp' es final, pero Jackson puede manejarlo.
        // Lo inicializamos a 'now()' como valor por defecto seguro.
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor principal para crear un nuevo evento de emergencia.
     */
    public EmergencyEvent(String emergencyType, String location, int severityLevel, String userData) {
        this.emergencyType = emergencyType;
        this.location = location;
        this.severityLevel = severityLevel;
        this.userData = userData;
        this.timestamp = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(int severityLevel) {
        this.severityLevel = severityLevel;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // No hay setter para timestamp porque es 'final'

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    @Override
    public String toString() {
        return String.format(
            "[%s] Emergencia: %s\nUbicación: %s\nGravedad: %d\nDatos del usuario: %s",
            timestamp, emergencyType, location, severityLevel, userData
        );
    }
}

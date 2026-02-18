package com.emergencias.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Clase que representa un evento de emergencia en el sistema.
 * 
 * Esta clase encapsula toda la información relevante de una emergencia reportada,
 * incluyendo metadatos como la marca de tiempo y la gravedad del incidente.
 */
public class EmergencyEvent {
    // ID único para la emergencia, asignado por el logger
    private String id;
    
    // Tipo de emergencia (ej: "Accidente de tráfico", "Problema médico", etc.)
    private String emergencyType;
    
    // Ubicación donde ocurrió la emergencia (coordenadas o dirección)
    private String location;
    
    // Nivel de gravedad en escala del 1 al 10
    private int severityLevel;
    
    // Marca de tiempo del momento en que se creó el evento
    // La anotación JsonFormat asegura que la fecha se guarde en un formato estándar
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;
    
    // Información del usuario relacionada con la emergencia
    private String userData;

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

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    /**
     * Devuelve una representación en cadena formateada del evento de emergencia.
     */
    @Override
    public String toString() {
        return String.format(
            "[%s] Emergencia: %s\nUbicación: %s\nGravedad: %d\nDatos del usuario: %s",
            timestamp, emergencyType, location, severityLevel, userData
        );
    }
}

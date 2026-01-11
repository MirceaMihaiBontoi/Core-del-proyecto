package com.emergencias.model;

import java.time.LocalDateTime;

/**
 * Clase que representa un evento de emergencia en el sistema.
 * 
 * Esta clase encapsula toda la información relevante de una emergencia reportada,
 * incluyendo metadatos como la marca de tiempo y la gravedad del incidente.
 * 
 * Características principales:
 * - Almacena información detallada sobre el tipo de emergencia
 * - Registra la ubicación exacta del incidente
 * - Incluye un nivel de gravedad cuantificable
 * - Genera automáticamente una marca de tiempo al crear el evento
 * - Proporciona una representación en cadena formateada para su visualización
 */
public class EmergencyEvent {
    // Tipo de emergencia (ej: "Accidente de tráfico", "Problema médico", etc.)
    private String emergencyType;
    
    // Ubicación donde ocurrió la emergencia (coordenadas o dirección)
    private String location;
    
    // Nivel de gravedad en escala del 1 al 10
    private int severityLevel;
    
    // Marca de tiempo del momento en que se creó el evento
    private final LocalDateTime timestamp;
    
    // Información del usuario relacionada con la emergencia
    private String userData;

    /**
     * Constructor principal para crear un nuevo evento de emergencia.
     * 
     * @param emergencyType Tipo de emergencia (no puede ser nulo o vacío)
     * @param location Ubicación de la emergencia (no puede ser nula o vacía)
     * @param severityLevel Nivel de gravedad (1-10)
     * @param userData Información del usuario relacionada con la emergencia
     * @throws IllegalArgumentException si los parámetros no son válidos
     */
    public EmergencyEvent(String emergencyType, String location, int severityLevel, String userData) {
        this.emergencyType = emergencyType;
        this.location = location;
        this.severityLevel = severityLevel;
        this.userData = userData;
        this.timestamp = LocalDateTime.now();
    }

    // Getters y Setters
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
     * 
     * @return Cadena que contiene todos los detalles del evento formateados
     *         para su visualización en consola o registro.
     */
    @Override
    public String toString() {
        return String.format(
            "[%s] Emergencia: %s\nUbicación: %s\nGravedad: %d\nDatos del usuario: %s",
            timestamp, emergencyType, location, severityLevel, userData
        );
    }
}

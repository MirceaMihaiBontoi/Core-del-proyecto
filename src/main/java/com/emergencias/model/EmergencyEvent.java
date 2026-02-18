package com.emergencias.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Importar la clase necesaria

/**
 * <h1>Entidad de Evento de Emergencia</h1>
 *
 * <p>
 * Esta clase (POJO) representa un único evento de emergencia reportado en el sistema.
 * Encapsula todos los detalles relevantes del incidente, como el tipo, la ubicación,
 * la gravedad y la información del usuario asociado.
 * </p>
 *
 * <p>
 * Está diseñada para ser serializada a JSON por la librería Jackson.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.2
 * @since 2023-10-27
 */
public class EmergencyEvent {
    // --- ATRIBUTOS ---
    private String id;
    private String emergencyType;
    private String location;
    private int severityLevel;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty
    private final LocalDateTime timestamp;
    
    private String userData;

    /**
     * Constructor sin argumentos, requerido por Jackson para la deserialización.
     */
    public EmergencyEvent() {
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

    // --- GETTERS Y SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmergencyType() { return emergencyType; }
    public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(int severityLevel) { this.severityLevel = severityLevel; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getUserData() { return userData; }
    public void setUserData(String userData) { this.userData = userData; }

    /**
     * Devuelve una representación en cadena del evento, ideal para logs rápidos o depuración.
     *
     * @return Una cadena formateada con los detalles clave del evento.
     */
    @Override
    public String toString() {
        // CORRECCIÓN: Usar DateTimeFormatter para formatear la fecha correctamente.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format(
            "[%s] Emergencia: %s, Ubicación: %s, Gravedad: %d, Usuario: %s",
            timestamp.format(formatter), 
            emergencyType, location, severityLevel, 
            userData.split("\n")[0]
        );
    }
}

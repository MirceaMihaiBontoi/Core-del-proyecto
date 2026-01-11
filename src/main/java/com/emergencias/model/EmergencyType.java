package com.emergencias.model;

/**
 * Clase abstracta que define el contrato para diferentes tipos de emergencias.
 * Implementa herencia y permite que cada tipo de emergencia tenga su propia lógica.
 * 
 * Ejemplos de subclases:
 * - MedicalEmergency: Emergencias médicas
 * - TrafficAccidentEmergency: Accidentes de tráfico
 * - FireEmergency: Incendios
 * - SecurityEmergency: Problemas de seguridad
 */
public abstract class EmergencyType {
    protected String name;
    protected int priority;  // 1-10, donde 10 es máxima prioridad
    protected String description;
    protected boolean requiresMedicalAssistance;

    /**
     * Constructor de la clase abstracta.
     */
    public EmergencyType(String name, int priority, String description, boolean requiresMedicalAssistance) {
        this.name = name;
        this.priority = priority;
        this.description = description;
        this.requiresMedicalAssistance = requiresMedicalAssistance;
    }

    /**
     * Obtiene la prioridad de la emergencia.
     * Método concreto accesible desde subclases.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Obtiene la descripción de la emergencia.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Obtiene el nombre del tipo de emergencia.
     */
    public String getName() {
        return name;
    }

    /**
     * Verifica si requiere asistencia médica.
     */
    public boolean requiresMedicalAssistance() {
        return requiresMedicalAssistance;
    }

    /**
     * Método abstracto que cada subclase debe implementar.
     * Proporciona el protocolo específico de respuesta para este tipo de emergencia.
     */
    public abstract String getResponseProtocol();

    /**
     * Método abstracto para obtener los servicios requeridos.
     */
    public abstract String[] getRequiredServices();

    @Override
    public String toString() {
        return String.format(
            "Emergencia: %s\nPrioridad: %d/10\nDescripción: %s\nAsistencia médica: %s",
            name, priority, description, 
            requiresMedicalAssistance ? "Sí" : "No"
        );
    }
}

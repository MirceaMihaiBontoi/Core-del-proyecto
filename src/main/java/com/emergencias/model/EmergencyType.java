package com.emergencias.model;

/**
 * <h1>Plantilla para Tipos de Emergencia (Clase Abstracta)</h1>
 *
 * <p>
 * Esta clase abstracta define la estructura y el comportamiento base para
 * cualquier tipo de emergencia en el sistema. Utiliza la herencia para
 * establecer un contrato común que todas las emergencias concretas deben seguir.
 * </p>
 *
 * <p>
 * El objetivo es poder tratar diferentes tipos de emergencias (médica, incendio, etc.)
 * de forma polimórfica, a la vez que cada una puede definir su propio protocolo
 * de respuesta y los servicios que requiere.
 * </p>
 *
 * @author  MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public abstract class EmergencyType {
    // --- ATRIBUTOS COMUNES ---
    protected String name;
    protected int priority; // Escala de 1 a 10
    protected String description;
    protected boolean requiresMedicalAssistance;

    /**
     * Constructor para inicializar los atributos comunes de cualquier tipo de emergencia.
     *
     * @param name                    El nombre del tipo de emergencia (ej. "Incendio").
     * @param priority                La prioridad numérica (1-10).
     * @param description             Una breve descripción de la emergencia.
     * @param requiresMedicalAssistance {@code true} si este tipo de emergencia necesita asistencia médica.
     */
    public EmergencyType(String name, int priority, String description, boolean requiresMedicalAssistance) {
        this.name = name;
        this.priority = priority;
        this.description = description;
        this.requiresMedicalAssistance = requiresMedicalAssistance;
    }

    // --- MÉTODOS CONCRETOS (Comportamiento común) ---

    public int getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public boolean requiresMedicalAssistance() {
        return requiresMedicalAssistance;
    }

    // --- MÉTODOS ABSTRACTOS (Comportamiento específico de cada subclase) ---

    /**
     * Devuelve el protocolo de respuesta específico para este tipo de emergencia.
     * <p>
     * Cada subclase (como {@code MedicalEmergency}) debe implementar este método
     * para definir los pasos a seguir.
     * </p>
     *
     * @return Una cadena de texto con el protocolo de actuación.
     */
    public abstract String getResponseProtocol();

    /**
     * Devuelve una lista de los servicios de emergencia requeridos.
     * <p>
     * Cada subclase debe especificar qué unidades o personal son necesarios
     * (ej. "Ambulancia", "Bomberos").
     * </p>
     *
     * @return Un array de cadenas con los nombres de los servicios.
     */
    public abstract String[] getRequiredServices();

    /**
     * Devuelve una representación en cadena del tipo de emergencia.
     *
     * @return Una cadena formateada con los detalles principales.
     */
    @Override
    public String toString() {
        return String.format(
            "Tipo de Emergencia: %s (Prioridad: %d/10)",
            name, priority
        );
    }
}

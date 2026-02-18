package com.emergencias.model;

/**
 * <h1>Implementación Concreta para Emergencias Médicas</h1>
 *
 * <p>
 * Esta clase extiende {@link EmergencyType} para representar específicamente
 * una emergencia de naturaleza médica.
 * </p>
 *
 * <p>
 * Proporciona la implementación de los métodos abstractos de la clase padre,
 * definiendo un protocolo de respuesta y los servicios requeridos que son
 * específicos para incidentes médicos.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class MedicalEmergency extends EmergencyType {
    
    /**
     * Constructor para una emergencia médica.
     * <p>
     * Llama al constructor de la clase padre con valores predefinidos que
     * caracterizan a una emergencia médica, como una alta prioridad y el
     * requisito de asistencia médica.
     * </p>
     */
    public MedicalEmergency() {
        super(
            "Problema Médico", // Nombre fijo para este tipo de emergencia
            8,                 // Prioridad alta (escala 1-10)
            "Emergencia relacionada con la salud de una persona, como un ataque cardíaco, una caída grave, etc.",
            true               // Siempre requiere asistencia médica
        );
    }

    /**
     * Devuelve el protocolo de actuación específico para una emergencia médica.
     *
     * @return Una cadena de texto con los pasos a seguir por los servicios de emergencia.
     */
    @Override
    public String getResponseProtocol() {
        return "PROTOCOLO DE EMERGENCIA MÉDICA:\n" +
               "1. Evaluar la conciencia y respiración del paciente.\n" +
               "2. Proporcionar primeros auxilios si es posible y seguro.\n" +
               "3. No mover al paciente si hay sospecha de lesión espinal.\n" +
               "4. Preparar para la llegada de la ambulancia.";
    }

    /**
     * Devuelve los servicios requeridos para atender una emergencia médica.
     *
     * @return Un array de cadenas con los servicios necesarios.
     */
    @Override
    public String[] getRequiredServices() {
        return new String[] {
            "Ambulancia de Soporte Vital Avanzado (SVA)",
            "Equipo Médico de Urgencias"
        };
    }
}

package com.emergencias.model;

/**
 * Implementación concreta de EmergencyType para emergencias médicas.
 * Demuestra herencia y especialización de comportamiento.
 */
public class MedicalEmergency extends EmergencyType {
    
    public MedicalEmergency() {
        super(
            "Problema médico",
            8,  // Alta prioridad
            "Emergencia relacionada con problemas de salud del usuario",
            true  // Requiere asistencia médica
        );
    }

    @Override
    public String getResponseProtocol() {
        return """
            PROTOCOLO DE EMERGENCIA MÉDICA:
            1. Llamar inmediatamente a ambulancia (112)
            2. Solicitar información médica del paciente
            3. Realizar primeros auxilios si es necesario
            4. Mantener al paciente calmado
            5. Notificar a contactos de emergencia
            """;
    }

    @Override
    public String[] getRequiredServices() {
        return new String[]{"Ambulancia", "Hospital", "Servicio médico de emergencia"};
    }
}

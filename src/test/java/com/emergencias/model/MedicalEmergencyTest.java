package com.emergencias.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MedicalEmergencyTest {

    @Test
    @DisplayName("MedicalEmergency inicializa correctamente los campos heredados")
    void medicalEmergencyInitialization() {
        MedicalEmergency me = new MedicalEmergency("Infarto", 10, "Ataque al corazón");
        
        assertEquals("Infarto", me.getName());
        assertEquals(10, me.getPriority());
        assertEquals("Ataque al corazón", me.getDescription());
        assertTrue(me.requiresMedicalAssistance(), "Las emergencias médicas deben requerir asistencia por defecto");
    }

    @Test
    @DisplayName("El protocolo de respuesta médica es el esperado")
    void protocolIsCorrect() {
        MedicalEmergency me = new MedicalEmergency("Test", 1, "Test Desc");
        String protocol = me.getResponseProtocol();
        
        assertTrue(protocol.contains("signos vitales"));
        assertTrue(protocol.contains("Estabilizar"));
        assertTrue(protocol.contains("traslado"));
    }

    @Test
    @DisplayName("Los servicios requeridos incluyen ambulancia y médicos")
    void requiredServicesAreCorrect() {
        MedicalEmergency me = new MedicalEmergency("Test", 1, "Test Desc");
        String[] services = me.getRequiredServices();
        
        assertNotNull(services);
        assertTrue(services.length >= 3);
        assertEquals("Ambulancia Soporte Vital Avanzado", services[0]);
    }
}

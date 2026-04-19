package com.emergencias.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmergencyTypeTest {

    private static class DummyEmergency extends EmergencyType {
        public DummyEmergency(String name, int priority, String description, boolean requiresMedical) {
            super(name, priority, description, requiresMedical);
        }

        @Override
        public String getResponseProtocol() {
            return "Protocolo Dummy";
        }

        @Override
        public String[] getRequiredServices() {
            return new String[]{"Servicio Dummy"};
        }
    }

    @Test
    @DisplayName("Los métodos getter de EmergencyType funcionan correctamente")
    void gettersWork() {
        DummyEmergency de = new DummyEmergency("Incendio", 7, "Fuego en cocina", false);
        
        assertEquals("Incendio", de.getName());
        assertEquals(7, de.getPriority());
        assertEquals("Fuego en cocina", de.getDescription());
        assertFalse(de.requiresMedicalAssistance());
    }

    @Test
    @DisplayName("toString() genera el formato esperado")
    void toStringFormatIsCorrect() {
        DummyEmergency de = new DummyEmergency("Test", 1, "Desc", true);
        String result = de.toString();
        
        assertTrue(result.contains("Emergencia: Test"));
        assertTrue(result.contains("Prioridad: 1/10"));
        assertTrue(result.contains("Asistencia médica: Sí"));
    }
}

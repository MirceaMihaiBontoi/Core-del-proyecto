package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AlertSenderTest {

    private AlertSender alertSender;

    @BeforeEach
    void setUp() {
        alertSender = new AlertSender();
    }

    @Test
    @DisplayName("getAlertType devuelve la descripción correcta")
    void getAlertTypeReturnsCorrectName() {
        assertEquals("Sistema de Alertas de Emergencia", alertSender.getAlertType());
    }

    @Test
    @DisplayName("send() devuelve falso si el evento es nulo")
    void sendReturnsFalseOnNullEvent() {
        assertFalse(alertSender.send(null));
    }

    @Test
    @DisplayName("send() completa la simulación correctamente para un evento válido")
    void sendSimulatesSuccessfully() {
        UserData realUserData = new UserData("Juan Perez", "600111222", "Ninguna", "Maria 600333444");
        EmergencyEvent event = new EmergencyEvent("FUEGO", "Calle Test", 5, realUserData.toString());
        
        boolean result = alertSender.send(event);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("notifyContacts no falla si los datos de usuario son nulos")
    void notifyContactsHandlesNullUserData() {
        EmergencyEvent event = new EmergencyEvent("FUEGO", "Calle Test", 5, "");
        assertDoesNotThrow(() -> alertSender.notifyContacts(null, event));
    }
}


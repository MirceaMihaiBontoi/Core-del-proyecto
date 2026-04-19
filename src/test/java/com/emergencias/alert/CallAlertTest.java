package com.emergencias.alert;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CallAlertTest {

    private CallAlert callAlert;

    @BeforeEach
    void setUp() {
        callAlert = new CallAlert();
    }

    @Test
    @DisplayName("getAlertType devuelve 'Llamada Telefónica'")
    void getAlertTypeReturnsCorrectName() {
        assertEquals("Llamada Telefónica", callAlert.getAlertType());
    }

    @Test
    @DisplayName("send() devuelve falso si el evento es nulo")
    void sendReturnsFalseOnNullEvent() {
        assertFalse(callAlert.send(null));
    }

    @Test
    @DisplayName("send() completa la simulación de llamada para un evento válido")
    void sendWorksForValidEvent() {
        EmergencyEvent event = new EmergencyEvent("MEDICA", "Sótano 1", 8, "Datos Test");

        boolean result = callAlert.send(event);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("notifyContacts maneja contactos nulos o vacíos")
    void notifyContactsHandlesMissingInfo() {
        EmergencyEvent event = new EmergencyEvent("MEDICA", "Sótano 1", 8, "Datos Test");
        
        // Caso nulo
        assertDoesNotThrow(() -> callAlert.notifyContacts(null, event));
        
        // Caso vacío
        UserData userData = new UserData("Juan", "123", "Nada", "");
        assertDoesNotThrow(() -> callAlert.notifyContacts(userData, event));
    }
}


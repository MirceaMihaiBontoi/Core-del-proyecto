package com.emergencias.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserFeedbackTest {

    @Test
    @DisplayName("UserFeedback almacena los datos de construcción correctamente")
    void feedbackDataStorage() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        UserFeedback feedback = new UserFeedback("EM-001", 5, "Excelente servicio");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        
        assertEquals("EM-001", feedback.getEmergencyId());
        assertEquals(5, feedback.getSatisfactionRating());
        assertEquals("Excelente servicio", feedback.getComments());
        

        assertTrue(feedback.getFeedbackTime().isAfter(before) || feedback.getFeedbackTime().isEqual(before));
        assertTrue(feedback.getFeedbackTime().isBefore(after) || feedback.getFeedbackTime().isEqual(after));
    }

    @Test
    @DisplayName("toString contiene la información clave")
    void toStringContainsInfo() {
        UserFeedback feedback = new UserFeedback("ID-123", 4, "Bien");
        String out = feedback.toString();
        
        assertTrue(out.contains("ID-123"));
        assertTrue(out.contains("4"));
        assertTrue(out.contains("Bien"));
    }
}

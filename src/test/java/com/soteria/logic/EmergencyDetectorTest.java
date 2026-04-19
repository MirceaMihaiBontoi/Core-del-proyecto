package com.soteria.logic;

import com.soteria.core.model.EmergencyEvent;
import com.soteria.core.model.UserData;
import com.soteria.logic.EmergencyDetector.DetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmergencyDetectorTest {

    private EmergencyDetector detector;

    @BeforeEach
    void setUp() {
        UserData user = new UserData("Test User", "600123456", "None", "Relative 600000000");
        // Passing null for classifier will trigger manual fallback
        detector = new EmergencyDetector(user, null);
    }

    @Test
    @DisplayName("Manual fallback classifies fires by keywords")
    void classifiesFireFromKeywords() {
        DetectionResult fuego = detector.classifyEmergency("hay fuego en la cocina");
        DetectionResult incendio = detector.classifyEmergency("un INCENDIO enorme");

        assertTrue(fuego.detected());
        assertTrue(fuego.typeName().contains("Fire") || fuego.typeName().contains("Incendio"));
        assertTrue(incendio.detected());
        assertTrue(fuego.instructions().length > 0);
    }

    @Test
    @DisplayName("Manual fallback classifies other categories")
    void classifiesOtherCategories() {
        assertTrue(detector.classifyEmergency("accident with the car").typeName().contains("Traffic"));
        assertTrue(detector.classifyEmergency("me duele el pecho").typeName().contains("Medical"));
    }

    @Test
    @DisplayName("Manual fallback detected nothing for unknown text")
    void returnsNotDetectedForUnknownText() {
        DetectionResult result = detector.classifyEmergency("hello testing");
        assertFalse(result.detected());
        assertNull(result.typeName());
        assertEquals(0, result.instructions().length);
    }

    @Test
    @DisplayName("isValidSeverity respects bounds [1,10]")
    void validatesSeverityBounds() {
        assertFalse(detector.isValidSeverity(0));
        assertTrue(detector.isValidSeverity(1));
        assertTrue(detector.isValidSeverity(10));
        assertFalse(detector.isValidSeverity(11));
    }

    @Test
    @DisplayName("createEvent handles default locations")
    void createEventDefaultsLocation() {
        DetectionResult result = detector.classifyEmergency("fuego");
        EmergencyEvent eventEmpty = detector.createEvent(result, "", 5);
        EmergencyEvent eventNull = detector.createEvent(result, null, 5);
        EmergencyEvent eventReal = detector.createEvent(result, "Street 1", 7);

        assertEquals("Unknown location", eventEmpty.location());
        assertEquals("Unknown location", eventNull.location());
        assertEquals("Street 1", eventReal.location());
        assertEquals(7, eventReal.severityLevel());
    }
}

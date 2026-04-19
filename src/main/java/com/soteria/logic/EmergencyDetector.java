package com.soteria.logic;

import com.soteria.core.interfaces.EmergencyClassifier;
import com.soteria.core.model.EmergencyEvent;
import com.soteria.core.model.UserData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for detecting and classifying emergencies.
 * Decoupled from specific AI implementation via the EmergencyClassifier interface.
 */
public class EmergencyDetector {
    private static final int MIN_SEVERITY = 1;
    private static final int MAX_SEVERITY = 10;

    private final UserData userData;
    private final EmergencyClassifier classifier;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmergencyDetector(UserData userData, EmergencyClassifier classifier) {
        this.userData = userData;
        this.classifier = classifier;
    }

    /**
     * Records the result of a detection.
     */
    public record DetectionResult(
        boolean detected,
        String typeName,
        String context,
        double confidence,
        String[] instructions,
        String correctedText
    ) {}

    /**
     * Classifies an emergency description.
     */
    public DetectionResult classifyEmergency(String description) {
        if (classifier != null && classifier.isAvailable()) {
            return classifyWithAI(description);
        } else {
            return classifyManually(description);
        }
    }

    private DetectionResult classifyWithAI(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return classifyManually("");
        }
        
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            String corrected = root.has("corrected_text") ? root.get("corrected_text").asText() : "";
            
            if (root.has("emergencies") && root.get("emergencies").isArray() && !root.get("emergencies").isEmpty()) {
                JsonNode first = root.get("emergencies").get(0);
                return new DetectionResult(
                    true, 
                    first.has("type") ? first.get("type").asText() : "AI Detected", 
                    "AI Context", 
                    first.has("confidence") ? first.get("confidence").asDouble() : 0.95, 
                    new String[]{"Follow AI voice/text instructions"}, 
                    corrected
                );
            }
        } catch (Exception e) {
            // Fallback if parsing fails
        }
        
        return classifyManually("");
    }

    private DetectionResult classifyManually(String message) {
        String lower = message.toLowerCase();
        String typeName = null;
        String context = null;
        String[] instructions = new String[0];
        
        if (lower.contains("fire") || lower.contains("fuego") || lower.contains("incendio") || lower.contains("quema")) {
            typeName = "Incendio / Fire";
            context = "fire";
            instructions = new String[]{
                "Evacuate immediately / Evacuar inmediatamente",
                "Call 112/911 / Llamar al 112",
                "Do not use elevators / No usar ascensores"
            };
        } else if (lower.contains("accident") || lower.contains("coche") || lower.contains("car") || lower.contains("choque") || lower.contains("atropello")) {
            typeName = "Traffic Accident / Accidente";
            context = "accident";
            instructions = new String[]{
                "Secure the area / Asegurar la zona",
                "Do not move injured people / No mover a los heridos",
                "Call 112/911 / Llamar al 112"
            };
        } else if (lower.contains("pain") || lower.contains("médico") || lower.contains("medico") || lower.contains("hurt") || lower.contains("duele") || lower.contains("pecho") || lower.contains("ataque")) {
            typeName = "Medical Emergency / Médica";
            context = "medical";
            instructions = new String[]{
                "Stay calm / Mantener la calma",
                "Sit down / Sentarse",
                "Call 112/911 / Llamar al 112"
            };
        }
        
        if (typeName != null) {
            return new DetectionResult(true, typeName, context, 0.0, instructions, message);
        }
        
        return new DetectionResult(false, null, null, 0.0, new String[0], message);
    }

    public EmergencyEvent createEvent(DetectionResult result, String location, int severity) {
        String loc = (location == null || location.isEmpty()) ? "Unknown location" : location;
        
        return new EmergencyEvent(
            result.typeName(),
            loc,
            severity,
            userData.fullName() + " (" + userData.phoneNumber() + ")"
        );
    }

    public boolean isValidSeverity(int severity) {
        return severity >= MIN_SEVERITY && severity <= MAX_SEVERITY;
    }
}

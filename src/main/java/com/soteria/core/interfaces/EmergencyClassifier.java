package com.soteria.core.interfaces;

/**
 * Interface that defines the contract for AI-based emergency classification.
 */
public interface EmergencyClassifier {
    /**
     * Classifies a text description into an emergency type.
     */
    String classify(String text);
    
    /**
     * Checks if the classification service is currently available.
     */
    boolean isAvailable();
}

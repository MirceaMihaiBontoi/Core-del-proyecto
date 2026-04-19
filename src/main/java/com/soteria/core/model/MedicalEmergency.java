package com.soteria.core.model;

/**
 * Concrete implementation for medical emergencies.
 */
public class MedicalEmergency extends EmergencyType {

    public MedicalEmergency(String name, int priority, String description) {
        super(name, priority, description, true);
    }

    @Override
    public String getResponseProtocol() {
        return "1. Assess vital signs.\n" +
               "2. Stabilize the patient.\n" +
               "3. Prepare for transport to the nearest hospital.";
    }

    @Override
    public String[] getRequiredServices() {
        return new String[] {
            "Advanced Life Support Ambulance",
            "Emergency Physician",
            "Nurse"
        };
    }
}

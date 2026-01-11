package com.emergencias.model;

import java.util.Scanner;

/**
 * Clase que contiene los datos personales del usuario para situaciones de emergencia.
 * 
 * Esta clase almacena información crítica que puede ser vital en situaciones de emergencia,
 * incluyendo información médica relevante y contactos de emergencia.
 * 
 * Características principales:
 * - Almacena información personal básica del usuario
 * - Incluye información médica relevante
 * - Mantiene un contacto de emergencia
 * - Proporciona una representación en cadena formateada
 */
public class UserData {
    // Nombre completo del usuario
    private String fullName;
    
    // Número de teléfono de contacto principal
    private String phoneNumber;
    
    // Información médica relevante (alergias, condiciones, etc.)
    private String medicalInfo;
    
    // Datos del contacto de emergencia (nombre y teléfono)
    private String emergencyContact;

    /**
     * Constructor para crear un nuevo objeto UserData con toda la información necesaria.
     * 
     * @param fullName Nombre completo del usuario
     * @param phoneNumber Número de teléfono de contacto
     * @param medicalInfo Información médica relevante (puede ser nula)
     * @param emergencyContact Información del contacto de emergencia (nombre y teléfono)
     */
    public UserData(String fullName, String phoneNumber, String medicalInfo, String emergencyContact) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.medicalInfo = medicalInfo;
        this.emergencyContact = emergencyContact;
    }

    // Getters y Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMedicalInfo() {
        return medicalInfo;
    }

    public void setMedicalInfo(String medicalInfo) {
        this.medicalInfo = medicalInfo;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    /**
     * Recopila los datos del usuario mediante entrada por consola.
     * Este método guía al usuario a través de un proceso paso a paso
     * para ingresar su información personal y de contacto de emergencia.
     */
    public void collectUserData(Scanner scanner) {
        System.out.println("\n=== REGISTRO DE DATOS DE USUARIO ===");
        
        // Obtener nombre completo
        System.out.print("Ingrese su nombre completo: ");
        this.fullName = scanner.nextLine().trim();
        
        // Obtener número de teléfono
        System.out.print("Ingrese su número de teléfono: ");
        this.phoneNumber = scanner.nextLine().trim();
        
        // Obtener información médica (opcional)
        System.out.print("Ingrese información médica relevante (alergias, condiciones, etc.) [opcional]: ");
        this.medicalInfo = scanner.nextLine().trim();
        
        // Obtener contacto de emergencia
        System.out.print("Ingrese nombre y teléfono de contacto de emergencia (ejemplo: Juan Pérez 123456789): ");
        this.emergencyContact = scanner.nextLine().trim();
        
        System.out.println("\n¡Gracias! Sus datos han sido registrados correctamente.");
        System.out.println("==========================================\n");
    }

    /**
     * Devuelve una representación en cadena formateada de los datos del usuario.
     * 
     * @return Cadena formateada con la información del usuario, lista para ser mostrada
     *         o almacenada. Incluye nombre, teléfono, contacto de emergencia e
     *         información médica.
     */
    @Override
    public String toString() {
        return String.format(
            "Nombre: %s\nTeléfono: %s\nContacto de emergencia: %s\nInformación médica: %s",
            fullName, phoneNumber, emergencyContact, medicalInfo
        );
    }
}

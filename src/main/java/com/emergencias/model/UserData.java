package com.emergencias.model;

import java.util.Scanner;

/**
 * <h1>Entidad de Datos del Usuario</h1>
 *
 * <p>
 * Esta clase (POJO - Plain Old Java Object) representa la información personal
 * y de contacto de un usuario. Su propósito es encapsular los datos que son
 * vitales durante una emergencia.
 * </p>
 *
 * <p>
 * Contiene métodos para recopilar y validar esta información desde la consola.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class UserData {
    // --- ATRIBUTOS ---
    private String fullName;
    private String phoneNumber;
    private String medicalInfo;
    private String emergencyContact;

    /**
     * Constructor para inicializar un objeto UserData con toda la información.
     *
     * @param fullName         Nombre completo del usuario.
     * @param phoneNumber      Número de teléfono principal.
     * @param medicalInfo      Información médica relevante (alergias, tipo de sangre, etc.).
     * @param emergencyContact Nombre y teléfono del contacto de emergencia.
     */
    public UserData(String fullName, String phoneNumber, String medicalInfo, String emergencyContact) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.medicalInfo = medicalInfo;
        this.emergencyContact = emergencyContact;
    }

    // --- GETTERS Y SETTERS ---
    // Métodos estándar para acceder y modificar los atributos de la clase.
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getMedicalInfo() { return medicalInfo; }
    public void setMedicalInfo(String medicalInfo) { this.medicalInfo = medicalInfo; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    /**
     * Recopila y valida los datos del usuario interactivamente a través de la consola.
     * <p>
     * Este método guía al usuario para que introduzca su información personal,
     * validando que los campos obligatorios no estén vacíos y que el número de
     * teléfono tenga un formato básico correcto.
     * </p>
     *
     * @param scanner La instancia compartida de Scanner para leer la entrada del usuario.
     */
    public void collectUserData(Scanner scanner) {
        System.out.println("\n=== REGISTRO DE DATOS DE USUARIO ===");
        
        // 1. Obtener nombre completo (obligatorio)
        this.fullName = promptForNonEmptyInput("Ingrese su nombre completo: ", scanner);
        
        // 2. Obtener y validar número de teléfono (mínimo 9 dígitos)
        while (true) {
            System.out.print("Ingrese su número de teléfono (mínimo 9 dígitos): ");
            String inputPhone = scanner.nextLine().trim();
            String digitsOnly = inputPhone.replaceAll("[\\s-]", ""); // Limpia el input
            if (digitsOnly.matches("\\d{9,}")) { // Valida con Regex
                this.phoneNumber = inputPhone;
                break;
            }
            System.out.println("⚠️  Error: El teléfono debe contener al menos 9 dígitos. Intente nuevamente.");
        }
        
        // 3. Obtener información médica (opcional)
        System.out.print("Ingrese información médica relevante (alergias, etc.) [opcional]: ");
        this.medicalInfo = scanner.nextLine().trim();
        if (this.medicalInfo.isEmpty()) {
            this.medicalInfo = "No especificada";
        }
        
        // 4. Obtener contacto de emergencia (obligatorio)
        this.emergencyContact = promptForNonEmptyInput("Ingrese nombre y teléfono de contacto de emergencia: ", scanner);
        
        System.out.println("\n✅ ¡Gracias! Sus datos han sido registrados correctamente.");
        System.out.println("==========================================\n");
    }

    /**
     * Método de utilidad para solicitar una entrada no vacía al usuario.
     *
     * @param prompt  El mensaje a mostrar al usuario.
     * @param scanner La instancia de Scanner.
     * @return La entrada del usuario, una vez validada como no vacía.
     */
    private String promptForNonEmptyInput(String prompt, Scanner scanner) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("⚠️  Error: Este campo no puede estar vacío. Intente nuevamente.");
        }
    }

    /**
     * Devuelve una representación en cadena de los datos del usuario.
     * <p>
     * Es útil para logs o para mostrar la información de forma rápida.
     * </p>
     *
     * @return Una cadena formateada con toda la información del usuario.
     */
    @Override
    public String toString() {
        return String.format(
            "Nombre: %s\nTeléfono: %s\nContacto de emergencia: %s\nInformación médica: %s",
            fullName, phoneNumber, emergencyContact, medicalInfo
        );
    }
}

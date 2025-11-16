package com.emergencias;

import com.emergencias.controller.EmergencyManager;
import com.emergencias.model.UserData;

/**
 * Clase principal que inicia la aplicación del sistema de emergencias.
 */
public class Main {
    /**
     * Punto de entrada principal de la aplicación.
     */
    public static void main(String[] args) {
        // 1. Crear datos de ejemplo del usuario con información relevante
        UserData userData = new UserData(
            "Juan Pérez",                      // Nombre completo del usuario
            "+34 600 123 456",                // Teléfono de contacto
            "Alergias: Ninguna\nTipo de sangre: A+",  // Información médica relevante
            "María García (Madre): +34 600 654 321"  // Contacto de emergencia
        );
        
        // 2. Inicializar el gestor de emergencias con los datos del usuario
        //    El gestor se encargará de coordinar todas las operaciones del sistema
        EmergencyManager emergencyManager = new EmergencyManager(userData);
        
        // 3. Iniciar el sistema de gestión de emergencias
        //    Este método contiene el bucle principal de la aplicación
        emergencyManager.startSystem();
    }
}

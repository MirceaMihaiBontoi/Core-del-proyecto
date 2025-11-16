package com.emergencias.controller;

import com.emergencias.alert.AlertSender;
import com.emergencias.detector.EmergencyDetector;
import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import java.util.Scanner;

/**
 Clase controladora principal que gestiona el flujo de detección y notificación de emergencias.
 */
public class EmergencyManager {
    // Componentes del sistema
    private final EmergencyDetector detector;  // Para detectar emergencias
    private final AlertSender alertSender;     // Para enviar notificaciones
    private final UserData userData;           // Datos del usuario actual

    /**
     Constructor que inicializa el gestor de emergencias con los datos del usuario.
     */
    public EmergencyManager(UserData userData) {
        this.userData = userData;  // Almacenar datos del usuario
        this.detector = new EmergencyDetector(userData);  // Inicializar detector
        this.alertSender = new AlertSender();  // Inicializar sistema de alertas
    }

    /**
     * Inicia el sistema de gestión de emergencias.
     * Este método implementa el bucle principal de la aplicación
     * El sistema se ejecutará hasta que el usuario decida salir.
     */

    public void startSystem() {
        System.out.println("Sistema de Gestión de Emergencias - Iniciado");
        System.out.println("=========================================\n");
        
        // Obtener datos del usuario al iniciar
        userData.collectUserData();
        
        // Inicializar Scanner para entrada de usuario
        Scanner scanner = new Scanner(System.in);
        
        // Bucle principal
        try {
            while (true) {
            // Paso 1: Detectar emergencia a través del detector
            EmergencyEvent event = detector.detectEmergency();
            
            // Si se detectó una emergencia válida
            if (event != null) {
                // Paso 2: Enviar alerta a servicios de emergencia
                boolean alertSent = alertSender.sendAlert(event);
                
                if (alertSent) {
                    // Paso 3: Notificar a los contactos de emergencia
                    alertSender.notifyEmergencyContacts(userData.toString(), event);
                
                    // Confirmación al usuario
                    System.out.println("\n¡Emergencia reportada con éxito!");
                    System.out.println("Se ha creado un registro de la emergencia en el sistema.");
                } else {
                    // Manejo de error en el envío de alerta
                    System.out.println("\nNo se pudo enviar la alerta. Por favor, intente nuevamente o llame al 112 manualmente.");
                }
            }
            
            // Preguntar al usuario si desea realizar otra acción
            System.out.print("\n¿Desea realizar otra acción? (S/N): ");
            String response = scanner.nextLine().trim();
            
            // Salir del bucle si el usuario no desea continuar
            if (!response.equalsIgnoreCase("S")) {
                System.out.println("\nSaliendo del sistema de emergencias. ¡Hasta pronto!");
                break;  // Terminar el bucle principal
            }
            
                // Separador visual entre operaciones
                System.out.println("\n" + "=".repeat(80) + "\n");
            }
        } finally {
            // Asegurarse de cerrar el scanner al salir
            scanner.close();
        }
    }
}

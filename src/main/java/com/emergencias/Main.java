package com.emergencias;

import com.emergencias.alert.AlertSender;
import com.emergencias.alert.EmergencyLogger;
import com.emergencias.controller.EmergencyManager;
import com.emergencias.detector.EmergencyDetector;
import com.emergencias.model.UserData;
import com.emergencias.services.IAlert;
import java.util.Scanner;

/**
 * <h1>Clase Principal del Sistema de Emergencias</h1>
 *
 * <p>
 * Esta clase es el punto de entrada de la aplicación (entrypoint). Su única
 * responsabilidad es "ensamblar" la aplicación, creando todas las instancias
 * de los componentes clave y conectándolos entre sí mediante la Inyección de
 * Dependencias.
 * </p>
 *
 * <p>
 * El flujo es el siguiente:
 * <ol>
 *     <li>Crea los objetos de datos (como {@link UserData}).</li>
 *     <li>Crea los servicios o componentes ({@link EmergencyDetector}, {@link AlertSender}, {@link EmergencyLogger}).</li>
 *     <li>Inyecta estos componentes en el controlador principal ({@link EmergencyManager}).</li>
 *     <li>Inicia el sistema.</li>
 * </ol>
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class Main {
    
    /**
     * Punto de entrada principal de la aplicación.
     * <p>
     * Configura el entorno, gestiona las dependencias y maneja las excepciones
     * críticas que puedan ocurrir durante el arranque o la ejecución.
     * </p>
     *
     * @param args Argumentos de la línea de comandos (no se utilizan en esta aplicación).
     */
    public static void main(String[] args) {
        // Se crea un único objeto Scanner para toda la aplicación.
        // Esto evita problemas de cierre del flujo de entrada estándar (System.in).
        Scanner scanner = new Scanner(System.in);
        
        try {
            // --- PASO 1: Creación de los objetos de datos ---
            // Se instancia un objeto UserData con información de ejemplo.
            // Esta información será posteriormente actualizada por el usuario.
            UserData userData = new UserData(
                "Juan Pérez",
                "+34 600 123 456",
                "Alergias: Ninguna\nTipo de sangre: A+",
                "María García (Madre): +34 600 654 321"
            );
            
            // --- PASO 2: Creación de las dependencias (servicios) ---
            // Se instancian los componentes que realizarán las tareas principales.
            EmergencyDetector detector = new EmergencyDetector(userData, scanner);
            IAlert alertSender = new AlertSender(); // Usamos la interfaz para desacoplar
            EmergencyLogger logger = new EmergencyLogger();
            
            // --- PASO 3: Inyección de Dependencias ---
            // Se crea el gestor principal y se le "inyectan" todas las dependencias
            // a través de su constructor. El EmergencyManager no crea sus propias
            // herramientas, solo las recibe y las orquesta.
            EmergencyManager emergencyManager = new EmergencyManager(
                userData, 
                scanner, 
                detector, 
                alertSender, 
                logger
            );
            
            // --- PASO 4: Inicio del sistema ---
            // Se llama al método que contiene el bucle principal de la aplicación.
            emergencyManager.startSystem();
            
        } catch (Exception e) {
            // Captura global para cualquier error no esperado que pueda detener la aplicación.
            System.err.println("\n=== ERROR CRÍTICO INESPERADO ===");
            System.err.println("La aplicación ha encontrado un problema fatal y debe cerrarse.");
            System.err.println("Detalles del error: " + e.getMessage());
            e.printStackTrace(); // Imprime la traza completa para depuración.
            
        } finally {
            // --- PASO 5: Liberación de recursos ---
            // Es crucial cerrar el Scanner para liberar el recurso System.in
            // de forma segura al finalizar la aplicación, sin importar si hubo errores.
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}

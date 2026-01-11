package com.emergencias.detector;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import java.util.Scanner;

/**
 * Clase encargada de detectar emergencias y recopilar información inicial.
 */
public class EmergencyDetector {
    // Constantes para validación de gravedad
    private static final int MIN_SEVERITY = 1;  // Nivel mínimo de gravedad
    private static final int MAX_SEVERITY = 10; // Nivel máximo de gravedad
    
    // Componentes internos
    private final Scanner scanner;  // Para leer la entrada del usuario
    private final UserData userData; // Datos del usuario para la notificación

    /**
     * Constructor de la clase EmergencyDetector
     */
    public EmergencyDetector(UserData userData, Scanner scanner) {
        this.scanner = scanner;  // Usa el scanner compartido
        this.userData = userData;  // Almacena los datos del usuario
    }

    /**
     * Inicia el proceso de detección de emergencia
     */
    public EmergencyEvent detectEmergency() {
        // Mostrar encabezado de la sección
        System.out.println("\n=== DETECCIÓN DE EMERGENCIA ===");
        
        // Preguntar al usuario si está en una emergencia
        System.out.print("¿Estás en una situación de emergencia? (S/N): ");
        
        // Verificar si el usuario confirma la emergencia
        if (scanner.nextLine().equalsIgnoreCase("S")) {
            // Recopilar información detallada de la emergencia
            String emergencyType = getEmergencyType();  // Obtener el tipo de emergencia
            String location = getLocation();           // Obtener la ubicación
            int severity = getSeverityLevel();         // Obtener nivel de gravedad
            
            // Confirmar con el usuario antes de proceder
            if (confirmEmergency(emergencyType, location, severity)) {
                // Crear y retornar un nuevo evento de emergencia con los datos recopilados
                return new EmergencyEvent(
                    emergencyType,     // Tipo de emergencia
                    location,          // Ubicación de la emergencia
                    severity,          // Nivel de gravedad
                    userData.toString() // Datos del usuario
                );
            }
        }
        
        // Si el usuario cancela o no confirma, mostrar mensaje y retornar null
        System.out.println("Emergencia cancelada o no confirmada.");
        return null;  // Indicar que no se generó ningún evento de emergencia
    }

    private String getEmergencyType() {
        System.out.println("\nTipos de emergencia disponibles:");
        System.out.println("1. Accidente de tráfico");
        System.out.println("2. Problema médico");
        System.out.println("3. Incendio");
        System.out.println("4. Agresión");
        System.out.println("5. Otro");
        
        System.out.print("Seleccione el tipo de emergencia (1-5): ");
        String input = scanner.nextLine();
        
        return switch (input) {
            case "1" -> "Accidente de tráfico";
            case "2" -> "Problema médico";
            case "3" -> "Incendio";
            case "4" -> "Agresión";
            default -> "Otro";
        };
    }

    private String getLocation() {
        System.out.print("\nUbicación actual (o presione Enter para usar ubicación por GPS): ");
        String location = scanner.nextLine();
        
        if (location.trim().isEmpty()) {
            // Simular obtención de ubicación por GPS
            return "40.4168° N, 3.7038° O (Plaza Mayor, Madrid)";
        }
        
        return location;
    }

    private int getSeverityLevel() {
        while (true) {
            try {
                System.out.printf("\nNivel de gravedad (%d-%d): ", MIN_SEVERITY, MAX_SEVERITY);
                int severity = Integer.parseInt(scanner.nextLine());
                
                if (severity >= MIN_SEVERITY && severity <= MAX_SEVERITY) {
                    return severity;
                }
                
                System.out.printf("Por favor, ingrese un valor entre %d y %d.\n", 
                    MIN_SEVERITY, MAX_SEVERITY);
                    
            } catch (NumberFormatException e) {
                System.out.println("Por favor, ingrese un número válido.");
            }
        }
    }

    private boolean confirmEmergency(String emergencyType, String location, int severity) {
        System.out.println("\n=== RESUMEN DE LA EMERGENCIA ===");
        System.out.println("Tipo: " + emergencyType);
        System.out.println("Ubicación: " + location);
        System.out.println("Nivel de gravedad: " + severity + "/" + MAX_SEVERITY);
        
        System.out.print("\n¿Confirmar envío de alerta de emergencia? (S/N): ");
        return scanner.nextLine().equalsIgnoreCase("S");
    }
}

package com.emergencias.detector;

import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import java.util.Scanner;

/**
 * <h1>Detector de Emergencias</h1>
 *
 * <p>
 * Esta clase es responsable de interactuar con el usuario para determinar si
 * está ocurriendo una emergencia y, en caso afirmativo, recopilar toda la
 * información necesaria sobre ella.
 * </p>
 *
 * <p>
 * Guía al usuario a través de una serie de preguntas para definir el tipo,
 * la ubicación y la gravedad del incidente, y finalmente pide confirmación
 * antes de crear un {@link EmergencyEvent}.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class EmergencyDetector {
    // Constantes para la validación del nivel de gravedad.
    private static final int MIN_SEVERITY = 1;
    private static final int MAX_SEVERITY = 10;
    
    // Dependencias inyectadas.
    private final Scanner scanner;
    private final UserData userData;

    /**
     * Constructor que recibe las dependencias necesarias.
     *
     * @param userData Los datos del usuario, necesarios para asociarlos al evento.
     * @param scanner  La instancia compartida de Scanner para leer la entrada del usuario.
     */
    public EmergencyDetector(UserData userData, Scanner scanner) {
        this.scanner = scanner;
        this.userData = userData;
    }

    /**
     * Inicia el proceso de detección y definición de una emergencia.
     * <p>
     * Este es el método principal de la clase. Orquesta la interacción con el
     * usuario y, si se confirma una emergencia, devuelve un objeto
     * {@link EmergencyEvent} completamente poblado.
     * </p>
     *
     * @return Un objeto {@link EmergencyEvent} si se confirma una emergencia,
     *         o {@code null} si el usuario cancela el proceso.
     */
    public EmergencyEvent detectEmergency() {
        System.out.println("\n=== DETECCIÓN DE EMERGENCIA ===");
        System.out.print("¿Estás en una situación de emergencia? (S/N): ");
        
        if (scanner.nextLine().equalsIgnoreCase("S")) {
            // Recopila todos los detalles de la emergencia.
            String emergencyType = getEmergencyType();
            String location = getLocation();
            int severity = getSeverityLevel();
            
            // Pide confirmación final al usuario.
            if (confirmEmergency(emergencyType, location, severity)) {
                // Si se confirma, crea y devuelve el objeto del evento.
                return new EmergencyEvent(
                    emergencyType,
                    location,
                    severity,
                    userData.toString() // Se adjuntan los datos del usuario.
                );
            }
        }
        
        // Si el usuario no confirma la emergencia en ningún paso, se cancela.
        System.out.println("Proceso cancelado por el usuario.");
        return null;
    }

    /**
     * Muestra un menú y solicita al usuario que seleccione el tipo de emergencia.
     * <p>
     * El método insiste hasta que el usuario introduce una opción válida (1-5).
     * </p>
     *
     * @return El tipo de emergencia seleccionado como una cadena de texto.
     */
    private String getEmergencyType() {
        while (true) {
            System.out.println("\nTipos de emergencia disponibles:");
            System.out.println("1. Accidente de tráfico");
            System.out.println("2. Problema médico");
            System.out.println("3. Incendio");
            System.out.println("4. Agresión");
            System.out.println("5. Otro");
            
            System.out.print("Seleccione el tipo de emergencia (1-5): ");
            String input = scanner.nextLine().trim();
            
            switch (input) {
                case "1": return "Accidente de tráfico";
                case "2": return "Problema médico";
                case "3": return "Incendio";
                case "4": return "Agresión";
                case "5": return "Otro";
                default:
                    System.out.println("⚠️  Opción no válida. Por favor, ingrese un número entre 1 y 5.");
            }
        }
    }

    /**
     * Solicita al usuario que introduzca la ubicación de la emergencia.
     * <p>
     * La ubicación es un campo obligatorio. El método no avanzará hasta que
     * el usuario proporcione una entrada no vacía.
     * </p>
     *
     * @return La ubicación proporcionada por el usuario.
     */
    private String getLocation() {
        while (true) {
            System.out.print("\nUbicación actual de la emergencia (obligatorio): ");
            String location = scanner.nextLine().trim();
            if (!location.isEmpty()) {
                return location;
            }
            System.out.println("⚠️  Error: La ubicación no puede estar vacía. Intente nuevamente.");
        }
    }

    /**
     * Solicita al usuario que califique la gravedad de la emergencia en una escala.
     * <p>
     * Valida que la entrada sea un número entero dentro del rango definido
     * por {@code MIN_SEVERITY} y {@code MAX_SEVERITY}.
     * </p>
     *
     * @return El nivel de gravedad como un entero.
     */
    private int getSeverityLevel() {
        while (true) {
            try {
                System.out.printf("\nNivel de gravedad (%d-%d): ", MIN_SEVERITY, MAX_SEVERITY);
                String line = scanner.nextLine();
                int severity = Integer.parseInt(line);
                
                if (severity >= MIN_SEVERITY && severity <= MAX_SEVERITY) {
                    return severity;
                }
                
                System.out.printf("⚠️  Por favor, ingrese un valor entre %d y %d.\n", MIN_SEVERITY, MAX_SEVERITY);
                    
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Por favor, ingrese un número válido.");
            }
        }
    }

    /**
     * Muestra un resumen de la emergencia y pide una confirmación final al usuario.
     *
     * @param emergencyType El tipo de emergencia recopilado.
     * @param location      La ubicación recopilada.
     * @param severity      El nivel de gravedad recopilado.
     * @return {@code true} si el usuario confirma (escribe "S" o "s"),
     *         {@code false} en caso contrario.
     */
    private boolean confirmEmergency(String emergencyType, String location, int severity) {
        System.out.println("\n=== RESUMEN DE LA EMERGENCIA ===");
        System.out.println("Tipo: " + emergencyType);
        System.out.println("Ubicación: " + location);
        System.out.println("Nivel de gravedad: " + severity + "/" + MAX_SEVERITY);
        
        System.out.print("\n¿Confirmar y enviar alerta de emergencia? (S/N): ");
        return scanner.nextLine().equalsIgnoreCase("S");
    }
}

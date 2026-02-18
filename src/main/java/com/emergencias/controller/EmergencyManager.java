package com.emergencias.controller;

import com.emergencias.alert.EmergencyLogger;
import com.emergencias.detector.EmergencyDetector;
import com.emergencias.model.EmergencyEvent;
import com.emergencias.model.UserData;
import com.emergencias.model.CentroSalud;
import com.emergencias.model.CentroSaludUtils;
import com.emergencias.services.IAlert;
import java.util.List;
import java.util.Scanner;

/**
 * <h1>Controlador Principal del Sistema de Emergencias</h1>
 *
 * <p>
 * Esta clase actúa como el "cerebro" de la aplicación. Orquesta el flujo
 * completo de una operación de emergencia, desde la detección hasta la
 * notificación y el registro.
 * </p>
 *
 * <p>
 * Sigue el principio de Inyección de Dependencias: no crea sus propios
 * componentes (detector, logger, etc.), sino que los recibe en su constructor.
 * Esto lo hace flexible, modular y fácil de probar.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class EmergencyManager {
    // --- DEPENDENCIAS ---
    // Estos son los componentes que el EmergencyManager necesita para funcionar.
    // Son 'final' porque se asignan una vez en el constructor y no deben cambiar.
    private final EmergencyDetector detector;
    private final IAlert alertSender;
    private final UserData userData;
    private final EmergencyLogger logger;
    private final Scanner scanner;

    /**
     * Constructor que recibe e inicializa todas las dependencias del gestor.
     *
     * @param userData      El objeto que contiene los datos del usuario.
     * @param scanner       La instancia compartida de Scanner para la entrada del usuario.
     * @param detector      El componente encargado de detectar y definir la emergencia.
     * @param alertSender   El componente para enviar alertas a los servicios y contactos.
     * @param logger        El componente para registrar los eventos y el feedback.
     */
    public EmergencyManager(UserData userData, Scanner scanner, EmergencyDetector detector, IAlert alertSender, EmergencyLogger logger) {
        this.userData = userData;
        this.scanner = scanner;
        this.detector = detector;
        this.alertSender = alertSender;
        this.logger = logger;
    }

    /**
     * Inicia el bucle principal del sistema de gestión de emergencias.
     * <p>
     * Este método se encarga de:
     * <ol>
     *     <li>Recolectar los datos del usuario al inicio.</li>
     *     <li>Entrar en un ciclo infinito que espera y procesa emergencias.</li>
     *     <li>Manejar la lógica de salida del programa.</li>
     * </ol>
     * </p>
     */
    public void startSystem() {
        System.out.println("Sistema de Gestión de Emergencias - Iniciado");
        System.out.println("=========================================\n");
        
        try {
            // Al iniciar, se solicitan los datos del usuario.
            userData.collectUserData(scanner);
            
            // Bucle principal de la aplicación. Se ejecuta hasta que el usuario decida salir.
            while (true) {
                try {
                    // 1. Detección: Se delega al detector la tarea de identificar una emergencia.
                    EmergencyEvent event = detector.detectEmergency();

                    // 2. Procesamiento: Si se ha confirmado una emergencia...
                    if (event != null) {
                        processEmergency(event);
                    }
                    
                    // 3. Continuación: Se pregunta al usuario si desea realizar otra operación.
                    System.out.print("\n¿Desea reportar otra emergencia? (S/N): ");
                    String response = scanner.nextLine().trim();
                    if (!response.equalsIgnoreCase("S")) {
                        System.out.println("\n✅ Saliendo del sistema de emergencias. ¡Hasta pronto!");
                        break; // Sale del bucle while.
                    }
                    
                    System.out.println("\n" + "=".repeat(80) + "\n");
                    
                } catch (Exception e) {
                    // Captura errores dentro del bucle para que la aplicación no se detenga.
                    System.err.println("❌ Error en el ciclo principal: " + e.getMessage());
                    System.out.println("El sistema se ha recuperado. Intente nuevamente...\n");
                }
            }
        } catch (Exception e) {
            // Captura errores críticos que ocurran fuera del bucle principal.
            System.err.println("\n❌ Error crítico en el sistema: " + e.getMessage());
        }
    }

    /**
     * Procesa un evento de emergencia confirmado.
     * <p>
     * Este método coordina el registro, la alerta y las acciones post-alerta.
     * </p>
     *
     * @param event El evento de emergencia a procesar.
     */
    private void processEmergency(EmergencyEvent event) {
        try {
            // 2.1. Registro: Se guarda la emergencia en el log, obteniendo un ID único.
            String emergencyId = logger.logEmergency(event);
            System.out.println("\n✅ Emergencia registrada con ID: " + emergencyId);
            
            // 2.2. Alerta: Se envía la alerta a los servicios de emergencia.
            boolean alertSent = alertSender.send(event);
            
            if (alertSent) {
                // 2.3. Notificación: Se avisa a los contactos de emergencia del usuario.
                alertSender.notifyContacts(userData, event);

                // 2.4. Acción local: Si la emergencia es en Murcia, se ofrece info adicional.
                handleMurciaSpecifics(event.getLocation());

                System.out.println("\n✅ ¡Emergencia reportada con éxito!");
                System.out.println("Se ha creado un registro de la emergencia en el sistema.");

                // 2.5. Feedback: Se solicita al usuario que valore la experiencia.
                logger.collectAndLogFeedback(emergencyId, scanner);
                System.out.println("\n✅ Gracias por tu feedback. Nos ayuda a mejorar el sistema.");

            } else {
                // Manejo de fallo en el envío de la alerta principal.
                System.out.println("\n❌ No se pudo enviar la alerta. Por favor, intente nuevamente o llame al 112 manualmente.");
            }
        } catch (Exception e) {
            System.err.println("\n❌ Error al procesar la emergencia: " + e.getMessage());
        }
    }

    /**
     * Maneja la lógica específica si la ubicación de la emergencia es "Murcia".
     * <p>
     * Ofrece al usuario la posibilidad de ver una lista de centros de salud locales
     * cargados desde un archivo JSON.
     * </p>
     *
     * @param location La ubicación de la emergencia.
     */
    private void handleMurciaSpecifics(String location) {
        if (location != null && location.toLowerCase().contains("murcia")) {
            System.out.print("\n¿Quieres ver todos los centros de salud de Murcia? (S/N): ");
            String verCentros = scanner.nextLine().trim();
            if (verCentros.equalsIgnoreCase("S")) {
                // Carga los datos desde el archivo de recursos.
                List<CentroSalud> centros = CentroSaludUtils.cargarCentros("/CentrosdeSaludMurcia.json");
                if (centros != null && !centros.isEmpty()) {
                    System.out.println("\n=== CENTROS DE SALUD DE LA REGIÓN DE MURCIA ===");
                    for (CentroSalud centro : centros) {
                        System.out.printf("- %s | %s | Municipio: %s | Tel: %s%n",
                            centro.getNombre(), centro.getDireccion(), centro.getMunicipio(), centro.getTelefono());
                    }
                    System.out.println("============================================\n");
                } else {
                    System.out.println("No se encontraron centros de salud para mostrar.");
                }
            }
        }
    }
}

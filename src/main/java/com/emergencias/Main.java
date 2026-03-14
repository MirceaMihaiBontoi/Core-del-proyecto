package com.emergencias;

import com.emergencias.alert.AlertSender;
import com.emergencias.alert.EmergencyLogger;
import com.emergencias.controller.EmergencyManager;
import com.emergencias.detector.EmergencyDetector;
import com.emergencias.model.UserData;
import com.emergencias.services.AIClassifierClient;
import com.emergencias.services.IAlert;
import java.util.Scanner;

/**
 * Clase principal que inicia la aplicación y ensambla sus componentes (Inyección de Dependencias).
 */
public class Main {
    /**
     * Punto de entrada principal de la aplicación.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // 1. Comprobar entorno Python
            AIClassifierClient aiClient = checkPythonEnvironment(scanner);

            // 2. Crear los datos del usuario
            UserData userData = new UserData(
                "Juan Pérez",
                "+34 600 123 456",
                "Alergias: Ninguna\nTipo de sangre: A+",
                "María García (Madre): +34 600 654 321"
            );

            // 3. Crear las dependencias
            EmergencyDetector detector = new EmergencyDetector(userData, scanner, aiClient);
            IAlert alertSender = new AlertSender();
            EmergencyLogger logger = new EmergencyLogger();

            // 4. Inyectar las dependencias en el gestor principal
            EmergencyManager emergencyManager = new EmergencyManager(
                userData,
                scanner,
                detector,
                alertSender,
                logger
            );

            // 5. Iniciar el sistema
            emergencyManager.startSystem();
            
        } catch (Exception e) {
            System.err.println("\n=== ERROR CRÍTICO ===");
            System.err.println("Se ha producido un error inesperado: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            // Cerrar el recurso principal
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static AIClassifierClient checkPythonEnvironment(Scanner scanner) {
        System.out.println("=== COMPROBACION DEL ENTORNO ===\n");

        // Check 1: Python instalado
        if (!isCommandAvailable("python --version")) {
            System.out.println("❌ Python no esta instalado.");
            System.out.println("   Para instalar Python:");
            System.out.println("   - Windows: Descargue desde https://www.python.org/downloads/");
            System.out.println("   - Asegurese de marcar 'Add Python to PATH' durante la instalacion");
            System.out.println("\n   Sin Python se pierde:");
            System.out.println("   - Clasificacion de emergencias por IA (texto libre)");
            System.out.println("   - Correccion ortografica automatica");
            System.out.println("   - Geolocalizacion automatica por IP");
            System.out.println("   - Reconocimiento de voz");
            System.out.println("   - Instrucciones contextuales de actuacion");
            System.out.println("   El sistema funcionara en modo manual (menu basico).\n");
            System.out.print("¿Desea continuar sin Python? (S/N): ");
            if (scanner.nextLine().equalsIgnoreCase("S")) {
                return null;
            }
            System.out.println("Instale Python y reinicie la aplicacion.");
            System.exit(0);
        }
        System.out.println("✅ Python detectado.");

        // Check 2: Dependencias instaladas
        String[] dependencies = {"fastapi", "uvicorn", "sklearn", "spellchecker", "httpx", "sounddevice", "soundfile", "speech_recognition"};
        boolean allInstalled = true;
        StringBuilder missing = new StringBuilder();
        for (String dep : dependencies) {
            if (!isPythonModuleInstalled(dep)) {
                allInstalled = false;
                missing.append("   - ").append(dep).append("\n");
            }
        }

        if (!allInstalled) {
            System.out.println("❌ Faltan dependencias de Python:");
            System.out.println(missing);
            System.out.println("   Para instalarlas ejecute:");
            System.out.println("   pip install -r python-backend/requirements.txt\n");
            System.out.println("   Sin las dependencias se pierde la misma funcionalidad que sin Python.\n");
            System.out.print("¿Desea continuar sin las dependencias? (S/N): ");
            if (scanner.nextLine().equalsIgnoreCase("S")) {
                return null;
            }
            System.out.println("Instale las dependencias y reinicie la aplicacion.");
            System.exit(0);
        }
        System.out.println("✅ Dependencias de Python instaladas.");

        // Check 3: Servidor corriendo
        AIClassifierClient aiClient = new AIClassifierClient("http://localhost:8000");
        if (!aiClient.isAvailable()) {
            System.out.println("❌ El servidor Python no esta arrancado.");
            System.out.println("   Para arrancarlo ejecute en otra terminal:");
            System.out.println("   PowerShell: cd python-backend; python -m uvicorn server:app --host 0.0.0.0 --port 8000");
            System.out.println("   CMD:        cd python-backend && python -m uvicorn server:app --host 0.0.0.0 --port 8000\n");
            System.out.println("   Sin el servidor se pierde la misma funcionalidad que sin Python.\n");
            System.out.print("¿Desea continuar sin el servidor? (S/N): ");
            if (scanner.nextLine().equalsIgnoreCase("S")) {
                return null;
            }
            System.out.println("Arranque el servidor y reinicie la aplicacion.");
            System.exit(0);
        }
        System.out.println("✅ Servidor Python activo.");

        System.out.println("\n✅ Entorno completo. Todas las funcionalidades disponibles.\n");
        return aiClient;
    }

    private static boolean isCommandAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command.split(" ")).start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPythonModuleInstalled(String module) {
        try {
            Process process = new ProcessBuilder("python", "-c", "import " + module).start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

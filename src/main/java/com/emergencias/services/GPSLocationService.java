package com.emergencias.services;

/**
 * <h1>Implementación Simulada de un Servicio de Ubicación GPS</h1>
 *
 * <p>
 * Esta clase es una implementación <b>simulada</b> de la interfaz {@link ILocationService}.
 * Su propósito es actuar como un sustituto (placeholder) de un servicio de GPS real,
 * permitiendo que la aplicación se desarrolle y pruebe sin necesidad de un
 * dispositivo físico con GPS.
 * </p>
 *
 * <p>
 * En una aplicación real de Android, esta clase sería reemplazada por una que
 * utilice las APIs de geolocalización de Google Play Services o del sistema operativo.
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public class GPSLocationService implements ILocationService {
    // --- ESTADO Y VALORES POR DEFECTO ---
    private boolean hasPermission = false; // Simula si el usuario ha concedido el permiso.
    private static final String DEFAULT_COORDINATES = "37.9922° N, 1.1307° W"; // Coordenadas de Murcia
    private static final String DEFAULT_LOCATION = "Plaza del Cardenal Belluga, Murcia";

    /**
     * Devuelve las coordenadas GPS simuladas.
     * <p>
     * Si no se ha concedido el permiso, lo indica y devuelve las coordenadas por defecto.
     * En un caso real, este método activaría el hardware del GPS para obtener una lectura.
     * </p>
     *
     * @return Una cadena con las coordenadas por defecto.
     */
    @Override
    public String getCoordinates() {
        if (!hasPermission) {
            System.out.println("⚠️  Permiso de ubicación no concedido. Devolviendo coordenadas por defecto.");
        }
        System.out.println("🛰️  GPS simulado: Obteniendo coordenadas...");
        return DEFAULT_COORDINATES;
    }

    /**
     * Comprueba si el permiso de ubicación ha sido concedido (en esta simulación).
     *
     * @return El estado actual del permiso simulado.
     */
    @Override
    public boolean hasLocationPermission() {
        return hasPermission;
    }

    /**
     * Simula la solicitud de permiso de ubicación al usuario.
     * <p>
     * En esta simulación, el permiso siempre se concede automáticamente.
     * </p>
     *
     * @return Siempre devuelve {@code true}.
     */
    @Override
    public boolean requestPermission() {
        if (!hasPermission) {
            System.out.println("📍 Solicitando permiso para acceder a la ubicación del dispositivo...");
            this.hasPermission = true; // El permiso se concede automáticamente en la simulación.
            System.out.println("✅ Permiso de ubicación concedido.");
        }
        return true;
    }

    /**
     * Devuelve una descripción textual de la ubicación simulada.
     * <p>
     * Si no tiene permiso, lo solicita primero.
     * </p>
     *
     * @return Una cadena con la descripción de la ubicación por defecto.
     */
    @Override
    public String getLocationDescription() {
        if (!hasPermission) {
            requestPermission();
        }
        System.out.println("🌍  GPS simulado: Obteniendo descripción de la ubicación...");
        return DEFAULT_LOCATION;
    }
}

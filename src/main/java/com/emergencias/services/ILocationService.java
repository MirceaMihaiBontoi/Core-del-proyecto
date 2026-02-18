package com.emergencias.services;

/**
 * <h1>Contrato para Servicios de Ubicación</h1>
 *
 * <p>
 * Esta interfaz define las operaciones que un servicio de geolocalización
 * debe proporcionar. Abstrae la forma en que se obtiene la ubicación,
 * permitiendo diferentes implementaciones (estrategias).
 * </p>
 *
 * <p>
 * Por ejemplo, podríamos tener:
 * <ul>
 *     <li>{@code GPSLocationService}: Una implementación que obtiene la ubicación del GPS del dispositivo.</li>
 *     <li>{@code NetworkLocationService}: Una que la obtiene a través de redes Wi-Fi o móviles.</li>
 *     <li>{@code ManualLocationService}: Una que simplemente pregunta la ubicación al usuario por texto.</li>
 * </ul>
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public interface ILocationService {

    /**
     * Obtiene las coordenadas geográficas actuales (latitud y longitud).
     *
     * @return Una cadena de texto con las coordenadas, por ejemplo, "40.4167, -3.70325".
     *         Podría devolver null o lanzar una excepción si la ubicación no está disponible.
     */
    String getCoordinates();
    
    /**
     * Verifica si la aplicación tiene los permisos necesarios para acceder a la ubicación.
     * <p>
     * En un entorno real (como Android), esto implicaría comprobar si el usuario
     * ha concedido {@code ACCESS_FINE_LOCATION}.
     * </p>
     *
     * @return {@code true} si los permisos están concedidos, {@code false} en caso contrario.
     */
    boolean hasLocationPermission();
    
    /**
     * Inicia el proceso para solicitar al usuario los permisos de ubicación.
     * <p>
     * En una aplicación de consola, esto podría ser una simple pregunta. En una
     * aplicación móvil, lanzaría el diálogo de permisos del sistema operativo.
     * </p>
     *
     * @return {@code true} si el usuario concede el permiso, {@code false} si lo deniega.
     */
    boolean requestPermission();
    
    /**
     * Obtiene una descripción textual de la ubicación actual.
     * <p>
     * Esto podría implicar un proceso de geocodificación inversa para convertir
     * coordenadas en una dirección postal (ej. "Calle Mayor, 1, Madrid").
     * </p>
     *
     * @return Una cadena con la descripción de la ubicación, o un mensaje de error si no se puede obtener.
     */
    String getLocationDescription();
}

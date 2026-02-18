package com.emergencias.services;

/**
 * <h1>Contrato para Servicios de Registro (Logging)</h1>
 *
 * <p>
 * Esta interfaz define las operaciones estándar que cualquier servicio de logging
 * en la aplicación debe implementar. Al igual que {@link IAlert}, sigue el
 * <b>Patrón de Diseño Strategy</b>, permitiendo que las implementaciones de
 * registro sean intercambiables.
 * </p>
 *
 * <p>
 * Por ejemplo, podríamos tener:
 * <ul>
 *     <li>{@code FileLogger}: Una implementación que escribe los logs en archivos de texto.</li>
 *     <li>{@code DatabaseLogger}: Una que guarda los registros en una base de datos.</li>
 *     <li>{@code ConsoleLogger}: Una que simplemente imprime los mensajes en la consola.</li>
 * </ul>
 * </p>
 *
 * @author MirceaMihaiBontoi (Documentado por Davgaltol)
 * @version 1.1
 * @since 2023-10-27
 */
public interface ILogger {

    /**
     * Registra un mensaje de nivel informativo.
     * <p>
     * Se utiliza para registrar eventos generales del flujo de la aplicación,
     * como el inicio de un proceso o la finalización de una tarea.
     * </p>
     *
     * @param message El mensaje informativo a registrar.
     */
    void logInfo(String message);
    
    /**
     * Registra un mensaje de advertencia.
     * <p>
     * Se utiliza para notificar sobre situaciones inesperadas pero que no
     * impiden que la aplicación continúe funcionando, como un valor por
     * defecto que se está utilizando.
     * </p>
     *
     * @param message El mensaje de advertencia a registrar.
     */
    void logWarning(String message);
    
    /**
     * Registra un error o una excepción.
     * <p>
     * Se utiliza para registrar fallos que han ocurrido en la aplicación.
     * Es crucial para la depuración y el monitoreo de problemas.
     * </p>
     *
     * @param message   Un mensaje descriptivo del contexto del error.
     * @param exception La excepción que fue capturada (puede ser null).
     */
    void logError(String message, Exception exception);
    
    /**
     * Devuelve la ubicación donde se están guardando los logs.
     * <p>
     * Puede ser una ruta de archivo, el nombre de una tabla de base de datos,
     * o simplemente "Consola".
     * </p>
     *
     * @return Una cadena que describe la ubicación de los registros.
     */
    String getLogLocation();
}

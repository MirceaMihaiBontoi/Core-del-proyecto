package com.emergencias.services;

/**
 * Interfaz que define el contrato para servicios de logging.
 * Permite diferentes estrategias de registro (archivo, base de datos, etc).
 */
public interface ILogger {
    /**
     * Registra un mensaje informativo.
     */
    void logInfo(String message);
    
    /**
     * Registra un mensaje de advertencia.
     */
    void logWarning(String message);
    
    /**
     * Registra un error.
     */
    void logError(String message, Exception exception);
    
    /**
     * Obtiene el archivo o recurso donde se guardan los logs.
     */
    String getLogLocation();
}

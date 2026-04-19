package com.soteria.core.interfaces;

/**
 * Interface that defines the contract for history logging services.
 * Allows different registration strategies (file, database, etc).
 */
public interface HistoryLogger {
    void logInfo(String message);
    void logWarning(String message);
    void logError(String message, Exception exception);
    String getLogLocation();
}

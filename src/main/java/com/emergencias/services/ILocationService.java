package com.emergencias.services;

/**
 * Interfaz que define el contrato para servicios de ubicación/GPS.
 * Permite obtener la ubicación actual del usuario de forma segura.
 */
public interface ILocationService {
    /**
     * Obtiene las coordenadas actuales de ubicación.
     */
    String getCoordinates();
    
    /**
     * Verifica si el usuario ha otorgado permiso para acceder a la ubicación.
     */
    boolean hasLocationPermission();
    
    /**
     * Solicita permiso al usuario para acceder a la ubicación.
     */
    boolean requestPermission();
    
    /**
     * Obtiene la ubicación con descripción de lugar (ej: "Plaza Mayor, Madrid").
     */
    String getLocationDescription();
}

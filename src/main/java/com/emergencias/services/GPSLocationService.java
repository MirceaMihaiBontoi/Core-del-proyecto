package com.emergencias.services;

/**
 * Implementación de ILocationService simulada.
 * En una aplicación real, se integraría con APIs de GPS del dispositivo.
 */
public class GPSLocationService implements ILocationService {
    private boolean hasPermission = false;
    private static final String DEFAULT_COORDINATES = "40.4168° N, 3.7038° O";
    private static final String DEFAULT_LOCATION = "Plaza Mayor, Madrid";

    @Override
    public String getCoordinates() {
        if (!hasPermission) {
            System.out.println("⚠️  Permiso de ubicación no otorgado. Usando ubicación por defecto.");
            return DEFAULT_COORDINATES;
        }
        return DEFAULT_COORDINATES;
    }

    @Override
    public boolean hasLocationPermission() {
        return hasPermission;
    }

    @Override
    public boolean requestPermission() {
        System.out.println("📍 Solicitando permiso de ubicación...");
        this.hasPermission = true;
        System.out.println("✅ Permiso de ubicación otorgado.");
        return true;
    }

    @Override
    public String getLocationDescription() {
        if (!hasPermission) {
            requestPermission();
        }
        return DEFAULT_LOCATION;
    }
}

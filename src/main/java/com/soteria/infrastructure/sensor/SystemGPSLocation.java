package com.soteria.infrastructure.sensor;

import com.soteria.core.interfaces.LocationProvider;

/**
 * Simulated implementation of LocationProvider.
 * In a real application, it would integrate with device GPS APIs.
 */
public class SystemGPSLocation implements LocationProvider {
    private boolean hasPermission = false;
    private static final String DEFAULT_COORDINATES = "40.4168° N, 3.7038° W";
    private static final String DEFAULT_LOCATION = "Plaza Mayor, Madrid";

    @Override
    public String getCoordinates() {
        if (!hasPermission) {
            System.out.println("⚠️ Location permission not granted. Using default location.");
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
        System.out.println("📍 Requesting location permission...");
        this.hasPermission = true;
        System.out.println("✅ Location permission granted.");
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

package com.soteria.core.interfaces;

/**
 * Interface that defines the contract for location/GPS services.
 * Allows retrieving the user's current location securely.
 */
public interface LocationProvider {
    String getCoordinates();
    boolean hasLocationPermission();
    boolean requestPermission();
    String getLocationDescription();
}

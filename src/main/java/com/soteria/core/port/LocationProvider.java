package com.soteria.core.port;

/**
 * Port for location/GPS services.
 */
public interface LocationProvider {

    String getCoordinates();

    boolean hasLocationPermission();

    boolean requestPermission();

    String getLocationDescription();
}

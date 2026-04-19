package com.emergencias.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GPSLocationServiceTest {

    private GPSLocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new GPSLocationService();
    }

    @Test
    @DisplayName("Sin permiso, las coordenadas son las de por defecto")
    void defaultCoordinatesWithoutPermission() {
        assertFalse(locationService.hasLocationPermission());
        String coords = locationService.getCoordinates();
        assertTrue(coords.contains("40.4168")); // Madrid por defecto
    }

    @Test
    @DisplayName("Al solicitar permiso, se otorga correctamente")
    void requestPermissionWorks() {
        assertTrue(locationService.requestPermission());
        assertTrue(locationService.hasLocationPermission());
    }

    @Test
    @DisplayName("Al pedir la descripción de ubicación, solicita permiso automáticamente")
    void getLocationDescriptionRequestsPermission() {
        assertFalse(locationService.hasLocationPermission());
        String desc = locationService.getLocationDescription();
        assertEquals("Plaza Mayor, Madrid", desc);
        assertTrue(locationService.hasLocationPermission());
    }
}

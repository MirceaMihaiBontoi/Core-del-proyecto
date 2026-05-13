package com.soteria.infrastructure.sensor;

import com.soteria.core.port.LocationProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Best-effort implementation of {@link LocationProvider} for desktop environments.
 *
 * <p>Tries three strategies in order, stopping at the first that succeeds:
 * <ol>
 *   <li>Windows {@code GeoCoordinateWatcher} via a PowerShell subprocess</li>
 *   <li>IP-based geolocation via {@code ip-api.com} (requires internet)</li>
 *   <li>Hard-coded fallback to Madrid for Linux/Android simulation</li>
 * </ol>
 * The result is cached after the first successful call so subsequent reads
 * are free.</p>
 *
 * <p>All callers must handle {@code "Unknown"} — location is best-effort and
 * is used only to pre-select a language and to enrich emergency alerts.</p>
 */
public class SystemGPSLocation implements LocationProvider {

    private static final Logger log = Logger.getLogger(SystemGPSLocation.class.getName());
    private static final String UNKNOWN = "Unknown";

    private boolean hasPermission = false;
    private String cachedCoordinates = null;

    @Override
    public String getCoordinates() {
        if (cachedCoordinates != null)
            return cachedCoordinates;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cachedCoordinates = getWindowsLocation();
        } else if (os.contains("android") || os.contains("linux")) {
            // Android/Linux bridge — to be implemented with native JNI or sidecar.
            log.info("Mobile/Linux environment detected. Placeholder for native geolocator.");
            cachedCoordinates = "40.4168° N, 3.7038° W"; // Default to Madrid for simulation
        } else {
            cachedCoordinates = UNKNOWN;
        }

        return cachedCoordinates;
    }

    private String getWindowsLocation() {
        log.info("Detecting location via Windows GeoCoordinateWatcher...");
        String psCommand = "Add-Type -AssemblyName System.Device; " +
                "$w = New-Object System.Device.Location.GeoCoordinateWatcher; " +
                "$w.Start(); $cnt = 0; " +
                "while (($w.Status -ne 1) -and ($cnt -lt 30)) { Start-Sleep -Milliseconds 100; $cnt++ }; " +
                "$pos = $w.Position.Location; $status = $w.Status; $w.Stop(); " +
                "if ($status -eq 1 -and -not $pos.IsUnknown) { \"$($pos.Latitude),$($pos.Longitude)\" } else { 'Unknown' }";

        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psCommand);
            Process p = pb.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().equals(UNKNOWN)) {
                    hasPermission = true;
                    return line.trim();
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "GPS sensor failed, attempting IP-based fallback...");
        }

        return getIPLocation();
    }

    private String getIPLocation() {
        log.info("GPS unavailable. Fetching location via IP...");
        try {
            java.net.URL url = java.net.URI.create("http://ip-api.com/line/?fields=lat,lon").toURL();
            try (java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(url.openStream()))) {
                String lat = in.readLine();
                String lon = in.readLine();
                if (lat != null && lon != null) {
                    hasPermission = true;
                    return lat.trim() + "," + lon.trim();
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "IP-based location failed", e);
        }
        return UNKNOWN;
    }

    /**
     * Infers the most likely display language from the detected coordinates.
     *
     * <p>Falls back to the JVM default locale when coordinates are unavailable.
     * Currently only maps Spain's geographic bounding box to Spanish; all other
     * coordinates resolve to English. Extend the coordinate ranges here as
     * more languages are added to the onboarding picker.</p>
     *
     * @return display-name language string compatible with the onboarding picker
     *         (e.g. {@code "Spanish"}, {@code "English"})
     */
    public String detectPrimaryLanguage() {
        String coords = getCoordinates();
        if (coords == null || coords.equals(UNKNOWN)) {
            String lang = java.util.Locale.getDefault().getLanguage().toLowerCase();
            return switch (lang) {
                case "es" -> "Spanish";
                case "fr" -> "French";
                case "de" -> "German";
                case "it" -> "Italian";
                case "pt" -> "Portuguese";
                default -> "English";
            };
        }

        try {
            String[] parts = coords.split(",");
            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);

            // Spain mainland: lat 35–44, lon -10 to 5
            // Canary Islands:  lat 27–30, lon -19 to -13
            boolean isMainland = (lat >= 35.0 && lat <= 44.0 && lon >= -10.0 && lon <= 5.0);
            boolean isCanaries = (lat >= 27.0 && lat <= 30.0 && lon >= -19.0 && lon <= -13.0);

            if (isMainland || isCanaries) {
                return "Spanish";
            }
        } catch (Exception e) {
            log.warning("Could not parse coordinates for language detection: " + coords);
        }

        return "English";
    }

    @Override
    public boolean hasLocationPermission() {
        return hasPermission;
    }

    @Override
    public boolean requestPermission() {
        log.info("Requesting location permission...");
        this.hasPermission = true;
        return true;
    }

    @Override
    public String getLocationDescription() {
        String coords = getCoordinates();
        if (coords.equals(UNKNOWN))
            return "Unknown Location";
        if (coords.contains("40.4168") || coords.contains("3.7038"))
            return "Madrid, Spain";
        return "Detected Location (" + coords + ")";
    }
}

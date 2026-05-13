package com.soteria.infrastructure.sensor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Best-effort read of the device's own phone number.
 *
 * <p>On Windows, queries WMI for modem-based mobile broadband devices.
 * Most desktops and laptops have no such hardware, so {@link #UNKNOWN} is
 * the common result. On Android (future), will delegate to
 * {@code TelephonyManager.getLine1Number()} once the native bridge is wired.
 * On Linux, no telephony API is available.</p>
 *
 * <p>All callers must treat the result as possibly {@link #UNKNOWN} — the
 * phone number is used for display and as a hand-off hint to 112 operators;
 * it is not required to complete onboarding.</p>
 */
public final class DevicePhoneDetector {

    /** Sentinel returned when the phone number cannot be determined. */
    public static final String UNKNOWN = "Unknown";

    private static final Logger log = Logger.getLogger(DevicePhoneDetector.class.getName());

    private DevicePhoneDetector() { }

    public static String detect() {
        return detect(System.getProperty("os.name", ""));
    }

    /**
     * Overload that accepts an explicit OS name string, used by tests to
     * exercise platform branches without spawning real subprocesses.
     */
    static String detect(String osName) {
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.contains("android")) {
            // Placeholder for native bridge (TelephonyManager#getLine1Number).
            return UNKNOWN;
        }
        if (os.contains("win")) {
            return detectWindows();
        }
        if (os.contains("linux")) {
            // Linux desktops have no telephony hardware; skip detection entirely.
            return UNKNOWN;
        }
        return UNKNOWN;
    }

    private static String detectWindows() {
        // Targets USB/integrated mobile broadband modems. Most desktops/laptops
        // won't have one — the WMI query returns nothing and we fall through to UNKNOWN.
        String psCommand =
                "$ErrorActionPreference='SilentlyContinue'; " +
                "$m = Get-CimInstance -Namespace root\\cimv2\\mdm -ClassName MDM_RemoteAccess_NumericAddress 2>$null; " +
                "if ($m) { $m | Select-Object -First 1 -ExpandProperty Address }";
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", psCommand);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                p.waitFor();
                if (line != null && !line.isBlank() && !line.equalsIgnoreCase(UNKNOWN)) {
                    return line.trim();
                }
            }
        } catch (InterruptedException e) {
            log.log(Level.FINE, "Windows phone detection interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.log(Level.FINE, "Windows phone detection failed (non-fatal)", e);
        }
        return UNKNOWN;
    }
}

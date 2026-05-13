# Sensor Package

## Responsibility

Reads physical or network-derived signals from the host device (location and phone number) and exposes them to the rest of the application through the port interfaces defined in `core.port`. 
Both reads are best-effort: failures return sentinel values rather than throwing, as neither piece of data is strictly required for the system to function.

## Structure

```
sensor/
├── SystemGPSLocation.java   # LocationProvider implementation — coordinates + language hint
└── DevicePhoneDetector.java # Best-effort phone number read — utility class, no port
```

## Data Flow & Integration

1. **Inbound Flow:** Collects location and telemetry data via OS-level commands (PowerShell/WMI) or external HTTP APIs (ip-api).
2. **Outbound Flow:** Returns enriched data strings (Coordinates, Country, Language hints) or sentinel values (e.g., "Unknown") back to the application layer.
3. **Integration:** Implements `core.port.LocationProvider`. It is entirely self-contained and avoids importing other infrastructure sub-packages.

## Lifecycle

1. **Initialization:** Instantiated directly or injected. Caches values internally on first access.
2. **Execution:** 
   - `SystemGPSLocation`: Attempts Windows GeoCoordinateWatcher, falls back to IP geolocation, and finally falls back to hardcoded defaults (Madrid) if all fail.
   - `DevicePhoneDetector`: Queries WMI (Windows) or TelephonyManager (Android), returning "UNKNOWN" silently on error or unsupported platforms (Linux).
3. **Caching:** Because location processes are slow (PowerShell subprocesses and HTTP network calls), results are cached for the lifetime of the process.

## Design Decisions

- **Why PowerShell for GPS instead of a Java library?**
  Java SE lacks native GPS APIs. On Windows, `System.Device.Location` is a .NET assembly. Spawning a PowerShell subprocess is the least-invasive pure-Java mechanism to query it.
- **Why return `"Unknown"` instead of `Optional` or throwing?**
  Location and phone numbers are enrichment data. Returning a sentinel keeps callers simple, preventing the emergency flow from halting or forcing the UI to handle `Optional` unwraps for non-critical data.
- **Why detect primary language?**
  `SystemGPSLocation` implements `detectPrimaryLanguage()` mapped to geographic bounding boxes. This helps `OnboardingController` pre-select the appropriate locale UI.

package com.soteria.infrastructure.intelligence.system;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects the host operating system and architecture at runtime to determine
 * which native binary extensions (.dll or .so) and paths to resolve.
 * <p>
 * This normalizes common architectures (e.g. amd64/x86_64 to x64) to ensure
 * consistency when querying the packaged native resources.
 * </p>
 */
public final class PlatformDetector {
    
    private static final Logger logger = Logger.getLogger(PlatformDetector.class.getName());
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
    
    private PlatformDetector() {
        // Prevent instantiation
    }
    
    public static String detectOS() {
        if (OS_NAME.contains("win")) {
            return "windows";
        } else if (OS_NAME.contains("nux") || OS_NAME.contains("nix") || OS_NAME.contains("aix")) {
            return "linux";
        } else {
            return "unknown";
        }
    }
    
    public static String detectArchitecture() {
        // Normalize common architecture names
        if (OS_ARCH.equals("amd64") || OS_ARCH.equals("x86_64")) {
            return "x64";
        } else if (OS_ARCH.equals("aarch64") || OS_ARCH.equals("arm64")) {
            return "arm64";
        } else {
            return OS_ARCH;
        }
    }
    
    public static String getPlatformIdentifier() {
        String os = detectOS();
        logger.log(Level.CONFIG, "Detected platform: {0}, architecture: {1}", 
            new Object[]{os, detectArchitecture()});
        return os;
    }
    
    public static String getLibraryExtension() {
        String os = detectOS();
        switch (os) {
            case "windows":
                return ".dll";
            case "linux":
                return ".so";
            default:
                logger.log(Level.WARNING, "Unknown platform: {0}, defaulting to .so", os);
                return ".so";
        }
    }
}

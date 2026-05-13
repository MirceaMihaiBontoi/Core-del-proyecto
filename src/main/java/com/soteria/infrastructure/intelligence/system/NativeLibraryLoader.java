package com.soteria.infrastructure.intelligence.system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized loader for native libraries used by sherpa-onnx.
 * 
 * <p>Bypasses standard {@code java.library.path} resolution in favor of absolute paths
 * to prevent classloader stability issues. For Linux, it strictly enforces the
 * dependency loading order (onnxruntime -> cxx-api -> c-api -> jni) to avoid
 * {@code UnsatisfiedLinkError} during symbol resolution by {@code ld.so}.</p>
 */
public class NativeLibraryLoader {

    private static final Logger logger = Logger.getLogger(NativeLibraryLoader.class.getName());

    private NativeLibraryLoader() {
        // Utility class
    }

    public static synchronized void load() {
        logger.fine("load() called");

        try {
            String platform = PlatformDetector.getPlatformIdentifier();
            logger.log(Level.FINE, "Platform detected: {0}", platform);
            logger.log(Level.CONFIG, "Loading native libraries for platform: {0}", platform);

            if ("windows".equals(platform)) {
                loadWindows();
            } else if ("linux".equals(platform)) {
                loadLinux();
            } else {
                throw new IllegalStateException("Unsupported platform: " + platform);
            }

            logger.info("All libraries loaded successfully");
        } catch (LinkageError | Exception t) {
            logger.log(Level.SEVERE, "CRITICAL: Failed to load native libraries for SoterIA. Audio services will fail.", t);
            // Don't throw - let the application continue and fail later with a more specific error
        }
    }

    private static void loadWindows() {
        // Try system path first
        if (tryLoadFromSystem()) {
            return;
        }

        // Priority 2: Platform-specific directory (lib/sherpa-onnx/windows)
        String userDir = System.getProperty("user.dir");
        Path platformDir = Paths.get(userDir, "lib", "sherpa-onnx", "windows");

        if (Files.isDirectory(platformDir)) {
            Path ortPath = platformDir.resolve("onnxruntime.dll");
            Path jniPath = platformDir.resolve("sherpa-onnx-jni.dll");

            if (Files.exists(ortPath)) {
                System.load(ortPath.toAbsolutePath().toString());
                logger.log(Level.INFO, "Loaded onnxruntime from: {0}", ortPath);
            }

            if (Files.exists(jniPath)) {
                System.load(jniPath.toAbsolutePath().toString());
                logger.log(Level.INFO, "Loaded sherpa-onnx-jni from: {0}", jniPath);
                return;
            }
        }

        // Priority 3: Fallback to lib/native for backward compatibility
        Path nativeDir = Paths.get(userDir, "lib", "native");
        Path ortPath = nativeDir.resolve("onnxruntime.dll");
        Path jniPath = nativeDir.resolve("sherpa-onnx-jni.dll");

        if (Files.exists(ortPath)) {
            System.load(ortPath.toAbsolutePath().toString());
            logger.log(Level.INFO, "Loaded onnxruntime from fallback: {0}", ortPath);
        }

        if (Files.exists(jniPath)) {
            System.load(jniPath.toAbsolutePath().toString());
            logger.log(Level.INFO, "Loaded sherpa-onnx-jni from fallback: {0}", jniPath);
        }
    }

    private static void loadLinux() {
        logger.fine("loadLinux() starting...");
        
        // Try system path first
        if (tryLoadFromSystem()) {
            logger.info("Loaded from system path");
            return;
        }

        // On Linux, use System.load() with absolute paths since modifying java.library.path
        // after JVM startup doesn't work reliably
        String userDir = System.getProperty("user.dir");
        Path platformDir = Paths.get(userDir, "lib", "sherpa-onnx", "linux");
        logger.log(Level.FINE, "Platform directory: {0}", platformDir);

        if (!Files.isDirectory(platformDir)) {
            throw new IllegalStateException("Linux native library directory not found: " + platformDir);
        }

        // Load libraries in dependency order
        Path ortPath = platformDir.resolve("libonnxruntime.so");
        Path cxxApiPath = platformDir.resolve("libsherpa-onnx-cxx-api.so");
        Path cApiPath = platformDir.resolve("libsherpa-onnx-c-api.so");
        Path jniPath = platformDir.resolve("libsherpa-onnx-jni.so");

        if (!Files.exists(ortPath)) {
            throw new IllegalStateException("Missing libonnxruntime.so in: " + platformDir);
        }
        if (!Files.exists(cxxApiPath)) {
            throw new IllegalStateException("Missing libsherpa-onnx-cxx-api.so in: " + platformDir);
        }
        if (!Files.exists(cApiPath)) {
            throw new IllegalStateException("Missing libsherpa-onnx-c-api.so in: " + platformDir);
        }
        if (!Files.exists(jniPath)) {
            throw new IllegalStateException("Missing libsherpa-onnx-jni.so in: " + platformDir);
        }

        // Load in dependency order: onnxruntime -> cxx-api -> c-api -> jni
        logger.fine("Loading libonnxruntime.so...");
        System.load(ortPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded onnxruntime from: {0}", ortPath);

        logger.fine("Loading libsherpa-onnx-cxx-api.so...");
        System.load(cxxApiPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded sherpa-onnx-cxx-api from: {0}", cxxApiPath);

        logger.fine("Loading libsherpa-onnx-c-api.so...");
        System.load(cApiPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded sherpa-onnx-c-api from: {0}", cApiPath);

        logger.fine("Loading libsherpa-onnx-jni.so...");
        System.load(jniPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded sherpa-onnx-jni from: {0}", jniPath);
        
        logger.info("All Linux libraries loaded successfully");
    }

    private static boolean tryLoadFromSystem() {
        try {
            System.loadLibrary("onnxruntime");
            System.loadLibrary("sherpa-onnx-jni");
            logger.info("Native libraries loaded from system path");
            return true;
        } catch (UnsatisfiedLinkError e) {
            logger.log(Level.FINE, "System path load failed, using platform-specific directory: {0}", e.getMessage());
            return false;
        }
    }
}

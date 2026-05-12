package com.soteria.infrastructure.intelligence.system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized loader for native libraries used by sherpa-onnx.
 * Supports both Windows and Linux platforms with platform-specific library loading strategies.
 */
public class NativeLibraryLoader {

    private static final Logger logger = Logger.getLogger(NativeLibraryLoader.class.getName());

    private NativeLibraryLoader() {
        // Utility class
    }

    public static synchronized void load() {
        System.out.println("[NativeLibraryLoader] load() called");
        System.out.flush();

        try {
            String platform = PlatformDetector.getPlatformIdentifier();
            System.out.println("[NativeLibraryLoader] Platform detected: " + platform);
            System.out.flush();
            logger.log(Level.CONFIG, "Loading native libraries for platform: {0}", platform);

            if ("windows".equals(platform)) {
                loadWindows();
            } else if ("linux".equals(platform)) {
                loadLinux();
            } else {
                throw new IllegalStateException("Unsupported platform: " + platform);
            }

            System.out.println("[NativeLibraryLoader] All libraries loaded successfully");
            System.out.flush();
        } catch (LinkageError | Exception t) {
            System.err.println("[NativeLibraryLoader] CRITICAL FAILURE: " + t.getMessage());
            System.err.flush();
            t.printStackTrace();
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
        System.out.println("[NativeLibraryLoader] loadLinux() starting...");
        System.out.flush();
        
        // Try system path first
        if (tryLoadFromSystem()) {
            System.out.println("[NativeLibraryLoader] Loaded from system path");
            System.out.flush();
            return;
        }

        // On Linux, use System.load() with absolute paths since modifying java.library.path
        // after JVM startup doesn't work reliably
        String userDir = System.getProperty("user.dir");
        Path platformDir = Paths.get(userDir, "lib", "sherpa-onnx", "linux");
        System.out.println("[NativeLibraryLoader] Platform directory: " + platformDir);
        System.out.flush();

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
        System.out.println("[NativeLibraryLoader] Loading libonnxruntime.so...");
        System.out.flush();
        System.load(ortPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded onnxruntime from: {0}", ortPath);

        System.out.println("[NativeLibraryLoader] Loading libsherpa-onnx-cxx-api.so...");
        System.out.flush();
        System.load(cxxApiPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded sherpa-onnx-cxx-api from: {0}", cxxApiPath);

        System.out.println("[NativeLibraryLoader] Loading libsherpa-onnx-c-api.so...");
        System.out.flush();
        System.load(cApiPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded sherpa-onnx-c-api from: {0}", cApiPath);

        System.out.println("[NativeLibraryLoader] Loading libsherpa-onnx-jni.so...");
        System.out.flush();
        System.load(jniPath.toAbsolutePath().toString());
        logger.log(Level.INFO, "Loaded sherpa-onnx-jni from: {0}", jniPath);
        
        System.out.println("[NativeLibraryLoader] All Linux libraries loaded successfully");
        System.out.flush();
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

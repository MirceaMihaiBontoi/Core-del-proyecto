package com.soteria.infrastructure.intelligence.llm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads the {@code jllama} native built from SoterIA's tracked fork of java-llama.cpp.
 *
 * <p>The Maven artifact {@code de.kherud:llama} supplies the Java API ({@code de.kherud.llama});
 * the native must match that fork's llama.cpp (build under {@code vendor/java-llama.cpp}
 * or copy binaries into {@code lib/llama/}). If the platform-named library exists (e.g.
 * {@code libjllama.so} on Linux, from {@link System#mapLibraryName} for the logical name
 * {@code jllama}), this class sets {@link #LIB_PATH_PROPERTY} to that directory before any model
 * loads. Same convention as {@code de.kherud.llama.LlamaLoader}, which joins the property path with
 * the mapped native file name. An explicit {@code -Dde.kherud.llama.lib.path=...} is never
 * overwritten.</p>
 */
public final class LlamaNativeBootstrap {

    /** System property read by {@code de.kherud.llama} JNI loaders (unchanged for binary compatibility). */
    public static final String LIB_PATH_PROPERTY = "de.kherud.llama.lib.path";

    private static final Logger log = Logger.getLogger(LlamaNativeBootstrap.class.getName());

    private LlamaNativeBootstrap() {}

    /**
     * Sets {@value #LIB_PATH_PROPERTY} to {@code lib/llama/} if the platform-specific native
     * exists there and the property has not already been set.
     *
     * <p>Idempotent: safe to call multiple times. An explicit
     * {@code -Dde.kherud.llama.lib.path=...} on the JVM command line is never overwritten.</p>
     */
    public static void applyIfNeeded() {
        String existing = System.getProperty(LIB_PATH_PROPERTY);
        if (existing != null && !existing.isBlank()) {
            return;
        }
        Path libDir = Path.of(System.getProperty("user.dir", "."), "lib", "llama").toAbsolutePath().normalize();
        if (!Files.isDirectory(libDir)) {
            return;
        }
        String nativeName = System.mapLibraryName("jllama");
        if (!Files.isRegularFile(libDir.resolve(nativeName))) {
            return;
        }
        System.setProperty(LIB_PATH_PROPERTY, libDir.toString());
        log.log(Level.CONFIG, () -> "Using jllama native from " + libDir + " (fork build; see lib/llama/BUILD.md)");
    }
}

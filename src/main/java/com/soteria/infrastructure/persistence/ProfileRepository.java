package com.soteria.infrastructure.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soteria.core.model.UserData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the single device-owner profile as JSON at {@code ~/.soteria/profile.json}.
 *
 * <p>SoterIA is a single-user application with no authentication — the device
 * owner is the only user. On first launch the onboarding wizard collects the
 * profile; every subsequent launch skips straight to the chat screen.
 * {@link #exists()} is the gate that drives that decision.</p>
 */
public class ProfileRepository {

    private static final Logger log = Logger.getLogger(ProfileRepository.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Path profileFile;

    public ProfileRepository() {
        this(Paths.get(System.getProperty("user.home"), ".soteria", "profile.json"));
    }

    public ProfileRepository(Path profileFile) {
        this.profileFile = profileFile;
    }

    public boolean exists() {
        return Files.exists(profileFile);
    }

    /**
     * Loads the profile from disk.
     *
     * <p>Returns {@link Optional#empty()} when the file does not exist or cannot
     * be parsed, rather than throwing. The caller decides whether to show the
     * onboarding wizard or abort.</p>
     *
     * @return the persisted profile, or empty if absent or unreadable
     */
    public Optional<UserData> load() {
        if (!exists()) return Optional.empty();
        try {
            return Optional.of(MAPPER.readValue(profileFile.toFile(), UserData.class));
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to read profile.json — treating as missing", e);
            return Optional.empty();
        }
    }

    /**
     * Writes the profile to disk, creating parent directories if needed.
     *
     * <p>Propagates {@link IOException} — unlike {@link #load()}, a save failure
     * during onboarding is unrecoverable and the caller must handle it
     * explicitly.</p>
     *
     * @param profile the profile to persist; must not be {@code null}
     * @throws IOException if the file cannot be written
     */
    public void save(UserData profile) throws IOException {
        Files.createDirectories(profileFile.getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(profileFile.toFile(), profile);
        log.log(Level.INFO, "Profile saved to {0}", profileFile);
    }
}

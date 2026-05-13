package com.soteria.core.model;

/**
 * Immutable profile of the device owner, persisted to {@code ~/.soteria/profile.json}.
 *
 * <p>{@code ttsSpeechRate} and {@code wakeWordEnabled} are optional ({@code null} = not set).
 * Use {@link #effectiveTtsSpeechRate()} and {@link #effectiveWakeWordEnabled()} to get
 * resolved values with defaults applied.
 */
public record UserData(
    String fullName,
    String phoneNumber,
    String gender,
    String birthDate,
    String medicalInfo,
    String emergencyContact,
    String preferredModel,
    String preferredLanguage,
    Float ttsSpeechRate,
    Boolean wakeWordEnabled
) {
    /** Sentinel assigned to {@code fullName} when the onboarding wizard is incomplete. */
    public static final String INCOMPLETE_NAME = "[INCOMPLETE]";

    /**
     * Returns the TTS speech rate, falling back to {@code 1.44} if not set,
     * clamped to [0.5, 2.0].
     */
    public float effectiveTtsSpeechRate() {
        float v = ttsSpeechRate != null ? ttsSpeechRate : 1.44f;
        return Math.clamp(v, 0.5f, 2.0f);
    }

    /** Returns whether the wake-word listener is active, defaulting to {@code true} if not set. */
    public boolean effectiveWakeWordEnabled() {
        return wakeWordEnabled == null || wakeWordEnabled;
    }

    /**
     * Returns {@code true} if the profile has a real name (not {@link #INCOMPLETE_NAME})
     * and a non-blank emergency contact.
     */
    public boolean isComplete() {
        return fullName != null && !fullName.equals(INCOMPLETE_NAME)
                && emergencyContact != null && !emergencyContact.isBlank();
    }

    @Override
    public String toString() {
        return String.format(
            "Name: %s%nPhone: %s%nGender: %s%nBirthDate: %s%nEmergency Contact: %s%nMedical Info: %s%n" +
            "Language: %s%nModel: %s%nTTS rate: %s%nWake word: %s",
            fullName, phoneNumber, gender, birthDate, emergencyContact, medicalInfo,
            preferredLanguage, preferredModel,
            ttsSpeechRate != null ? ttsSpeechRate : "default",
            wakeWordEnabled != null ? wakeWordEnabled : "default"
        );
    }
}

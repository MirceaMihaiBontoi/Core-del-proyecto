package com.soteria.ui.i18n;

import com.soteria.ui.onboarding.OnboardingLanguageCatalog;

import java.util.Locale;

/**
 * Maps onboarding and settings language values to {@link java.util.Locale} for UI
 * {@link java.util.ResourceBundle} loading.
 *
 * <p>Input is normalized by {@link com.soteria.ui.onboarding.OnboardingLanguageCatalog#matchOrDefault(String)}.
 * Bundle file names and the full locale matrix are documented in {@code _i18n.spec.md}.
 */
public final class UiLocales {

    private UiLocales() {
    }

    /**
     * Resolves the user's preferred language string to a {@link Locale}.
     *
     * <p>Null, blank, and unrecognized values fall back to English via
     * {@link com.soteria.ui.onboarding.OnboardingLanguageCatalog#matchOrDefault(String)}.
     *
     * @param preferred persisted profile value, onboarding selection, or {@code null}
     * @return the locale for resource bundle lookup (never {@code null})
     */
    public static Locale fromPreferredLanguage(String preferred) {
        String canonical = OnboardingLanguageCatalog.matchOrDefault(preferred);
        if ("Spanish".equals(canonical)) {
            return Locale.forLanguageTag("es");
        }
        if ("French".equals(canonical)) {
            return Locale.FRENCH;
        }
        if ("German".equals(canonical)) {
            return Locale.GERMAN;
        }
        if ("Italian".equals(canonical)) {
            return Locale.ITALIAN;
        }
        if ("Portuguese".equals(canonical)) {
            return Locale.forLanguageTag("pt");
        }
        if ("Romanian".equals(canonical)) {
            return Locale.forLanguageTag("ro");
        }
        if ("Valencian".equals(canonical)) {
            return Locale.forLanguageTag("ca-ES-valencia");
        }
        if ("Chinese".equals(canonical)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if ("Russian".equals(canonical)) {
            return Locale.forLanguageTag("ru");
        }
        if ("Arabic".equals(canonical)) {
            return Locale.forLanguageTag("ar");
        }
        if ("Japanese".equals(canonical)) {
            return Locale.JAPANESE;
        }
        return Locale.ENGLISH;
    }
}

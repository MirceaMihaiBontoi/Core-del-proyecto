package com.soteria.core.port;

import java.util.Locale;

/**
 * Port for localization and internationalization.
 * Supports >50 languages for system messages and triage categories.
 */
public interface LocalizationService {

    String getMessage(String key);

    String getMessage(String key, Locale locale);

    String formatMessage(String key, Object... args);

    Locale getCurrentLocale();

    void setLocale(Locale locale);
}

package com.soteria.infrastructure.intelligence.system;

import com.soteria.core.port.LocalizationService;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralizes UI and text-to-speech string resolution to isolate the system
 * from MissingResourceExceptions that would crash the interface or halt audio output.
 */
public class ResourceLocalizationService implements LocalizationService {
    private static final Logger logger = Logger.getLogger(ResourceLocalizationService.class.getName());
    private static final String BUNDLE_BASE = "i18n.messages";
    
    private Locale currentLocale;
    private ResourceBundle bundle;

    public ResourceLocalizationService() {
        this(Locale.getDefault());
    }

    public ResourceLocalizationService(Locale locale) {
        setLocale(locale);
    }

    /**
     * Resolves a localization string with a strict safe-fallback contract.
     * If the key is absent in the target locale, it degrades gracefully and ultimately
     * returns the raw key string to prevent UI rendering or TTS synthesis failures.
     */
    @Override
    public String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception _) {
            logger.warning("Localization: Missing key '" + key + "' for locale " + currentLocale);
            return key; // Fallback to key itself
        }
    }

    @Override
    public String getMessage(String key, Locale locale) {
        try {
            ResourceBundle targetBundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            return targetBundle.getString(key);
        } catch (Exception _) {
            return getMessage(key); // Fallback to current
        }
    }

    @Override
    public String formatMessage(String key, Object... args) {
        String pattern = getMessage(key);
        return MessageFormat.format(pattern, args);
    }

    @Override
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    @Override
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        this.bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
        logger.log(Level.INFO, "Localization: Switched to locale {0}", locale);
    }
}

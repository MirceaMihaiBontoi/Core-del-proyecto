package com.soteria.infrastructure.intelligence.system;

import javax.sound.sampled.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for resilient audio acquisition.
 * Handles common Java Sound API issues across all supported platforms.
 */
public class AudioUtils {
    private static final Logger logger = Logger.getLogger(AudioUtils.class.getName());

    private AudioUtils() {}

    /**
     * Attempts to acquire an audio capture line by trying multiple fallback strategies.
     * <p>
     * Audio subsystem behavior varies significantly across OSes (PulseAudio/ALSA on Linux,
     * CoreAudio on Mac, WASAPI on Windows). This method mitigates "Line not supported"
     * errors by searching for compatible mixers when the default system line fails.
     *
     * @param format the specific audio format (sample rate, bit depth) required for the model
     * @return an already opened line ready for capture
     * @throws LineUnavailableException if all hardware fallback strategies are exhausted
     */
    public static TargetDataLine getResilientMic(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        logAvailableMixers(info);

        // Strategy 0: Retry loop for busy lines
        TargetDataLine defaultLine = tryOpenDefaultLine(info, format);
        if (defaultLine != null) return defaultLine;

        // Strategy 1: Targeted mixer search (any mixer that supports the format)
        TargetDataLine mixerLine = searchInMixers(info, format);
        if (mixerLine != null) return mixerLine;

        // Strategy 2: Fallback to well-known mixer names per platform
        TargetDataLine namedLine = searchInNamedMixers(info, format);
        if (namedLine != null) return namedLine;

        String platform = PlatformDetector.detectOS();
        String hint = "windows".equals(platform)
                ? "Please ensure a recording device is enabled in Sound settings."
                : "Please ensure PulseAudio/PipeWire is running and a capture device is available (pactl list sources).";

        throw new LineUnavailableException("No compatible microphone line found for " + format + ". " + hint);
    }

    private static void logAvailableMixers(DataLine.Info info) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        logger.log(Level.INFO, "Available audio mixers ({0} total):", mixers.length);
        for (Mixer.Info mi : mixers) {
            boolean supported = AudioSystem.getMixer(mi).isLineSupported(info);
            logger.log(Level.INFO, "  Mixer: {0} [{1}] - supported: {2}",
                    new Object[]{mi.getName(), mi.getDescription(), supported});
        }
    }

    private static TargetDataLine tryOpenDefaultLine(DataLine.Info info, AudioFormat format) throws LineUnavailableException {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (AudioSystem.isLineSupported(info)) {
                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    logger.log(Level.INFO, "Acquired microphone on attempt {0}", (i + 1));
                    return line;
                }
            } catch (LineUnavailableException e) {
                if (i == maxRetries - 1) throw e;
                logger.log(Level.WARNING, "Microphone busy, retrying in 100ms... (Attempt {0})", (i + 1));
                try { Thread.sleep(100); } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                logger.log(Level.FINE, "Default system microphone acquisition failed: {0}", e.getMessage());
            }
        }
        return null;
    }

    private static TargetDataLine searchInMixers(DataLine.Info info, AudioFormat format) {
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                    line.open(format);
                    logger.log(Level.INFO, "Using microphone from validated mixer: {0}", mixerInfo.getName());
                    return line;
                }
            } catch (Exception e) {
                logger.log(Level.FINE, "Could not open line from validated mixer {0}: {1}",
                    new Object[]{mixerInfo.getName(), e.getMessage()});
            }
        }
        return null;
    }

    private static TargetDataLine searchInNamedMixers(DataLine.Info info, AudioFormat format) {
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            String name = mixerInfo.getName().toLowerCase();
            // Cross-platform: Windows uses "capture"/"microphone"/"primary",
            // Linux uses "default"/"pulse"/"hw:"/"plughw:"/"dmix"
            if (name.contains("capture") || name.contains("microphone") || name.contains("primary")
                    || name.contains("default") || name.contains("pulse")
                    || name.startsWith("hw:") || name.startsWith("plughw:")) {
                try {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                    line.open(format);
                    logger.log(Level.INFO, "Using fallback named mixer: {0}", mixerInfo.getName());
                    return line;
                } catch (Exception _) {
                    // Ignore and keep searching
                }
            }
        }
        return null;
    }
}

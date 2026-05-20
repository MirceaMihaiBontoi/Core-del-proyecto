package com.soteria.infrastructure.intelligence.tts;

import com.soteria.infrastructure.intelligence.system.PlatformDetector;

import javax.sound.sampled.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the Java audio line and the {@code TTS-Playback} daemon thread.
 *
 * <p>A single persistent {@link SourceDataLine} is opened once and reused for
 * the lifetime of the service. Reopening the line on every utterance introduced
 * audible gaps and latency spikes on Windows.</p>
 *
 * <p>Applies a 30 ms crossfade between consecutive PCM chunks to eliminate
 * clicks at chunk boundaries caused by phase discontinuities.</p>
 */
public class TTSAudioPlayer implements AutoCloseable {

    private static final int SAMPLE_RATE = 24000;
    private static final int FADE_MS = 5;
    private static final int FADE_SAMPLES = (SAMPLE_RATE * FADE_MS) / 1000;
    private static final int PLAYBACK_CHUNK_BYTES = 8192;
    private static final int CROSSFADE_MS = 30;
    private static final int CROSSFADE_SAMPLES = (SAMPLE_RATE * CROSSFADE_MS) / 1000;

    private final TTSLogger ttsLogger;
    private final LinkedBlockingQueue<byte[]> playbackQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean interruptRequested = new AtomicBoolean(false);

    private SourceDataLine persistentLine;
    private Thread playbackThread;
    private volatile boolean running = false;
    private volatile float volume = 1.0f;
    private byte[] previousChunkTail = null;
    /** Actual {@link SourceDataLine} rate; Kokoro PCM is always {@link #SAMPLE_RATE}. */
    private int lineSampleRate = SAMPLE_RATE;

    public TTSAudioPlayer(TTSLogger ttsLogger) {
        this.ttsLogger = ttsLogger;
    }

    public void start() {
        running = true;
        playbackThread = new Thread(this::processPlaybackQueue, "TTS-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    public void enqueue(byte[] pcm) {
        try {
            playbackQueue.put(pcm);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ttsLogger.error("Failed to enqueue PCM for playback", e);
        }
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void stop() {
        interruptRequested.set(true);
        playbackQueue.clear();
        previousChunkTail = null;
        if (persistentLine != null && persistentLine.isOpen()) {
            persistentLine.stop();
            persistentLine.flush();
            persistentLine.start();
        }
        interruptRequested.set(false);
    }

    public void clearQueue() {
        playbackQueue.clear();
    }

    private void processPlaybackQueue() {
        if (!openPersistentLine()) return;

        while (running) {
            try {
                byte[] pcm = playbackQueue.poll(100, TimeUnit.MILLISECONDS);
                if (pcm != null && !interruptRequested.get()) {
                    processAudioChunk(pcm);
                }
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                ttsLogger.warn("TTS playback error: " + ex.getMessage());
            }
        }
        closePersistentLine();
    }

    private void processAudioChunk(byte[] pcm) {
        if (previousChunkTail != null && pcm.length > 0) {
            pcm = applyCrossfade(previousChunkTail, pcm);
        }
        playPcm(pcm);
        updatePreviousChunkTail(pcm);
    }

    private void updatePreviousChunkTail(byte[] pcm) {
        int tailBytes = Math.min(CROSSFADE_SAMPLES * 2, pcm.length);
        if (tailBytes > 0) {
            previousChunkTail = new byte[tailBytes];
            System.arraycopy(pcm, pcm.length - tailBytes, previousChunkTail, 0, tailBytes);
        }
    }

    private boolean openPersistentLine() {
        try {
            if ("linux".equals(PlatformDetector.detectOS())) {
                return openLinuxPersistentLine();
            }
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            persistentLine = (SourceDataLine) AudioSystem.getLine(info);
            persistentLine.open(format, (int) (format.getFrameSize() * format.getSampleRate() / 10));
            lineSampleRate = (int) persistentLine.getFormat().getSampleRate();
            persistentLine.start();
            ttsLogger.info("TTS audio line opened: " + lineSampleRate + "Hz 16-bit mono");
            return true;
        } catch (Exception e) {
            ttsLogger.error("Failed to open audio line", e);
            return false;
        }
    }

    /**
     * PulseAudio/PipeWire on Linux often rejects or mis-handles 24 kHz playback.
     * Open a standard device rate and resample Kokoro output before writing.
     */
    private boolean openLinuxPersistentLine() {
        for (int rate : new int[]{48000, 44100, SAMPLE_RATE}) {
            SourceDataLine line = tryOpenOutputLine(rate);
            if (line == null) continue;
            persistentLine = line;
            lineSampleRate = (int) line.getFormat().getSampleRate();
            line.start();
            String resampleNote = lineSampleRate != SAMPLE_RATE
                    ? ", resampling from " + SAMPLE_RATE
                    : "";
            ttsLogger.info("TTS audio line opened (Linux): " + lineSampleRate + "Hz 16-bit mono" + resampleNote);
            return true;
        }
        ttsLogger.error("Failed to open audio line", new LineUnavailableException("No Linux output line at 48/44.1/24 kHz"));
        return false;
    }

    private SourceDataLine tryOpenOutputLine(int sampleRate) {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        int bufferBytes = format.getFrameSize() * sampleRate / 5;

        try {
            if (AudioSystem.isLineSupported(info)) {
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, bufferBytes);
                return line;
            }
        } catch (Exception e) {
            ttsLogger.warn("Default output line failed at " + sampleRate + "Hz: " + e.getMessage());
        }

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            String name = mixerInfo.getName().toLowerCase();
            if (!name.contains("pulse") && !name.contains("default") && !name.contains("output")) {
                continue;
            }
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(info)) continue;
                SourceDataLine line = (SourceDataLine) mixer.getLine(info);
                line.open(format, bufferBytes);
                ttsLogger.info("TTS using mixer: " + mixerInfo.getName());
                return line;
            } catch (Exception ignored) {
                // try next mixer
            }
        }
        return null;
    }

    private void playPcm(byte[] audioData) {
        if ((persistentLine == null || !persistentLine.isOpen()) && !openPersistentLine()) return;

        if (lineSampleRate != SAMPLE_RATE) {
            audioData = resamplePcm16(audioData, SAMPLE_RATE, lineSampleRate);
        }

        int frameSize = persistentLine.getFormat().getFrameSize();
        for (int offset = 0; offset < audioData.length && !interruptRequested.get(); offset += PLAYBACK_CHUNK_BYTES) {
            int remaining = audioData.length - offset;
            int thisChunk = (Math.min(PLAYBACK_CHUNK_BYTES, remaining) / frameSize) * frameSize;
            if (thisChunk > 0) persistentLine.write(audioData, offset, thisChunk);
        }

        if (!interruptRequested.get()) {
            persistentLine.drain();
        } else {
            persistentLine.flush();
        }
    }

    private void closePersistentLine() {
        if (persistentLine != null && persistentLine.isOpen()) {
            persistentLine.stop();
            persistentLine.close();
            ttsLogger.info("TTS audio line closed");
        }
    }

    // --- PCM processing utilities ---

    public byte[] floatToPcm16(float[] samples) {
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            float s = Math.clamp(samples[i] * volume, -1.0f, 1.0f);
            short val = (short) (s * 32767.0f);
            pcm[i * 2] = (byte) (val & 0xFF);
            pcm[i * 2 + 1] = (byte) ((val >>> 8) & 0xFF);
        }
        return pcm;
    }

    public void applyFadeIn(byte[] pcm) {
        int samplesToFade = Math.min(FADE_SAMPLES, pcm.length / 2);
        for (int i = 0; i < samplesToFade && (i * 2 + 1) < pcm.length; i++) {
            short sample = readSample(pcm, i);
            float gain = (float) i / samplesToFade;
            writeSample(pcm, i, Math.round(sample * gain));
        }
    }

    public void applyFadeOut(byte[] pcm) {
        int totalSamples = pcm.length / 2;
        int samplesToFade = Math.min(FADE_SAMPLES, totalSamples);
        int startSample = totalSamples - samplesToFade;
        for (int i = startSample; i < totalSamples && (i * 2 + 1) < pcm.length; i++) {
            short sample = readSample(pcm, i);
            float gain = (float) (totalSamples - i) / samplesToFade;
            writeSample(pcm, i, Math.round(sample * gain));
        }
    }

    private static short readSample(byte[] pcm, int index) {
        return (short) (((pcm[2 * index + 1] & 0xFF) << 8) | (pcm[2 * index] & 0xFF));
    }

    private static void writeSample(byte[] pcm, int index, int value) {
        pcm[2 * index] = (byte) (value & 0xFF);
        pcm[2 * index + 1] = (byte) ((value >>> 8) & 0xFF);
    }

    private static byte[] resamplePcm16(byte[] pcm, int fromRate, int toRate) {
        if (fromRate == toRate || pcm.length < 2) return pcm;
        int inSamples = pcm.length / 2;
        int outSamples = Math.max(1, (int) ((long) inSamples * toRate / fromRate));
        byte[] out = new byte[outSamples * 2];
        for (int i = 0; i < outSamples; i++) {
            float srcPos = (float) i * fromRate / toRate;
            int idx = (int) srcPos;
            float frac = srcPos - idx;
            short s0 = readSample(pcm, Math.min(idx, inSamples - 1));
            short s1 = readSample(pcm, Math.min(idx + 1, inSamples - 1));
            writeSample(out, i, Math.round(s0 + frac * (s1 - s0)));
        }
        return out;
    }

    public byte[] generateSilence(int ms) {
        return new byte[(int) (SAMPLE_RATE * (ms / 1000f)) * 2];
    }

    public boolean isQueueEmpty() {
        return playbackQueue.isEmpty();
    }

    private byte[] applyCrossfade(byte[] previousTail, byte[] currentChunk) {
        int crossfadeBytes = Math.min(CROSSFADE_SAMPLES * 2, Math.min(previousTail.length, currentChunk.length));
        if (crossfadeBytes < 4) return currentChunk;

        int previousKeep = previousTail.length - crossfadeBytes;
        int currentKeep = currentChunk.length - crossfadeBytes;
        byte[] result = new byte[previousKeep + crossfadeBytes + currentKeep];

        if (previousKeep > 0) System.arraycopy(previousTail, 0, result, 0, previousKeep);

        for (int i = 0; i < crossfadeBytes; i += 2) {
            short prevSample = (short) (((previousTail[previousKeep + i + 1] & 0xFF) << 8) | (previousTail[previousKeep + i] & 0xFF));
            short currSample = (short) (((currentChunk[i + 1] & 0xFF) << 8) | (currentChunk[i] & 0xFF));
            float progress = (float) i / crossfadeBytes;
            int mixed = Math.clamp(Math.round(prevSample * (1f - progress) + currSample * progress), -32768, 32767);
            result[previousKeep + i] = (byte) (mixed & 0xFF);
            result[previousKeep + i + 1] = (byte) ((mixed >>> 8) & 0xFF);
        }

        if (currentKeep > 0) System.arraycopy(currentChunk, crossfadeBytes, result, previousKeep + crossfadeBytes, currentKeep);

        return result;
    }

    @Override
    public void close() {
        running = false;
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(2000);
            } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
        }
        closePersistentLine();
    }
}

package net.informaticalibera.japanesetts;

import com.codename1.system.NativeLookup;

/**
 * Public API for Japanese TTS.
 *
 * Non-Android platforms: isSupported() == false (no native implementation present).
 */
public final class JapaneseTTS {
    private static JapaneseTTSNative nativeImpl;

    /** Default OpenJTalk speaking rate (-r). */
    public static final float DEFAULT_SPEECH_RATE = 1.0f;
    /** Default OpenJTalk pitch shift in semitones (-fm). */
    public static final float DEFAULT_PITCH = 0.0f;

    private static volatile float speechRate = DEFAULT_SPEECH_RATE;
    private static volatile float pitch = DEFAULT_PITCH;

    private JapaneseTTS() {}

    private static JapaneseTTSNative n() {
        try {
            if (nativeImpl == null) {
                nativeImpl = NativeLookup.create(JapaneseTTSNative.class);
                // Apply currently configured parameters as soon as the native impl is available.
                if (nativeImpl != null) {
                    try { nativeImpl.setSpeechRate(speechRate); } catch (Throwable ignored) {}
                    try { nativeImpl.setPitch(pitch); } catch (Throwable ignored) {}
                }
            }
            return nativeImpl;
        } catch (Exception ex) {
           return null;
        }
    }

    /** Returns the currently configured speaking rate (speed multiplier). */
    public static float getSpeechRate() {
        return speechRate;
    }

    /** Returns the currently configured pitch shift (in semitones). */
    public static float getPitch() {
        return pitch;
    }

    /** Sets the speaking rate (speed multiplier). Default is {@link #DEFAULT_SPEECH_RATE}. */
    public static void setSpeechRate(float rate) {
        speechRate = rate;
        JapaneseTTSNative n = n();
        if (n != null) {
            try { n.setSpeechRate(rate); } catch (Throwable ignored) {}
        }
    }

    /** Sets the pitch shift (in semitones). Default is {@link #DEFAULT_PITCH}. */
    public static void setPitch(float pitchSemitones) {
        pitch = pitchSemitones;
        JapaneseTTSNative n = n();
        if (n != null) {
            try { n.setPitch(pitchSemitones); } catch (Throwable ignored) {}
        }
    }

    /** Resets speaking rate and pitch to default values. */
    public static void resetVoiceParams() {
        speechRate = DEFAULT_SPEECH_RATE;
        pitch = DEFAULT_PITCH;
        JapaneseTTSNative n = n();
        if (n != null) {
            try { n.resetVoiceParams(); } catch (Throwable ignored) {}
        }
    }

    public static boolean isSupported() {
        JapaneseTTSNative n = n();
        return n != null && n.isSupported();
    }

    /**
     * @return absolute path of the generated WAV file, or null if unsupported/failure.
     */
    public static String synthesizeToWav(String text) {
        JapaneseTTSNative n = n();
        if (n == null || !n.isSupported()) {
            return null;
        }
        String p = n.synthesizeToWav(text);
        if (p == null) {
            return null;
        }
        // CN1 APIs (e.g. MediaManager) work best with a URL (file:///...) rather than a raw absolute path.
        if (p.startsWith("file:")) {
            return p;
        }
        if (p.startsWith("/")) {
            // "file://" + "/data/..." -> "file:///data/..."
            return "file://" + p;
        }
        return p;
    }
}

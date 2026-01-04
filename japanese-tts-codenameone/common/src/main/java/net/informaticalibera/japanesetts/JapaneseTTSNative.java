package net.informaticalibera.japanesetts;

import com.codename1.system.NativeInterface;

/**
 * Native interface for Japanese TTS.
 * Implemented only on Android. Other platforms will not provide an implementation,
 * so NativeLookup.create(...) returns null and JapaneseTTS.isSupported() will be false.
 */
public interface JapaneseTTSNative extends NativeInterface {
    boolean isSupported();

    /**
     * Synthesizes the provided text to a WAV file and returns the absolute path of the WAV file.
     * Returns null on failure.
     */
    String synthesizeToWav(String text);

    /**
     * Sets the speaking rate (speed multiplier).
     * <p>
     * On Android/OpenJTalk this maps to the <code>-r</code> option.
     * A value of <code>1.0</code> is the default.
     */
    void setSpeechRate(float rate);

    /**
     * Sets the pitch (intonation) in semitones.
     * <p>
     * On Android/OpenJTalk this maps to the <code>-fm</code> option (additional half-tone).
     * A value of <code>0.0</code> is the default.
     */
    void setPitch(float pitch);

    /**
     * Resets voice parameters to their defaults (speech rate and pitch).
     */
    void resetVoiceParams();
}

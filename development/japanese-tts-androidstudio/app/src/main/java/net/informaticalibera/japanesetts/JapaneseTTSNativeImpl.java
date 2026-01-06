package net.informaticalibera.japanesetts;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.io.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Android implementation.
 *
 * Extracts dictionary/voice from resources into app home dir, then runs open_jtalk via ProcessBuilder.
 * IMPORTANT: open_jtalk must be executed from nativeLibraryDir (SELinux execute_no_trans on Android 10+).
 */
public class JapaneseTTSNativeImpl implements JapaneseTTSNative {

    private static final String TAG = "JapaneseTTS";

    private static final Object LOCK = new Object();
    private static volatile boolean prepared = false;

    private static final String RES_ASSETS_ROOT = "/japanesetts/assets/";
    private static final String VOICE_RES = RES_ASSETS_ROOT + "voice/mei/mei_normal.htsvoice";
    private static final String DICT_RES_DIR = RES_ASSETS_ROOT + "dict/open_jtalk_dic_utf_8-1.11";

    private String lastError;

    // OpenJTalk parameters (see open_jtalk -h)
    // -r : speaking rate (default 1.0)
    // -fm: pitch shift in semitones ("additional half-tone", default 0.0)
    private volatile float speechRate = JapaneseTTS.DEFAULT_SPEECH_RATE;
    private volatile float pitch = JapaneseTTS.DEFAULT_PITCH;

    @Override
    public boolean isSupported() {
        return selectAbi() != null;
    }

    @Override
    public void setSpeechRate(float rate) {
        // Keep this permissive; clamp to a sane range so a bad UI value won't break synthesis.
        if (Float.isNaN(rate) || Float.isInfinite(rate)) return;
        if (rate < 0.2f) rate = 0.2f;
        if (rate > 5.0f) rate = 5.0f;
        this.speechRate = rate;
    }

    @Override
    public void setPitch(float pitchSemitones) {
        if (Float.isNaN(pitchSemitones) || Float.isInfinite(pitchSemitones)) return;
        if (pitchSemitones < -24.0f) pitchSemitones = -24.0f;
        if (pitchSemitones > 24.0f) pitchSemitones = 24.0f;
        this.pitch = pitchSemitones;
    }

    @Override
    public void resetVoiceParams() {
        this.speechRate = JapaneseTTS.DEFAULT_SPEECH_RATE;
        this.pitch = JapaneseTTS.DEFAULT_PITCH;
    }

    @Override
    public String synthesizeToWav(String text) {
        if (text == null) {
            lastError = "text is null";
            return null;
        }
        if (!isSupported()) {
            lastError = "unsupported ABI";
            return null;
        }

        try {
            ensurePrepared();

            File workDir = getWorkDir();
            File dictDir = new File(workDir, "assets/dict/open_jtalk_dic_utf_8-1.11");
            File voiceFile = new File(workDir, "assets/voice/mei/mei_normal.htsvoice");
            File binFile = getOpenJTalkBinary();

            if (!dictDir.isDirectory() || !voiceFile.isFile()) {
                lastError = "missing extracted assets (dict or voice)";
                return null;
            }

            File inTxt = new File(workDir, "input_" + System.currentTimeMillis() + ".txt");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(inTxt), StandardCharsets.UTF_8)) {
                w.write(text);
                w.write('\n');
            }

            File outWav = new File(workDir, "output_" + System.currentTimeMillis() + ".wav");

            ProcessBuilder pb = new ProcessBuilder(
                    binFile.getAbsolutePath(),
                    "-x", dictDir.getAbsolutePath(),
                    "-m", voiceFile.getAbsolutePath(),
                    "-r", Float.toString(speechRate),
                    "-fm", Float.toString(pitch),
                    "-ow", outWav.getAbsolutePath(),
                    inTxt.getAbsolutePath()
            );
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            // Ensure libc++_shared.so and any other deps are found beside the binary.
            pb.environment().put("LD_LIBRARY_PATH", binFile.getParentFile().getAbsolutePath());

            Process p = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }

            int rc = p.waitFor();
            //noinspection ResultOfMethodCallIgnored
            inTxt.delete();

            if (rc != 0) {
                lastError = "open_jtalk rc=" + rc + "\n" + out;
                return null;
            }
            if (!outWav.isFile() || outWav.length() == 0) {
                lastError = "open_jtalk produced empty wav";
                return null;
            }

            lastError = null;
            return outWav.getAbsolutePath();

        } catch (Throwable t) {
            lastError = (t.getMessage() != null)
                    ? (t.getClass().getName() + ": " + t.getMessage())
                    : t.getClass().getName();

            Log.e(TAG, "synthesizeToWav() FAILED", t);

            try {
                Log.e(TAG, "SDK_INT=" + Build.VERSION.SDK_INT
                        + " MODEL=" + Build.MODEL
                        + " DEVICE=" + Build.DEVICE
                        + " CPU_ABI=" + Build.CPU_ABI
                        + " CPU_ABI2=" + Build.CPU_ABI2
                        + " SUPPORTED_ABIS=" + Arrays.toString(Build.SUPPORTED_ABIS));
            } catch (Throwable ignored) {
            }

            return null;
        }
    }

    private void ensurePrepared() throws IOException {
        if (prepared) return;
        synchronized (LOCK) {
            if (prepared) return;

            File workDir = getWorkDir();

            // Voice
            File voiceOut = new File(workDir, "assets/voice/mei/mei_normal.htsvoice");
            extractIfMissing(VOICE_RES, voiceOut);

            // Dictionary
            String[] dictFiles = new String[]{
                    "char.bin",
                    "COPYING",
                    "left-id.def",
                    "matrix.bin",
                    "pos-id.def",
                    "rewrite.def",
                    "right-id.def",
                    "sys.dic",
                    "unk.dic"
            };

            File dictOutDir = new File(workDir, "assets/dict/open_jtalk_dic_utf_8-1.11");
            mkdirs(dictOutDir);
            for (String f : dictFiles) {
                String res = DICT_RES_DIR + "/" + f;
                File dst = new File(dictOutDir, f);
                extractIfMissing(res, dst);
            }

            prepared = true;
        }
    }

    /**
     * Returns an internal, app-private directory.
     *
     * IMPORTANT: We intentionally do NOT use Codename One's FileSystemStorage here because
     * on Android it can trigger the runtime "Storage" permission prompt (READ/WRITE_EXTERNAL_STORAGE)
     * even though we only need internal storage.
     */
    private static File getWorkDir() {
        Context ctx = AndroidNativeUtil.getContext();
        if (ctx == null) {
            throw new RuntimeException("Android context is null");
        }

        File base = ctx.getFilesDir();
        File dir = new File(base, "japanesetts");

        Log.e(TAG, "filesDir=" + base.getAbsolutePath() + " workDir=" + dir.getAbsolutePath());

        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new RuntimeException("Cannot create work dir: " + dir.getAbsolutePath());
        }
        return dir;
    }

    private static void mkdirs(File d) throws IOException {
        if (d.isDirectory()) return;
        if (!d.mkdirs() && !d.isDirectory()) {
            throw new IOException("Cannot create dir: " + d);
        }
    }

    private InputStream openBundledStream(String path) throws IOException {
        // 1) Try classpath resources (works in some packagings)
        InputStream in = JapaneseTTSNativeImpl.class.getResourceAsStream(path);
        if (in != null) {
            Log.d(TAG, "Opening classpath resource: " + path);
            return in;
        }

        // 2) Try Android assets (CN1 Android builds typically store resources as assets)
        Context ctx = AndroidNativeUtil.getContext();
        if (ctx != null) {
            String p = path.startsWith("/") ? path.substring(1) : path;
            try {
                Log.d(TAG, "Opening Android asset: " + p);
                return ctx.getAssets().open(p);
            } catch (FileNotFoundException fnf) {
                // fall through
            }
        }
        return null;
    }

    private void extractIfMissing(String resourcePath, File dst) throws IOException {
        if (dst.isFile() && dst.length() > 0) {
            return;
        }
        mkdirs(dst.getParentFile());

        try (InputStream in = openBundledStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource/asset not found: " + resourcePath);
            }
            try (OutputStream out = new FileOutputStream(dst)) {
                Util.copy(in, out);
            }
        }
    }


    private static String selectAbi() {
        String[] abis;
        try {
            abis = Build.SUPPORTED_ABIS;
        } catch (Throwable t) {
            abis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
        if (abis == null) return null;

        for (String a : abis) {
            if (a == null) continue;
            a = a.toLowerCase(Locale.ROOT).trim();
            if (a.equals("arm64-v8a")) return "arm64-v8a";
            if (a.equals("armeabi-v7a")) return "armeabi-v7a";
            if (a.equals("x86_64")) return "x86_64";
        }
        return null;
    }

    private static File getOpenJTalkBinary() throws IOException {
        Context ctx = AndroidNativeUtil.getContext();
        if (ctx == null) throw new IOException("Android context is null");

        String libDir = ctx.getApplicationInfo().nativeLibraryDir;
        File bin = new File(libDir, "libopen_jtalk.so");

        Log.e(TAG, "nativeLibraryDir=" + libDir + " open_jtalk=" + bin);

        if (!bin.isFile()) {
            throw new FileNotFoundException("open_jtalk not found in nativeLibraryDir: " + bin);
        }
        return bin;
    }
}

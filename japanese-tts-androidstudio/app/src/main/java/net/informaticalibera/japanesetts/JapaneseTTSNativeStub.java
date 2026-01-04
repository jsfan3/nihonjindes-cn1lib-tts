package net.informaticalibera.japanesetts;

import com.codename1.ui.PeerComponent;

public class JapaneseTTSNativeStub implements JapaneseTTSNative{
    private JapaneseTTSNativeImpl impl = new JapaneseTTSNativeImpl();

    public boolean isSupported() {
        return impl.isSupported();
    }

    public void setSpeechRate(float param0) {
        impl.setSpeechRate(param0);
    }

    public void setPitch(float param0) {
        impl.setPitch(param0);
    }

    public void resetVoiceParams() {
        impl.resetVoiceParams();
    }

    public String synthesizeToWav(String param0) {
        return impl.synthesizeToWav(param0);
    }

}

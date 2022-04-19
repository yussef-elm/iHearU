package com.ensias.ihearu.output.speech;

import org.dicio.skill.output.SpeechOutputDevice;


public abstract class InstantSpeechDevice implements SpeechOutputDevice {
    @Override
    public final boolean isSpeaking() {
        return false;
    }

    @Override
    public final void runWhenFinishedSpeaking(final Runnable runnable) {
        runnable.run();
    }
}

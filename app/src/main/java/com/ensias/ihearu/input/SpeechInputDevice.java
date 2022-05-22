package com.ensias.ihearu.input;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import com.ensias.ihearu.R;

public abstract class SpeechInputDevice extends InputDevice {

    public static final class UnableToAccessMicrophoneException extends Exception {
        UnableToAccessMicrophoneException() {
            super("Unable to access microphone."
                    + " Microphone might be already in use or the permission was not granted.");
        }
    }


    private enum ShownState {
        REQUIRES_DOWNLOAD, LOADING, INACTIVE, LISTENING
    }

    @Nullable private ExtendedFloatingActionButton voiceFab = null;
    @Nullable private ProgressBar voiceLoading = null;

    private ShownState currentShownState = ShownState.INACTIVE; // start with inactive state


    // use to show loading, inactive and listening states.
    public final void setVoiceViews(@Nullable final ExtendedFloatingActionButton voiceFabToSet,
                                    @Nullable final ProgressBar voiceLoadingToSet) {
        if (this.voiceFab != null) {
            // release previous on click listener to allow garbage collection to kick in
            this.voiceFab.setOnClickListener(null);
        }

        this.voiceFab = voiceFabToSet;
        this.voiceLoading = voiceLoadingToSet;

        if (voiceFabToSet != null) {
            voiceFabToSet.setText(voiceFabToSet.getContext().getString(R.string.listening));
            showState(currentShownState);
            voiceFabToSet.setOnClickListener(view -> {
                if (currentShownState == ShownState.LISTENING) {
                    // already listening, so stop listening
                    cancelGettingInput();
                } else {
                    tryToGetInput(true);
                }
            });
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        setVoiceViews(null, null);
        currentShownState = ShownState.INACTIVE;
    }



    @Override
    public abstract void load();

    // Listens for some spoken input from the microphone.
    @Override
    public void tryToGetInput(final boolean manual) {
        super.tryToGetInput(manual); // overridden just to provide a more detailed documentation ^
    }

    @Override
    public abstract void cancelGettingInput();


    protected final void onRequiresDownload() {
        showState(ShownState.REQUIRES_DOWNLOAD);
    }


    protected final void onLoading() {
        showState(ShownState.LOADING);
    }


    protected final void onInactive() {
        showState(ShownState.INACTIVE);
    }


    protected final void onListening() {
        showState(ShownState.LISTENING);
    }


    private void showState(final ShownState state) {
        currentShownState = state;
        if (voiceFab != null && voiceLoading != null) {
            switch (state) {
                case REQUIRES_DOWNLOAD:
                    voiceFab.setIcon(AppCompatResources.getDrawable(voiceFab.getContext(),
                            R.drawable.ic_download_white));
                    voiceFab.shrink();
                    voiceLoading.setVisibility(View.GONE);
                    break;
                case LOADING:
                    voiceFab.setIcon(new ColorDrawable(Color.TRANSPARENT));
                    voiceFab.shrink();
                    voiceLoading.setVisibility(View.VISIBLE);
                    break;
                case INACTIVE: default:
                    voiceFab.setIcon(AppCompatResources.getDrawable(voiceFab.getContext(),
                            R.drawable.ic_mic_none_white));
                    voiceFab.shrink();
                    voiceLoading.setVisibility(View.GONE);
                    break;
                case LISTENING:
                    voiceFab.setIcon(AppCompatResources.getDrawable(voiceFab.getContext(),
                            R.drawable.ic_mic_white));
                    voiceFab.extend();
                    voiceLoading.setVisibility(View.GONE);
                    break;
            }
        }
    }
}

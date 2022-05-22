package com.ensias.ihearu.input;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.ensias.ihearu.BuildConfig;
import org.dicio.skill.util.CleanableUp;

import java.util.Collections;
import java.util.List;

public abstract class InputDevice implements CleanableUp {


    public interface InputDeviceListener {

        // notify that the device is trying to get some input

        void onTryingToGetInput();

        //Called when the user provided some partial input (while talking)

        void onPartialInputReceived(String input);

        //Called when some input was received from the user.
        void onInputReceived(List<String> input);


        // Called when no input was received from the user after he seemed to want to provide some

        void onNoInputReceived();

        // Called when an error occurs while trying to get input or processing it

        void onError(Throwable e);
    }

    private static final String TAG = InputDevice.class.getSimpleName();

    @Nullable
    private InputDeviceListener inputDeviceListener = null;


    // Prepares the input device. If doing heavy work, run it in an asynchronous thread.

    public abstract void load();


    @CallSuper
    public void tryToGetInput(final boolean manual) {
        if (inputDeviceListener != null) {
            inputDeviceListener.onTryingToGetInput();
        }
    }


    public abstract void cancelGettingInput();


    public final void setInputDeviceListener(@Nullable final InputDeviceListener listener) {
        this.inputDeviceListener = listener;
    }

    @Override
    public void cleanup() {
        setInputDeviceListener(null);
    }


}

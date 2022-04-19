package com.ensias.ihearu.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.ensias.ihearu.R;

public class HeaderFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.pref_header);
    }
}

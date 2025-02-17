package com.tahtalı.hatirlatik.presentation.settings;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import com.tahtalı.hatirlatik.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
} 
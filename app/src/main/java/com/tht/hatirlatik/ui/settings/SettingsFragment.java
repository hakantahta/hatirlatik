package com.tht.hatirlatik.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.preferences.PreferencesManager;

/**
 * Ayarlar ekranını yöneten fragment sınıfı.
 * Kullanıcı tercihlerini yönetir ve değişiklikleri PreferencesManager aracılığıyla kaydeder.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private PreferencesManager preferencesManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        preferencesManager = new PreferencesManager(requireContext());

        setupDarkModePreference();
        setupNotificationPreferences();
        setupAdsPreference();
        setupAboutPreferences();
    }

    private void setupDarkModePreference() {
        SwitchPreferenceCompat darkModePreference = findPreference("dark_mode");
        if (darkModePreference != null) {
            darkModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean darkModeEnabled = (Boolean) newValue;
                AppCompatDelegate.setDefaultNightMode(
                    darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
                return true;
            });
        }
    }

    private void setupNotificationPreferences() {
        ListPreference defaultReminderPreference = findPreference("default_reminder_minutes");
        if (defaultReminderPreference != null) {
            defaultReminderPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                preferencesManager.setDefaultReminderMinutes(Integer.parseInt((String) newValue));
                return true;
            });
        }

        ListPreference notificationTypePreference = findPreference("default_notification_type");
        if (notificationTypePreference != null) {
            notificationTypePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                preferencesManager.setDefaultNotificationType((String) newValue);
                return true;
            });
        }

        SwitchPreferenceCompat soundPreference = findPreference("notification_sound");
        if (soundPreference != null) {
            soundPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                preferencesManager.setNotificationSoundEnabled((Boolean) newValue);
                return true;
            });
        }

        SwitchPreferenceCompat vibrationPreference = findPreference("notification_vibration");
        if (vibrationPreference != null) {
            vibrationPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                preferencesManager.setNotificationVibrationEnabled((Boolean) newValue);
                return true;
            });
        }
    }

    private void setupAdsPreference() {
        SwitchPreferenceCompat showAdsPreference = findPreference("show_ads");
        if (showAdsPreference != null) {
            showAdsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                preferencesManager.setAdsEnabled((Boolean) newValue);
                return true;
            });
        }
    }

    private void setupAboutPreferences() {
        Preference versionPreference = findPreference("app_version");
        if (versionPreference != null) {
            String versionName = getString(R.string.app_version);
            versionPreference.setSummary(versionName);
        }

        Preference privacyPolicyPreference = findPreference("privacy_policy");
        if (privacyPolicyPreference != null) {
            privacyPolicyPreference.setOnPreferenceClickListener(preference -> {
                openPrivacyPolicy();
                return true;
            });
        }
    }

    private void openPrivacyPolicy() {
        // TODO: Gizlilik politikası URL'sini ekleyin
        String privacyPolicyUrl = "https://example.com/privacy-policy";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
        startActivity(intent);
    }
} 
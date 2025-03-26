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
import com.tht.hatirlatik.utils.AdHelper;

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
        setupAdsPreference();
        setupAboutPreferences();
    }

    private void setupDarkModePreference() {
        SwitchPreferenceCompat darkModePreference = findPreference("dark_mode");
        if (darkModePreference != null) {
            // Sistem temasına göre başlangıç durumunu ayarla
            int currentNightMode = getResources().getConfiguration().uiMode 
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            darkModePreference.setChecked(
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            );
            
            darkModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean darkModeEnabled = (Boolean) newValue;
                AppCompatDelegate.setDefaultNightMode(
                    darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
                return true;
            });
        }
    }
    
    private void setupAdsPreference() {
        SwitchPreferenceCompat showAdsPreference = findPreference("show_ads");
        if (showAdsPreference != null) {
            // Mevcut ayar durumunu al
            showAdsPreference.setChecked(preferencesManager.isAdsEnabled());
            
            showAdsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean showAds = (Boolean) newValue;
                preferencesManager.setAdsEnabled(showAds);
                
                // Eğer reklamlar yeniden etkinleştirilirse AdMob'u yeniden başlat
                if (showAds) {
                    AdHelper.getInstance().initialize(requireContext());
                }
                
                // Değişikliklerin etkili olması için aktiviteyi yeniden başlatmak gerekebilir
                requireActivity().recreate();
                
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
        // Gizlilik politikası URL'si
        String privacyPolicyUrl = "https://hatirlatik.com/privacy-policy";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
        startActivity(intent);
    }
} 
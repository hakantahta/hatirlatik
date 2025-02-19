package com.tht.hatirlatik.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class PreferencesManager {
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_DEFAULT_REMINDER_MINUTES = "default_reminder_minutes";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_NOTIFICATION_VIBRATION = "notification_vibration";
    private static final String KEY_DEFAULT_NOTIFICATION_TYPE = "default_notification_type";
    private static final String KEY_SHOW_ADS = "show_ads";

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // Tema ayarları
    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    // Varsayılan hatırlatma süresi (dakika)
    public int getDefaultReminderMinutes() {
        return Integer.parseInt(preferences.getString(KEY_DEFAULT_REMINDER_MINUTES, "30"));
    }

    public void setDefaultReminderMinutes(int minutes) {
        preferences.edit().putString(KEY_DEFAULT_REMINDER_MINUTES, String.valueOf(minutes)).apply();
    }

    // Bildirim sesi
    public boolean isNotificationSoundEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATION_SOUND, true);
    }

    public void setNotificationSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply();
    }

    // Bildirim titreşimi
    public boolean isNotificationVibrationEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATION_VIBRATION, true);
    }

    public void setNotificationVibrationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATION_VIBRATION, enabled).apply();
    }

    // Varsayılan bildirim türü
    public String getDefaultNotificationType() {
        return preferences.getString(KEY_DEFAULT_NOTIFICATION_TYPE, "NOTIFICATION");
    }

    public void setDefaultNotificationType(String type) {
        preferences.edit().putString(KEY_DEFAULT_NOTIFICATION_TYPE, type).apply();
    }

    // Reklam gösterimi
    public boolean isAdsEnabled() {
        return preferences.getBoolean(KEY_SHOW_ADS, true);
    }

    public void setAdsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SHOW_ADS, enabled).apply();
    }
} 
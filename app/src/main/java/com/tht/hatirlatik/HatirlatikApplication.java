package com.tht.hatirlatik;

import android.app.Application;

import androidx.work.Configuration;

import com.tht.hatirlatik.utils.AdHelper;

public class HatirlatikApplication extends Application implements Configuration.Provider {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // AdMob'u başlat
        AdHelper.getInstance().initialize(this);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
} 
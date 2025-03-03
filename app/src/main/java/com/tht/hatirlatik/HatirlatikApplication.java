package com.tht.hatirlatik;

import android.app.Application;
import android.content.Intent;

import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.utils.AdHelper;
import com.tht.hatirlatik.widget.TaskWidgetProvider;

import java.util.concurrent.TimeUnit;

public class HatirlatikApplication extends Application implements Configuration.Provider {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // AdMob'u başlat
        AdHelper.getInstance().initialize(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Veritabanı instance'ını temizle
        AppDatabase.destroyInstance();
        // WorkManager'ı temizle
        WorkManager.getInstance(this).cancelAllWork();
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
    
    /**
     * Widget'ları günceller
     */
    public void updateWidgets() {
        Intent intent = new Intent(this, TaskWidgetProvider.class);
        intent.setAction(TaskWidgetProvider.ACTION_DATA_UPDATED);
        sendBroadcast(intent);
    }
} 
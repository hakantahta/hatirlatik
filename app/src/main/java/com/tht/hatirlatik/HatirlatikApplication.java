package com.tht.hatirlatik;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.tht.hatirlatik.utils.AdHelper;
import com.tht.hatirlatik.workers.WidgetUpdateWorker;

import java.util.concurrent.TimeUnit;

public class HatirlatikApplication extends Application implements Configuration.Provider {
    private static final String WIDGET_UPDATE_WORK = "widget_update_work";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // AdMob'u başlat
        AdHelper.getInstance().initialize(this);
        scheduleWidgetUpdate();
    }

    private void scheduleWidgetUpdate() {
        // Widget güncelleme işi için kısıtlamaları ayarla
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        // 5 dakikada bir çalışacak periyodik iş oluştur
        PeriodicWorkRequest widgetUpdateRequest = new PeriodicWorkRequest.Builder(
                WidgetUpdateWorker.class,
                5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        // İşi planla (varsa eskisini değiştir)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                widgetUpdateRequest);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
} 
package com.tht.hatirlatik.widget;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

/**
 * Widget için RemoteViewsService sınıfı.
 * Widget'ın liste görünümü için veri sağlayıcı factory'yi döndürür.
 */
public class TaskWidgetService extends RemoteViewsService {
    private static final String TAG = "TaskWidgetService";
    
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(TAG, "onGetViewFactory çağrıldı");
        
        try {
            int widgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);
            
            if (widgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.e(TAG, "Geçersiz widget ID");
                return new TaskWidgetRemoteViewsFactory(getApplicationContext());
            }
            
            Log.d(TAG, "Widget ID: " + widgetId + " için factory oluşturuluyor");
            
            // Factory oluştur
            TaskWidgetRemoteViewsFactory factory = new TaskWidgetRemoteViewsFactory(getApplicationContext());
            Log.d(TAG, "Factory başarıyla oluşturuldu");
            
            return factory;
        } catch (Exception e) {
            Log.e(TAG, "Factory oluşturulurken hata: " + e.getMessage(), e);
            return new TaskWidgetRemoteViewsFactory(getApplicationContext());
        }
    }
} 
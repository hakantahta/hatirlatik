package com.tht.hatirlatik.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.content.ComponentName;
import android.util.Log;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.MainActivity;

public class TaskListWidget extends AppWidgetProvider {
    private static final String TAG = "TaskListWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Her widget güncellemesi için
        for (int appWidgetId : appWidgetIds) {
            try {
                // Ana aktiviteyi açmak için intent oluştur
                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                // ListView için RemoteViews servisini ayarla
                Intent serviceIntent = new Intent(context, TaskListWidgetService.class);
                serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

                // RemoteViews oluştur ve yapılandır
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_list_widget);
                views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);
                views.setEmptyView(R.id.widget_list_view, R.id.empty_view);

                // Header'a tıklama olayını ekle
                views.setOnClickPendingIntent(R.id.widget_header, pendingIntent);

                // Liste öğelerine tıklama için template oluştur
                Intent itemIntent = new Intent(context, MainActivity.class);
                PendingIntent itemPendingIntent = PendingIntent.getActivity(context, 0, itemIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setPendingIntentTemplate(R.id.widget_list_view, itemPendingIntent);

                // Widget'ı güncelle
                appWidgetManager.updateAppWidget(appWidgetId, views);
                
                Log.d(TAG, "Widget başarıyla güncellendi: " + appWidgetId);
            } catch (Exception e) {
                Log.e(TAG, "Widget güncellenirken hata: " + e.getMessage());
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        // Widget'ı güncelle
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, TaskListWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
            
            // Önce widget görünümünü güncelle
            onUpdate(context, appWidgetManager, appWidgetIds);
            
            // Sonra widget verilerini güncelle
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
        }
    }
} 
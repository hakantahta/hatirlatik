package com.tht.hatirlatik.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.MainActivity;

public class TaskListWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // RemoteViews nesnesini oluştur
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_list_widget);

        // Widget'a tıklandığında ana uygulamayı açacak intent
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

        // ListView için RemoteViewsService'i ayarla
        Intent serviceIntent = new Intent(context, TaskListWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);

        // Boş durum için görünümü ayarla
        views.setEmptyView(R.id.widget_list_view, R.id.empty_view);

        // Widget'ı güncelle
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
} 
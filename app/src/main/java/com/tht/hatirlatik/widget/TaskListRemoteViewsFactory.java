package com.tht.hatirlatik.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.util.Log;

import com.tht.hatirlatik.R;
import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.model.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "TaskListRemoteViewsFactory";
    private final Context context;
    private List<Task> tasks;
    private AppDatabase database;
    private final SimpleDateFormat dateFormat;

    public TaskListRemoteViewsFactory(Context context) {
        this.context = context;
        this.tasks = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("tr"));
    }

    @Override
    public void onCreate() {
        try {
            database = AppDatabase.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG, "Veritabanı başlatılırken hata: " + e.getMessage());
        }
    }

    @Override
    public void onDataSetChanged() {
        try {
            if (database != null) {
                // Aktif görevleri al
                tasks = database.taskDao().getActiveTasksForWidget();
                Log.d(TAG, "Widget için " + tasks.size() + " aktif görev yüklendi");
            }
        } catch (Exception e) {
            Log.e(TAG, "Veriler güncellenirken hata: " + e.getMessage());
            tasks = new ArrayList<>();
        }
    }

    @Override
    public void onDestroy() {
        tasks.clear();
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        try {
            if (position == -1 || tasks == null || position >= tasks.size()) {
                return null;
            }

            Task task = tasks.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);

            // Görev başlığını ayarla
            views.setTextViewText(R.id.task_title, task.getTitle());

            // Görev tarihini ayarla
            String formattedDate = dateFormat.format(task.getDateTime());
            views.setTextViewText(R.id.task_date, formattedDate);

            // Durum göstergesini ayarla
            views.setInt(R.id.task_status_indicator, "setBackgroundResource", 
                    task.isCompleted() ? R.color.task_status_completed : R.color.accent);

            // Tıklama için fillInIntent ayarla
            Intent fillInIntent = new Intent();
            fillInIntent.putExtra("task_id", task.getId());
            views.setOnClickFillInIntent(R.id.widget_task_item, fillInIntent);

            return views;
        } catch (Exception e) {
            Log.e(TAG, "Görünüm oluşturulurken hata: " + e.getMessage());
            return null;
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
} 
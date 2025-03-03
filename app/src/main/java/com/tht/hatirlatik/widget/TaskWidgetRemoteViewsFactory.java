package com.tht.hatirlatik.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.tht.hatirlatik.MainActivity;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.database.TaskDao;
import com.tht.hatirlatik.model.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Widget için RemoteViewsFactory sınıfı.
 * Widget'ın liste görünümü için veri sağlar.
 */
public class TaskWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private List<Task> tasks = new ArrayList<>();
    private TaskDao taskDao;
    private static final String TAG = "TaskWidgetFactory";

    public TaskWidgetRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        try {
            Log.d(TAG, "onCreate çağrıldı");
            // Veritabanı bağlantısını kur
            AppDatabase database = AppDatabase.getInstance(context);
            if (database == null) {
                Log.e(TAG, "Veritabanı örneği alınamadı");
                return;
            }
            taskDao = database.taskDao();
            Log.d(TAG, "Veritabanı bağlantısı kuruldu");
            
            // Boş görev listesi oluştur
            tasks = new ArrayList<>();
            Log.d(TAG, "Boş görev listesi oluşturuldu");
        } catch (Exception e) {
            Log.e(TAG, "Veritabanı bağlantısı kurulurken hata: " + e.getMessage(), e);
            tasks = new ArrayList<>();
        }
    }

    @Override
    public void onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged çağrıldı");
        
        // Veritabanı işlemlerini ana thread'de yapmak StrictMode ihlali oluşturabilir
        // Bu nedenle senkron bir şekilde çalışacak bir çözüm kullanıyoruz
        
        // Önce boş bir liste oluştur
        tasks = new ArrayList<>();
        
        try {
            // Veritabanı bağlantısını yenile
            if (taskDao == null) {
                AppDatabase database = AppDatabase.getInstance(context);
                if (database == null) {
                    Log.e(TAG, "Veritabanı örneği alınamadı");
                    return;
                }
                taskDao = database.taskDao();
                Log.d(TAG, "TaskDao yeniden oluşturuldu");
            }
            
            // Bugünün görevlerini getir
            Calendar startOfDay = Calendar.getInstance();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);

            Calendar endOfDay = Calendar.getInstance();
            endOfDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDay.set(Calendar.MINUTE, 59);
            endOfDay.set(Calendar.SECOND, 59);
            endOfDay.set(Calendar.MILLISECOND, 999);

            Log.d(TAG, "Tarih aralığı: " + startOfDay.getTime() + " - " + endOfDay.getTime());
            
            // Bugünün görevlerini al - doğrudan çağırıyoruz
            final List<Task> todayTasks = taskDao.getTasksBetweenDatesSync(startOfDay.getTime(), endOfDay.getTime());
            Log.d(TAG, "Bugün için " + todayTasks.size() + " görev bulundu");
            
            // Görevleri güncelle
            tasks.clear();
            tasks.addAll(todayTasks);
            
            // Görevleri tarihe göre sırala
            java.util.Collections.sort(tasks, new java.util.Comparator<Task>() {
                @Override
                public int compare(Task task1, Task task2) {
                    return task1.getDateTime().compareTo(task2.getDateTime());
                }
            });
            
            // Görevleri logla
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                Log.d(TAG, "Görev " + i + ": " + task.getTitle() + " (ID: " + task.getId() + ")");
            }
            
            Log.d(TAG, "Görevler başarıyla yüklendi ve sıralandı");
        } catch (Exception e) {
            Log.e(TAG, "Görevler yüklenirken hata oluştu: " + e.getMessage(), e);
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
        Log.d(TAG, "getViewAt: position=" + position);
        
        // Güvenlik kontrolü
        if (position < 0 || position >= getCount() || tasks == null || tasks.isEmpty()) {
            Log.e(TAG, "getViewAt: Geçersiz pozisyon veya boş liste. Pozisyon: " + position + ", Liste boyutu: " + (tasks != null ? tasks.size() : 0));
            return getLoadingView(); // Null yerine yükleme görünümünü döndür
        }
        
        try {
            Task task = tasks.get(position);
            Log.d(TAG, "getViewAt: Görev yükleniyor: " + task.getTitle());
            
            // Widget öğesi için RemoteViews oluştur
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);
            
            // Görev başlığını ayarla
            views.setTextViewText(R.id.widget_task_title, task.getTitle());
            
            // Görev saatini ayarla
            if (task.getDateTime() != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String timeText = timeFormat.format(task.getDateTime());
                views.setTextViewText(R.id.widget_task_time, timeText);
            } else {
                views.setTextViewText(R.id.widget_task_time, "");
            }
            
            // Görev tamamlanma durumunu ayarla - CheckBox yerine ImageView kullanıyoruz
            if (task.isCompleted()) {
                views.setImageViewResource(R.id.widget_task_checkbox, R.drawable.ic_checkbox_checked);
            } else {
                views.setImageViewResource(R.id.widget_task_checkbox, R.drawable.ic_checkbox_unchecked);
            }
            
            // Öncelik göstergesini ayarla - ImageView için doğrudan renk ayarla
            int priorityColor = context.getResources().getColor(R.color.priority_medium, context.getTheme());
            views.setInt(R.id.widget_priority_indicator, "setBackgroundColor", priorityColor);
            
            // Tıklama olayını ayarla
            Intent fillInIntent = new Intent();
            fillInIntent.putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.getId());
            views.setOnClickFillInIntent(R.id.widget_task_item_container, fillInIntent);
            
            // Checkbox tıklama olayını ayarla
            Intent checkboxIntent = new Intent();
            checkboxIntent.putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.getId());
            checkboxIntent.putExtra(TaskWidgetProvider.EXTRA_TASK_COMPLETED, !task.isCompleted());
            views.setOnClickFillInIntent(R.id.widget_task_checkbox, checkboxIntent);
            
            return views;
        } catch (Exception e) {
            Log.e(TAG, "getViewAt: Görünüm oluşturulurken hata: " + e.getMessage(), e);
            return getLoadingView(); // Null yerine yükleme görünümünü döndür
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        Log.d(TAG, "getLoadingView called");
        try {
            // Yükleme görünümü için RemoteViews oluştur
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_loading_view);
            return views;
        } catch (Exception e) {
            Log.e(TAG, "getLoadingView: Yükleme görünümü oluşturulurken hata: " + e.getMessage(), e);
            // Basit bir yükleme görünümü oluştur
            RemoteViews fallbackView = new RemoteViews(context.getPackageName(), android.R.layout.simple_list_item_1);
            fallbackView.setTextViewText(android.R.id.text1, "Yükleniyor...");
            return fallbackView;
        }
    }

    @Override
    public int getViewTypeCount() {
        // Normal görev öğesi ve yükleme görünümü için 2 farklı tip
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return position < tasks.size() ? tasks.get(position).getId() : position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
} 
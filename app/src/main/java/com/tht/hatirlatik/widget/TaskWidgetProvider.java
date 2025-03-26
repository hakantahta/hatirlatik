package com.tht.hatirlatik.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.tht.hatirlatik.MainActivity;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.database.AppDatabase;
import com.tht.hatirlatik.database.TaskDao;

/**
 * Widget provider sınıfı, widget'ın yaşam döngüsünü ve güncellemelerini yönetir.
 */
public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_DATA_UPDATED = "com.tht.hatirlatik.ACTION_DATA_UPDATED";
    public static final String ACTION_TASK_COMPLETED = "com.tht.hatirlatik.ACTION_TASK_COMPLETED";
    public static final String EXTRA_TASK_ID = "com.tht.hatirlatik.EXTRA_TASK_ID";
    public static final String EXTRA_TASK_COMPLETED = "com.tht.hatirlatik.EXTRA_TASK_COMPLETED";
    public static final String ACTION_VIEW_TASK = "com.tht.hatirlatik.ACTION_VIEW_TASK";
    public static final String ACTION_REFRESH_WIDGET = "com.tht.hatirlatik.ACTION_REFRESH_WIDGET";
    private static final String TAG = "TaskWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Her widget için güncelleme yap
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        
        try {
            super.onReceive(context, intent);
            
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "onReceive: Action is null");
                return;
            }
            
            // BOOT_COMPLETED olayını BootCompletedReceiver ele alacak
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.d(TAG, "onReceive: BOOT_COMPLETED alındı, fakat bu işlem BootCompletedReceiver tarafından ele alınacak");
                return;
            }
            
            // Tarih veya saat değiştiğinde widget'ı güncelle
            if (action.equals(Intent.ACTION_DATE_CHANGED) || 
                action.equals(Intent.ACTION_TIME_CHANGED) || 
                action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                
                Log.d(TAG, "onReceive: Tarih veya saat değişikliği algılandı: " + action);
                
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TaskWidgetProvider.class));
                
                // Widget'ı güncelle
                onUpdate(context, appWidgetManager, appWidgetIds);
                
                // Veri değişikliğini bildir
                for (int appWidgetId : appWidgetIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view);
                }
                
                Log.d(TAG, "onReceive: Widget tarih/saat değişikliği sonrası güncellendi");
            }
            // Widget'ı manuel olarak güncelle
            else if (action.equals(ACTION_REFRESH_WIDGET)) {
                Log.d(TAG, "onReceive: Manuel güncelleme isteği alındı");
                
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TaskWidgetProvider.class));
                
                // Widget'ı güncelle
                onUpdate(context, appWidgetManager, appWidgetIds);
                
                // Veri değişikliğini bildir
                for (int appWidgetId : appWidgetIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view);
                }
                
                Log.d(TAG, "onReceive: Widget manuel olarak güncellendi");
            }
            // Veri güncellendiğinde widget'ı güncelle
            else if (action.equals(ACTION_DATA_UPDATED)) {
                Log.d(TAG, "onReceive: Veri güncelleme bildirimi alındı");
                
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TaskWidgetProvider.class));
                
                // Widget'ı güncelle
                onUpdate(context, appWidgetManager, appWidgetIds);
                
                // Veri değişikliğini bildir
                for (int appWidgetId : appWidgetIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view);
                }
                
                Log.d(TAG, "onReceive: Widget veri güncellemesi sonrası yenilendi");
            }
            // Görev tamamlandı/tamamlanmadı olarak işaretle
            else if (action.equals(ACTION_TASK_COMPLETED)) {
                Log.d(TAG, "onReceive: Görev tamamlanma durumu değiştirildi");
                
                // Görev ID'sini al
                long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
                boolean isCompleted = intent.getBooleanExtra(EXTRA_TASK_COMPLETED, false);
                
                if (taskId != -1) {
                    Log.d(TAG, "onReceive: Görev ID: " + taskId + ", Tamamlandı: " + isCompleted);
                    
                    // Veritabanında görev durumunu güncelle
                    AppDatabase database = AppDatabase.getInstance(context);
                    if (database != null) {
                        TaskDao taskDao = database.taskDao();
                        taskDao.updateTaskCompletionStatus(taskId, isCompleted);
                        Log.d(TAG, "onReceive: Görev durumu güncellendi");
                        
                        // Widget'ı güncelle
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, TaskWidgetProvider.class));
                        
                        // Veri değişikliğini bildir
                        for (int appWidgetId : appWidgetIds) {
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view);
                        }
                        
                        Log.d(TAG, "onReceive: Widget güncellendi");
                    } else {
                        Log.e(TAG, "onReceive: Veritabanı bağlantısı kurulamadı");
                    }
                } else {
                    Log.e(TAG, "onReceive: Geçersiz görev ID");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onReceive: Hata oluştu: " + e.getMessage(), e);
        }
    }

    /**
     * Widget'ı günceller
     */
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "updateAppWidget: Widget ID: " + appWidgetId + " güncelleniyor");
        
        try {
            // Widget görünümünü oluştur
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_list);
            
            // Bugünün tarihini ayarla
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("tr"));
            String formattedDate = dateFormat.format(new Date());
            views.setTextViewText(R.id.widget_date, formattedDate);
            Log.d(TAG, "updateAppWidget: Tarih ayarlandı: " + formattedDate);
            
            // Widget başlığına tıklandığında uygulamayı aç
            Intent openAppIntent = new Intent(context, MainActivity.class);
            PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                    context, 
                    appWidgetId, 
                    openAppIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent);
            Log.d(TAG, "updateAppWidget: Başlık tıklama olayı ayarlandı");
            
            // ListView için RemoteViewsService'i ayarla
            Intent serviceIntent = new Intent(context, TaskWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // Intent'i benzersiz yapmak için URI ekle
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent);
            Log.d(TAG, "updateAppWidget: RemoteAdapter ayarlandı");
            
            // Boş görünümü ayarla
            views.setEmptyView(R.id.widget_list_view, R.id.empty_view);
            Log.d(TAG, "updateAppWidget: Boş görünüm ayarlandı");
            
            // Görev öğesine tıklandığında açılacak intent
            Intent taskClickIntent = new Intent(context, MainActivity.class);
            taskClickIntent.putExtra(MainActivity.EXTRA_OPEN_TASK_DETAIL, true);
            PendingIntent taskClickPendingIntent = PendingIntent.getActivity(
                    context, 
                    0, 
                    taskClickIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setPendingIntentTemplate(R.id.widget_list_view, taskClickPendingIntent);
            Log.d(TAG, "updateAppWidget: Liste öğesi tıklama şablonu ayarlandı");
            
            // Widget'ı güncelle
            appWidgetManager.updateAppWidget(appWidgetId, views);
            Log.d(TAG, "updateAppWidget: Widget görünümü güncellendi");
            
            // Veri değişikliğini bildir
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view);
            Log.d(TAG, "updateAppWidget: Widget veri değişikliği bildirildi");
        } catch (Exception e) {
            Log.e(TAG, "updateAppWidget: Widget güncellenirken hata: " + e.getMessage(), e);
        }
    }

    /**
     * Tüm widget'ları günceller
     */
    public static void updateAllWidgets(Context context) {
        try {
            Log.d(TAG, "Widget güncelleme başlatıldı");
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, TaskWidgetProvider.class));
            
            Log.d(TAG, "Toplam " + appWidgetIds.length + " widget bulundu");
            
            if (appWidgetIds.length > 0) {
                // Widget verilerini güncelle
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
                
                // Widget'ları güncelle
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                    Log.d(TAG, "Widget ID: " + appWidgetId + " güncellendi");
                }
                
                // Ayrıca bir yayın gönder
                Intent updateIntent = new Intent(context, TaskWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
                
                // Veri güncellemesi için özel bir yayın daha gönder
                Intent dataIntent = new Intent(context, TaskWidgetProvider.class);
                dataIntent.setAction(ACTION_DATA_UPDATED);
                context.sendBroadcast(dataIntent);
                
                Log.d(TAG, "Widget güncelleme yayınları gönderildi");
            } else {
                Log.d(TAG, "Güncellenecek widget bulunamadı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Widget güncellenirken hata oluştu: " + e.getMessage(), e);
        }
    }

    /**
     * Widget'ı yenileyen statik metod.
     * Bu metod, görev eklendiğinde, güncellendiğinde veya silindiğinde çağrılmalıdır.
     */
    public static void refreshWidget(Context context) {
        Log.d(TAG, "refreshWidget: Widget yenileme isteği alındı");
        
        try {
            // Widget'ı yenilemek için intent oluştur
            Intent refreshIntent = new Intent(context, TaskWidgetProvider.class);
            refreshIntent.setAction(ACTION_REFRESH_WIDGET);
            
            // Intent'i yayınla
            context.sendBroadcast(refreshIntent);
            
            // Ayrıca doğrudan güncelleme yap
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, TaskWidgetProvider.class));
            
            if (appWidgetIds.length > 0) {
                // Widget verilerini güncelle
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
                
                // Standart güncelleme yayını da gönder
                Intent updateIntent = new Intent(context, TaskWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
            }
            
            Log.d(TAG, "refreshWidget: Widget yenileme isteği gönderildi");
        } catch (Exception e) {
            Log.e(TAG, "refreshWidget: Widget yenileme hatası: " + e.getMessage(), e);
        }
    }
} 
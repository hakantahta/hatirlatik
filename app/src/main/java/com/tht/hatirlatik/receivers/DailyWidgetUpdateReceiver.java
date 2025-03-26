package com.tht.hatirlatik.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tht.hatirlatik.HatirlatikApplication;
import com.tht.hatirlatik.notification.AlarmHelper;
import com.tht.hatirlatik.widget.TaskWidgetProvider;

/**
 * Günlük widget güncellemelerini yöneten BroadcastReceiver.
 * Bu sınıf, gece yarısında veya cihaz yeniden başlatıldığında tetiklenir
 * ve widget'ı güncelleyerek yeni günün görevlerini gösterir.
 */
public class DailyWidgetUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "DailyWidgetUpdateRcvr";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: Günlük güncelleme alarmı tetiklendi. Action: " + action);
        
        try {
            // Eğer boot completed ise, sadece widget'ı güncelle, alarm kurma
            if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.d(TAG, "onReceive: Cihaz yeniden başlatıldı, sadece widget'ı güncelliyoruz");
                updateWidgets(context);
                return;
            }
            
            // Widget'ı güncelle
            updateWidgets(context);
            
            // Eğer action BOOT_COMPLETED değilse, bir sonraki gün için alarmı kur
            if (action == null || !action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                scheduleNextDayUpdate(context);
            }
            
            Log.d(TAG, "onReceive: Widget güncellendi" + 
                (action == null || !action.equals(Intent.ACTION_BOOT_COMPLETED) ? 
                " ve bir sonraki gün için alarm kuruldu" : ""));
        } catch (Exception e) {
            Log.e(TAG, "onReceive: Widget güncellenirken hata oluştu", e);
        }
    }
    
    /**
     * Widget'ı güncelleyen yardımcı metot.
     * Farklı güncelleme yöntemlerini kullanarak widget'ın doğru şekilde güncellenmesini sağlar.
     */
    private void updateWidgets(Context context) {
        try {
            // Tüm widget güncelleme yöntemlerini çağır
            TaskWidgetProvider.refreshWidget(context);
            HatirlatikApplication.updateWidgets();
            TaskWidgetProvider.updateAllWidgets(context);
            
            Log.d(TAG, "updateWidgets: Widget başarıyla güncellendi");
        } catch (Exception e) {
            Log.e(TAG, "updateWidgets: Widget güncellenirken hata oluştu", e);
        }
    }
    
    /**
     * Bir sonraki gün için widget güncelleme alarmını kurar.
     */
    private void scheduleNextDayUpdate(Context context) {
        try {
            AlarmHelper alarmHelper = new AlarmHelper(context);
            alarmHelper.scheduleDailyWidgetUpdate();
            
            Log.d(TAG, "scheduleNextDayUpdate: Bir sonraki gün için alarm kuruldu");
        } catch (Exception e) {
            Log.e(TAG, "scheduleNextDayUpdate: Alarm kurulurken hata oluştu", e);
        }
    }
} 
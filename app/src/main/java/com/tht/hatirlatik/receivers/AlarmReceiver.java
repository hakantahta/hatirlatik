package com.tht.hatirlatik.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.tht.hatirlatik.notification.NotificationHelper;
import com.tht.hatirlatik.preferences.PreferencesManager;

public class AlarmReceiver extends BroadcastReceiver {
    private static MediaPlayer mediaPlayer;
    private static final String ALARM_TAG = "Hatirlatik:AlarmWakeLock";
    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ekranı uyandır
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE,
                ALARM_TAG
        );
        wakeLock.acquire(60000); // 1 dakika boyunca ekranı açık tut

        // Görev bilgilerini al
        long taskId = intent.getLongExtra("taskId", -1);
        String taskTitle = intent.getStringExtra("taskTitle");
        String taskDescription = intent.getStringExtra("taskDescription");
        
        // Tercihleri kontrol et
        PreferencesManager preferencesManager = new PreferencesManager(context);
        
        // Bildirim göster (butonlarla birlikte)
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.showTaskNotification(taskId, taskTitle, taskDescription);

        // Ses çal
        if (preferencesManager.isNotificationSoundEnabled()) {
            playAlarmSound(context);
        }

        // Titreşim
        if (preferencesManager.isNotificationVibrationEnabled()) {
            vibrate(context);
        }

        // 1 dakika sonra sesi durdur
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            stopAlarmSound();
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }, 60000);
    }

    private void playAlarmSound(Context context) {
        try {
            // Eğer zaten çalıyorsa, önce durdur
            stopAlarmSound();
            
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, alarmSound);
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Titreşim paterni: 0ms bekleme, 500ms titreşim, 250ms bekleme, 500ms titreşim
            long[] pattern = {0, 500, 250, 500};
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }
    
    // Alarm sesini durdur - statik metot olarak dışarıdan erişilebilir
    public static void stopAlarmSound() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
} 
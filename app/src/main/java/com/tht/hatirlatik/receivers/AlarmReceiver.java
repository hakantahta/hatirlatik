package com.tht.hatirlatik.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.tht.hatirlatik.notification.NotificationHelper;
import com.tht.hatirlatik.preferences.PreferencesManager;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static MediaPlayer mediaPlayer;
    private static final String ALARM_TAG = "Hatirlatik:AlarmWakeLock";
    private static PowerManager.WakeLock wakeLock;
    private static Vibrator vibrator;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Alarm alındı");
        
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
        
        Log.d(TAG, "onReceive: Görev ID: " + taskId + ", Başlık: " + taskTitle);
        
        // Tercihleri kontrol et
        PreferencesManager preferencesManager = new PreferencesManager(context);
        
        // Bildirim göster (butonlarla birlikte)
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.showTaskNotification(taskId, taskTitle, taskDescription);

        // Ses çal
        if (preferencesManager.isNotificationSoundEnabled()) {
            Log.d(TAG, "onReceive: Alarm sesi çalınıyor");
            playAlarmSound(context);
        } else {
            Log.d(TAG, "onReceive: Bildirim sesi kapalı");
        }

        // Titreşim
        if (preferencesManager.isNotificationVibrationEnabled()) {
            Log.d(TAG, "onReceive: Titreşim başlatılıyor");
            vibrate(context);
        } else {
            Log.d(TAG, "onReceive: Titreşim kapalı");
        }

        // 1 dakika sonra sesi durdur
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "onReceive: 1 dakika doldu, alarm durduruluyor");
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
            
            // Ses seviyesini maksimuma çıkar
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
                Log.d(TAG, "playAlarmSound: Ses seviyesi maksimuma ayarlandı: " + maxVolume);
            }
            
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Log.d(TAG, "playAlarmSound: Alarm sesi URI: " + alarmSound);
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, alarmSound);
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(1.0f, 1.0f); // Maksimum ses seviyesi
            
            // Hazırlık tamamlandığında çalmaya başla
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "playAlarmSound: MediaPlayer hazır, çalma başlıyor");
                mp.start();
            });
            
            // Hata durumunda log tut
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "playAlarmSound: MediaPlayer hatası: what=" + what + ", extra=" + extra);
                return false;
            });
            
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "playAlarmSound: Hata oluştu", e);
            e.printStackTrace();
            
            // Hata durumunda alternatif yöntem dene
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                
                mediaPlayer = MediaPlayer.create(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    mediaPlayer.start();
                    Log.d(TAG, "playAlarmSound: Alternatif yöntemle alarm çalınıyor");
                }
            } catch (Exception e2) {
                Log.e(TAG, "playAlarmSound: Alternatif yöntem de başarısız oldu", e2);
            }
        }
    }

    private void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Titreşim paterni: 0ms bekleme, 500ms titreşim, 250ms bekleme, 500ms titreşim
            long[] pattern = {0, 500, 250, 500, 250, 500, 250, 500};
            
            try {
                // Statik değişkene atama yap
                AlarmReceiver.vibrator = vibrator;
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 indeksi ile tekrarla
                    Log.d(TAG, "vibrate: Android O+ için titreşim başlatıldı");
                } else {
                    vibrator.vibrate(pattern, 0); // 0 indeksi ile tekrarla
                    Log.d(TAG, "vibrate: Eski Android sürümü için titreşim başlatıldı");
                }
            } catch (Exception e) {
                Log.e(TAG, "vibrate: Titreşim başlatılırken hata oluştu", e);
            }
        } else {
            Log.w(TAG, "vibrate: Cihazda titreşim özelliği yok veya erişilemiyor");
        }
    }
    
    // Alarm sesini durdur - statik metot olarak dışarıdan erişilebilir
    public static void stopAlarmSound() {
        Log.d(TAG, "stopAlarmSound: Alarm sesi durduruluyor");
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    Log.d(TAG, "stopAlarmSound: MediaPlayer durduruldu");
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "stopAlarmSound: MediaPlayer serbest bırakıldı");
            } catch (Exception e) {
                Log.e(TAG, "stopAlarmSound: Hata oluştu", e);
            }
        }
        
        // Titreşimi de durdur
        stopVibration();
        
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
                wakeLock = null;
                Log.d(TAG, "stopAlarmSound: WakeLock serbest bırakıldı");
            } catch (Exception e) {
                Log.e(TAG, "stopAlarmSound: WakeLock serbest bırakılırken hata oluştu", e);
            }
        }
    }
    
    // Titreşimi durdur - statik metot olarak dışarıdan erişilebilir
    public static void stopVibration() {
        Log.d(TAG, "stopVibration: Titreşim durduruluyor");
        if (vibrator != null) {
            try {
                vibrator.cancel();
                Log.d(TAG, "stopVibration: Titreşim durduruldu");
            } catch (Exception e) {
                Log.e(TAG, "stopVibration: Titreşim durdurulurken hata oluştu", e);
            }
        }
    }
} 
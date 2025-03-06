package com.tht.hatirlatik.workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.tht.hatirlatik.model.NotificationType;
import com.tht.hatirlatik.model.RepeatType;
import com.tht.hatirlatik.notification.NotificationHelper;
import com.tht.hatirlatik.receivers.AlarmReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Rutin görevleri işleyen worker sınıfı.
 * Bu sınıf, tekrarlanan görevlerin bildirimlerini planlar ve gösterir.
 */
public class RoutineTaskWorker extends Worker {
    private static final String TAG = "RoutineTaskWorker";
    private final Context context;

    public RoutineTaskWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Görev bilgilerini al
            long taskId = getInputData().getLong("taskId", -1);
            String taskTitle = getInputData().getString("taskTitle");
            String taskDescription = getInputData().getString("taskDescription");
            String notificationTypeStr = getInputData().getString("notificationType");
            String repeatTypeStr = getInputData().getString("repeatType");
            
            // Rutin ayarlarını al
            int timesPerDay = getInputData().getInt("timesPerDay", 1);
            int intervalHours = getInputData().getInt("intervalHours", 1);
            String startTime = getInputData().getString("startTime");
            String endTime = getInputData().getString("endTime");
            String weekDays = getInputData().getString("weekDays");
            String monthDays = getInputData().getString("monthDays");
            long startDateMillis = getInputData().getLong("startDate", 0);
            long endDateMillis = getInputData().getLong("endDate", 0);
            
            // Tarih nesnelerini oluştur
            Date startDate = startDateMillis > 0 ? new Date(startDateMillis) : null;
            Date endDate = endDateMillis > 0 ? new Date(endDateMillis) : null;
            
            // NotificationType ve RepeatType enum değerlerini al
            NotificationType notificationType = NotificationType.NOTIFICATION;
            if (notificationTypeStr != null && !notificationTypeStr.isEmpty()) {
                try {
                    notificationType = NotificationType.valueOf(notificationTypeStr);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Geçersiz bildirim tipi: " + notificationTypeStr, e);
                }
            }
            
            RepeatType repeatType = RepeatType.NONE;
            if (repeatTypeStr != null && !repeatTypeStr.isEmpty()) {
                try {
                    repeatType = RepeatType.valueOf(repeatTypeStr);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Geçersiz tekrarlama tipi: " + repeatTypeStr, e);
                }
            }
            
            if (taskId != -1) {
                // Tekrarlama tipine göre işlem yap
                switch (repeatType) {
                    case DAILY:
                        scheduleDailyTask(taskId, taskTitle, taskDescription, notificationType, timesPerDay, intervalHours, startTime, endTime);
                        break;
                        
                    case WEEKLY:
                        scheduleWeeklyTask(taskId, taskTitle, taskDescription, notificationType, weekDays);
                        break;
                        
                    case MONTHLY:
                        scheduleMonthlyTask(taskId, taskTitle, taskDescription, notificationType, monthDays);
                        break;
                        
                    case WEEKDAYS:
                        scheduleWeekdaysTask(taskId, taskTitle, taskDescription, notificationType);
                        break;
                        
                    case WEEKENDS:
                        scheduleWeekendsTask(taskId, taskTitle, taskDescription, notificationType);
                        break;
                        
                    case CUSTOM:
                        scheduleCustomTask(taskId, taskTitle, taskDescription, notificationType, weekDays, monthDays, timesPerDay, intervalHours, startTime, endTime);
                        break;
                        
                    default:
                        // Varsayılan olarak günlük görev planla
                        scheduleDailyTask(taskId, taskTitle, taskDescription, notificationType, 1, 24, null, null);
                        break;
                }
                
                return Result.success();
            }
            
            return Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "doWork: Hata oluştu", e);
            e.printStackTrace();
            return Result.failure();
        }
    }
    
    /**
     * Günlük görev planla
     */
    private void scheduleDailyTask(long taskId, String taskTitle, String taskDescription, 
                                  NotificationType notificationType, int timesPerDay, 
                                  int intervalHours, String startTimeStr, String endTimeStr) {
        try {
            // Şu anki zamanı al
            Calendar now = Calendar.getInstance();
            
            // Bir yıl sonrası için bitiş tarihi belirle
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.YEAR, 1);
            
            // Geçerli günden başlayarak bir yıl boyunca her gün için görev planla
            Calendar currentDate = (Calendar) now.clone();
            
            while (currentDate.before(endDate)) {
                // Bugün veya gelecekteki bir gün için görev oluştur
                if (!currentDate.before(now)) {
                    // Bildirim zamanlarını hesapla
                    List<Calendar> notificationTimes = calculateDailyNotificationTimes(
                            currentDate, timesPerDay, intervalHours, startTimeStr, endTimeStr);
                    
                    for (Calendar taskTime : notificationTimes) {
                        long delayMillis = taskTime.getTimeInMillis() - now.getTimeInMillis();
                        
                        // Geçmiş zamanlar için atla
                        if (delayMillis < 0) {
                            continue;
                        }
                        
                        // Her gün ve saat için benzersiz bir görev ID'si oluştur
                        // Format: taskId_yıl_ay_gün_saat_dakika
                        String uniqueTaskId = taskId + "_" + 
                                             taskTime.get(Calendar.YEAR) + "_" + 
                                             (taskTime.get(Calendar.MONTH) + 1) + "_" + 
                                             taskTime.get(Calendar.DAY_OF_MONTH) + "_" +
                                             taskTime.get(Calendar.HOUR_OF_DAY) + "_" +
                                             taskTime.get(Calendar.MINUTE);
                        
                        // WorkManager için input data oluştur
                        Data inputData = new Data.Builder()
                                .putLong("taskId", taskId)
                                .putString("taskTitle", taskTitle)
                                .putString("taskDescription", taskDescription)
                                .putString("notificationType", notificationType.name())
                                .putString("uniqueTaskId", uniqueTaskId) // Benzersiz ID ekle
                                .build();
                        
                        // WorkRequest oluştur - her gün için benzersiz bir tag kullan
                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                                .setInputData(inputData)
                                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                                .addTag("routine_task_" + uniqueTaskId) // Benzersiz tag kullan
                                .build();
                        
                        // Work'ü planla
                        WorkManager.getInstance(context).enqueue(workRequest);
                        
                        Log.d(TAG, "Günlük görev planlandı: " + taskTitle + ", tarih: " + 
                              taskTime.getTime() + ", uniqueId: " + uniqueTaskId);
                    }
                }
                
                // Bir sonraki güne geç
                currentDate.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "scheduleDailyTask: Hata oluştu", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Günlük bildirim zamanlarını hesaplar
     * @param date Bildirim tarihi
     * @param timesPerDay Günde kaç kez bildirim gösterileceği
     * @param intervalHours Bildirimler arası saat cinsinden süre
     * @param startTimeStr Başlangıç saati (HH:mm formatında)
     * @param endTimeStr Bitiş saati (HH:mm formatında)
     * @return Bildirim zamanlarının listesi
     */
    private List<Calendar> calculateDailyNotificationTimes(Calendar date, int timesPerDay, 
                                                         int intervalHours, String startTimeStr, 
                                                         String endTimeStr) {
        List<Calendar> notificationTimes = new ArrayList<>();
        
        try {
            // Başlangıç ve bitiş saatlerini ayarla
            Calendar startTime = (Calendar) date.clone();
            Calendar endTime = (Calendar) date.clone();
            
            // Saat formatı
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            // Başlangıç saati belirtilmişse ayarla
            if (startTimeStr != null && !startTimeStr.isEmpty()) {
                try {
                    Date parsedTime = timeFormat.parse(startTimeStr);
                    if (parsedTime != null) {
                        Calendar parsedCal = Calendar.getInstance();
                        parsedCal.setTime(parsedTime);
                        
                        startTime.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
                        startTime.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
                        startTime.set(Calendar.SECOND, 0);
                        startTime.set(Calendar.MILLISECOND, 0);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Başlangıç saati ayrıştırılamadı: " + startTimeStr, e);
                }
            } else {
                // Varsayılan olarak sabah 8:00
                startTime.set(Calendar.HOUR_OF_DAY, 8);
                startTime.set(Calendar.MINUTE, 0);
                startTime.set(Calendar.SECOND, 0);
                startTime.set(Calendar.MILLISECOND, 0);
            }
            
            // Bitiş saati belirtilmişse ayarla
            if (endTimeStr != null && !endTimeStr.isEmpty()) {
                try {
                    Date parsedTime = timeFormat.parse(endTimeStr);
                    if (parsedTime != null) {
                        Calendar parsedCal = Calendar.getInstance();
                        parsedCal.setTime(parsedTime);
                        
                        endTime.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
                        endTime.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
                        endTime.set(Calendar.SECOND, 0);
                        endTime.set(Calendar.MILLISECOND, 0);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Bitiş saati ayrıştırılamadı: " + endTimeStr, e);
                }
            } else {
                // Varsayılan olarak akşam 20:00
                endTime.set(Calendar.HOUR_OF_DAY, 20);
                endTime.set(Calendar.MINUTE, 0);
                endTime.set(Calendar.SECOND, 0);
                endTime.set(Calendar.MILLISECOND, 0);
            }
            
            // Günde kaç kez tekrarlanacak
            int actualTimesPerDay = Math.min(timesPerDay, 24); // En fazla 24 kez
            
            // Tekrarlamalar arası süre (saat cinsinden)
            long intervalMillis = intervalHours * 60 * 60 * 1000L;
            
            if (actualTimesPerDay == 1) {
                // Günde bir kez
                notificationTimes.add(startTime);
            } else {
                // Günde birden fazla kez
                long totalDuration = endTime.getTimeInMillis() - startTime.getTimeInMillis();
                long interval = totalDuration / (actualTimesPerDay - 1);
                
                // İlk bildirimi ekle
                notificationTimes.add(startTime);
                
                // Diğer bildirimleri ekle
                for (int i = 1; i < actualTimesPerDay; i++) {
                    Calendar time = Calendar.getInstance();
                    time.setTimeInMillis(startTime.getTimeInMillis() + (interval * i));
                    notificationTimes.add(time);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "calculateDailyNotificationTimes: Hata oluştu", e);
            e.printStackTrace();
            
            // Hata durumunda varsayılan olarak günün 8:00'ında bir bildirim ekle
            Calendar defaultTime = (Calendar) date.clone();
            defaultTime.set(Calendar.HOUR_OF_DAY, 8);
            defaultTime.set(Calendar.MINUTE, 0);
            defaultTime.set(Calendar.SECOND, 0);
            defaultTime.set(Calendar.MILLISECOND, 0);
            notificationTimes.add(defaultTime);
        }
        
        return notificationTimes;
    }
    
    /**
     * Haftalık görev planla
     */
    private void scheduleWeeklyTask(long taskId, String taskTitle, String taskDescription, 
                                   NotificationType notificationType, String weekDaysStr) {
        try {
            // Varsayılan olarak her Pazartesi
            int[] weekDays = {Calendar.MONDAY};
            
            // Haftanın günlerini ayrıştır (1-7, 1=Pazartesi, 7=Pazar)
            if (weekDaysStr != null && !weekDaysStr.isEmpty()) {
                String[] days = weekDaysStr.split(",");
                weekDays = new int[days.length];
                
                for (int i = 0; i < days.length; i++) {
                    try {
                        int day = Integer.parseInt(days[i].trim());
                        // 1-7 aralığını Calendar.MONDAY-Calendar.SUNDAY aralığına dönüştür
                        weekDays[i] = day + Calendar.SUNDAY;
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Geçersiz gün formatı: " + days[i], e);
                        weekDays[i] = Calendar.MONDAY; // Varsayılan olarak Pazartesi
                    }
                }
            }
            
            // Şu anki zamanı al
            Calendar now = Calendar.getInstance();
            
            // Bir yıl sonrası için bitiş tarihi belirle
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.YEAR, 1);
            
            // Her belirtilen gün için bir yıl boyunca görev planla
            for (int weekDay : weekDays) {
                Calendar currentDate = (Calendar) now.clone();
                
                // İlk olarak, belirtilen haftanın gününe ayarla
                while (currentDate.get(Calendar.DAY_OF_WEEK) != weekDay) {
                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                // Bu günden başlayarak bir yıl boyunca her hafta için görev planla
                while (currentDate.before(endDate)) {
                    // Saat ayarı
                    Calendar taskDate = (Calendar) currentDate.clone();
                    taskDate.set(Calendar.HOUR_OF_DAY, 8); // Varsayılan saat 8:00
                    taskDate.set(Calendar.MINUTE, 0);
                    taskDate.set(Calendar.SECOND, 0);
                    taskDate.set(Calendar.MILLISECOND, 0);
                    
                    // Bugün veya gelecekteki bir gün için görev oluştur
                    if (!taskDate.before(now)) {
                        long delayMillis = taskDate.getTimeInMillis() - now.getTimeInMillis();
                        
                        // Her gün için benzersiz bir görev ID'si oluştur
                        // Format: taskId_yıl_ay_gün
                        String uniqueTaskId = taskId + "_" + 
                                             taskDate.get(Calendar.YEAR) + "_" + 
                                             (taskDate.get(Calendar.MONTH) + 1) + "_" + 
                                             taskDate.get(Calendar.DAY_OF_MONTH);
                        
                        // WorkManager için input data oluştur
                        Data inputData = new Data.Builder()
                                .putLong("taskId", taskId)
                                .putString("taskTitle", taskTitle)
                                .putString("taskDescription", taskDescription)
                                .putString("notificationType", notificationType.name())
                                .putInt("weekDay", weekDay)
                                .putString("uniqueTaskId", uniqueTaskId) // Benzersiz ID ekle
                                .build();
                        
                        // WorkRequest oluştur - her gün için benzersiz bir tag kullan
                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                                .setInputData(inputData)
                                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                                .addTag("routine_task_" + uniqueTaskId) // Benzersiz tag kullan
                                .build();
                        
                        // Work'ü planla
                        WorkManager.getInstance(context).enqueue(workRequest);
                        
                        Log.d(TAG, "Haftalık görev planlandı: " + taskTitle + ", tarih: " + 
                              taskDate.getTime() + ", uniqueId: " + uniqueTaskId);
                    }
                    
                    // Bir sonraki haftaya geç
                    currentDate.add(Calendar.WEEK_OF_YEAR, 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "scheduleWeeklyTask: Hata oluştu", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Aylık görev planla
     */
    private void scheduleMonthlyTask(long taskId, String taskTitle, String taskDescription, 
                                    NotificationType notificationType, String monthDaysStr) {
        try {
            // Varsayılan olarak her ayın 1'i
            int[] monthDays = {1};
            
            // Ayın günlerini ayrıştır (1-31)
            if (monthDaysStr != null && !monthDaysStr.isEmpty()) {
                String[] days = monthDaysStr.split(",");
                monthDays = new int[days.length];
                
                for (int i = 0; i < days.length; i++) {
                    try {
                        monthDays[i] = Integer.parseInt(days[i].trim());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Geçersiz gün formatı: " + days[i], e);
                        monthDays[i] = 1; // Varsayılan olarak ayın 1'i
                    }
                }
            }
            
            // Şu anki zamanı al
            Calendar now = Calendar.getInstance();
            
            // Bir yıl sonrası için bitiş tarihi belirle
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.YEAR, 1);
            
            // Her belirtilen gün için bir yıl boyunca görev planla
            for (int monthDay : monthDays) {
                Calendar currentDate = (Calendar) now.clone();
                
                // Ayın gününü ayarla
                currentDate.set(Calendar.DAY_OF_MONTH, monthDay);
                
                // Eğer bu ay geçtiyse, bir sonraki aya ayarla
                if (currentDate.before(now)) {
                    currentDate.add(Calendar.MONTH, 1);
                }
                
                // Bu günden başlayarak bir yıl boyunca her ay için görev planla
                while (currentDate.before(endDate)) {
                    // Saat ayarı
                    Calendar taskDate = (Calendar) currentDate.clone();
                    taskDate.set(Calendar.HOUR_OF_DAY, 8); // Varsayılan saat 8:00
                    taskDate.set(Calendar.MINUTE, 0);
                    taskDate.set(Calendar.SECOND, 0);
                    taskDate.set(Calendar.MILLISECOND, 0);
                    
                    long delayMillis = taskDate.getTimeInMillis() - now.getTimeInMillis();
                    
                    // Her gün için benzersiz bir görev ID'si oluştur
                    // Format: taskId_yıl_ay_gün
                    String uniqueTaskId = taskId + "_" + 
                                         taskDate.get(Calendar.YEAR) + "_" + 
                                         (taskDate.get(Calendar.MONTH) + 1) + "_" + 
                                         taskDate.get(Calendar.DAY_OF_MONTH);
                    
                    // WorkManager için input data oluştur
                    Data inputData = new Data.Builder()
                            .putLong("taskId", taskId)
                            .putString("taskTitle", taskTitle)
                            .putString("taskDescription", taskDescription)
                            .putString("notificationType", notificationType.name())
                            .putInt("monthDay", monthDay)
                            .putString("uniqueTaskId", uniqueTaskId) // Benzersiz ID ekle
                            .build();
                    
                    // WorkRequest oluştur - her gün için benzersiz bir tag kullan
                    OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                            .setInputData(inputData)
                            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                            .addTag("routine_task_" + uniqueTaskId) // Benzersiz tag kullan
                            .build();
                    
                    // Work'ü planla
                    WorkManager.getInstance(context).enqueue(workRequest);
                    
                    Log.d(TAG, "Aylık görev planlandı: " + taskTitle + ", tarih: " + 
                          taskDate.getTime() + ", uniqueId: " + uniqueTaskId);
                    
                    // Bir sonraki aya geç
                    currentDate.add(Calendar.MONTH, 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "scheduleMonthlyTask: Hata oluştu", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Hafta içi görev planla (Pazartesi-Cuma)
     */
    private void scheduleWeekdaysTask(long taskId, String taskTitle, String taskDescription, 
                                     NotificationType notificationType) {
        try {
            // Şu anki zamanı al
            Calendar now = Calendar.getInstance();
            
            // Haftanın günü
            int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            
            Log.d(TAG, "scheduleWeekdaysTask başladı: taskId=" + taskId + ", title=" + taskTitle + ", bugünkü gün=" + dayOfWeek);
            
            // Eğer bugün hafta içi bir günse ve saat uygunsa, bugün için bildirim planla
            if (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY) {
                // Saat ayarı
                Calendar taskDate = Calendar.getInstance();
                taskDate.set(Calendar.HOUR_OF_DAY, 8); // Varsayılan saat 8:00
                taskDate.set(Calendar.MINUTE, 0);
                taskDate.set(Calendar.SECOND, 0);
                taskDate.set(Calendar.MILLISECOND, 0);
                
                // Eğer şu anki saat 8:00'den sonra ise, bildirim gösterme
                if (now.get(Calendar.HOUR_OF_DAY) < 8 || 
                    (now.get(Calendar.HOUR_OF_DAY) == 8 && now.get(Calendar.MINUTE) == 0)) {
                    
                    long delayMillis = taskDate.getTimeInMillis() - now.getTimeInMillis();
                    
                    // WorkManager için input data oluştur
                    Data inputData = new Data.Builder()
                            .putLong("taskId", taskId)
                            .putString("taskTitle", taskTitle)
                            .putString("taskDescription", taskDescription)
                            .putString("notificationType", notificationType.name())
                            .putInt("weekDay", dayOfWeek)
                            .putBoolean("isRoutineTask", true) // Rutin görev olduğunu belirt
                            .putString("repeatType", RepeatType.WEEKDAYS.name()) // Tekrarlama tipini belirt
                            .build();
                    
                    // WorkRequest oluştur
                    OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                            .setInputData(inputData)
                            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                            .addTag("routine_task_" + taskId)
                            .build();
                    
                    // Work'ü planla
                    WorkManager.getInstance(context).enqueue(workRequest);
                    
                    Log.d(TAG, "Hafta içi görev planlandı: " + taskTitle + ", bugün için: " + taskDate.getTime());
                }
            }
            
            // Bir sonraki hafta içi gün için bildirim planla
            Calendar nextWeekday = Calendar.getInstance();
            
            // Eğer bugün Cuma ise, bir sonraki Pazartesi'ye ayarla
            if (dayOfWeek == Calendar.FRIDAY) {
                nextWeekday.add(Calendar.DAY_OF_WEEK, 3); // Cuma -> Pazartesi (3 gün sonra)
            } 
            // Eğer bugün Cumartesi ise, bir sonraki Pazartesi'ye ayarla
            else if (dayOfWeek == Calendar.SATURDAY) {
                nextWeekday.add(Calendar.DAY_OF_WEEK, 2); // Cumartesi -> Pazartesi (2 gün sonra)
            } 
            // Eğer bugün Pazar ise, bir sonraki Pazartesi'ye ayarla
            else if (dayOfWeek == Calendar.SUNDAY) {
                nextWeekday.add(Calendar.DAY_OF_WEEK, 1); // Pazar -> Pazartesi (1 gün sonra)
            } 
            // Diğer günler için bir sonraki güne ayarla
            else {
                nextWeekday.add(Calendar.DAY_OF_WEEK, 1);
            }
            
            // Saat ayarı
            nextWeekday.set(Calendar.HOUR_OF_DAY, 8); // Varsayılan saat 8:00
            nextWeekday.set(Calendar.MINUTE, 0);
            nextWeekday.set(Calendar.SECOND, 0);
            nextWeekday.set(Calendar.MILLISECOND, 0);
            
            long delayMillis = nextWeekday.getTimeInMillis() - now.getTimeInMillis();
            
            // WorkManager için input data oluştur
            Data inputData = new Data.Builder()
                    .putLong("taskId", taskId)
                    .putString("taskTitle", taskTitle)
                    .putString("taskDescription", taskDescription)
                    .putString("notificationType", notificationType.name())
                    .putInt("weekDay", nextWeekday.get(Calendar.DAY_OF_WEEK))
                    .putBoolean("isRoutineTask", true) // Rutin görev olduğunu belirt
                    .putString("repeatType", RepeatType.WEEKDAYS.name()) // Tekrarlama tipini belirt
                    .build();
            
            // WorkRequest oluştur
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RoutineTaskWorker.class) // Kendini tekrar çağır
                    .setInputData(inputData)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .addTag("routine_task_next_" + taskId)
                    .build();
            
            // Work'ü planla
            WorkManager.getInstance(context).enqueue(workRequest);
            
            Log.d(TAG, "Bir sonraki hafta içi görev planlandı: " + taskTitle + ", tarih: " + nextWeekday.getTime());
            
        } catch (Exception e) {
            Log.e(TAG, "scheduleWeekdaysTask: Hata oluştu", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Hafta sonu görev planla (Cumartesi-Pazar)
     */
    private void scheduleWeekendsTask(long taskId, String taskTitle, String taskDescription, 
                                     NotificationType notificationType) {
        try {
            // Şu anki zamanı al
            Calendar now = Calendar.getInstance();
            
            // Bir yıl sonrası için bitiş tarihi belirle
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.YEAR, 1);
            
            // Geçerli haftadan başlayarak bir yıl boyunca her hafta sonu günü için görev planla
            Calendar currentDate = (Calendar) now.clone();
            
            while (currentDate.before(endDate)) {
                // Haftanın günü
                int dayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK);
                
                // Eğer hafta sonu bir günse (Cumartesi veya Pazar)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    // Bugün veya gelecekteki bir gün için görev oluştur
                    if (!currentDate.before(now)) {
                        // Saat ayarı
                        Calendar taskDate = (Calendar) currentDate.clone();
                        taskDate.set(Calendar.HOUR_OF_DAY, 10); // Hafta sonu için varsayılan saat 10:00
                        taskDate.set(Calendar.MINUTE, 0);
                        taskDate.set(Calendar.SECOND, 0);
                        taskDate.set(Calendar.MILLISECOND, 0);
                        
                        long delayMillis = taskDate.getTimeInMillis() - now.getTimeInMillis();
                        
                        // Her gün için benzersiz bir görev ID'si oluştur
                        // Format: taskId_yıl_ay_gün
                        String uniqueTaskId = taskId + "_" + 
                                             taskDate.get(Calendar.YEAR) + "_" + 
                                             (taskDate.get(Calendar.MONTH) + 1) + "_" + 
                                             taskDate.get(Calendar.DAY_OF_MONTH);
                        
                        // WorkManager için input data oluştur
                        Data inputData = new Data.Builder()
                                .putLong("taskId", taskId)
                                .putString("taskTitle", taskTitle)
                                .putString("taskDescription", taskDescription)
                                .putString("notificationType", notificationType.name())
                                .putInt("weekDay", dayOfWeek)
                                .putString("uniqueTaskId", uniqueTaskId) // Benzersiz ID ekle
                                .build();
                        
                        // WorkRequest oluştur - her gün için benzersiz bir tag kullan
                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                                .setInputData(inputData)
                                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                                .addTag("routine_task_" + uniqueTaskId) // Benzersiz tag kullan
                                .build();
                        
                        // Work'ü planla
                        WorkManager.getInstance(context).enqueue(workRequest);
                        
                        Log.d(TAG, "Hafta sonu görev planlandı: " + taskTitle + ", tarih: " + 
                              taskDate.getTime() + ", uniqueId: " + uniqueTaskId);
                    }
                }
                
                // Bir sonraki güne geç
                currentDate.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "scheduleWeekendsTask: Hata oluştu", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Özel görev planla
     */
    private void scheduleCustomTask(long taskId, String taskTitle, String taskDescription, 
                                   NotificationType notificationType, String weekDaysStr, 
                                   String monthDaysStr, int timesPerDay, int intervalHours, 
                                   String startTimeStr, String endTimeStr) {
        try {
            // Hem haftalık hem de aylık tekrarlama varsa, her ikisini de planla
            if (weekDaysStr != null && !weekDaysStr.isEmpty()) {
                scheduleWeeklyTask(taskId, taskTitle, taskDescription, notificationType, weekDaysStr);
            }
            
            if (monthDaysStr != null && !monthDaysStr.isEmpty()) {
                scheduleMonthlyTask(taskId, taskTitle, taskDescription, notificationType, monthDaysStr);
            }
            
            // Eğer her ikisi de yoksa, günlük görev olarak planla
            if ((weekDaysStr == null || weekDaysStr.isEmpty()) && 
                (monthDaysStr == null || monthDaysStr.isEmpty())) {
                scheduleDailyTask(taskId, taskTitle, taskDescription, notificationType, 
                                 timesPerDay, intervalHours, startTimeStr, endTimeStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "scheduleCustomTask: Hata oluştu", e);
            e.printStackTrace();
        }
    }
} 
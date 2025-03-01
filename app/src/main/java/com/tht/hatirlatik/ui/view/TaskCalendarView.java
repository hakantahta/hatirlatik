package com.tht.hatirlatik.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.tht.hatirlatik.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Görevlerin olduğu günleri işaretleyen özel takvim görünümü
 */
public class TaskCalendarView extends FrameLayout {
    
    // Görev durumlarını temsil eden enum
    public enum TaskStatus {
        NO_TASKS,       // Görev yok (sarı)
        HAS_TASKS,      // Görev var (yeşil)
        ALL_COMPLETED   // Tüm görevler tamamlanmış (kırmızı)
    }
    
    private final Map<String, TaskStatus> markedDatesWithStatus = new HashMap<>();
    private final SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();
    private CalendarView calendarView;
    private TextView infoTextView;
    
    // Tarih değişikliği listener'ı
    private CalendarView.OnDateChangeListener dateChangeListener;
    
    public TaskCalendarView(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public TaskCalendarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public TaskCalendarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        // Layout'u inflate et
        LayoutInflater.from(context).inflate(R.layout.view_task_calendar, this, true);
        
        // View'ları bul
        calendarView = findViewById(R.id.calendar_view_internal);
        infoTextView = findViewById(R.id.text_calendar_info);
        
        // Varsayılan ayarları yap
        calendarView.setFirstDayOfWeek(2); // Pazartesi
        
        // Takvim görünümünü özelleştir
        setupCalendarView();
    }
    
    /**
     * Takvim görünümünü özelleştirir
     */
    private void setupCalendarView() {
        // Takvim günlerinin görünümünü değiştirmek için bir listener ekleyelim
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Tarih değiştiğinde, o günün durumuna göre işlem yapalım
            
            // Eğer bir listener varsa, onu da çağıralım
            if (dateChangeListener != null) {
                dateChangeListener.onSelectedDayChange(view, year, month, dayOfMonth);
            }
        });
    }
    
    /**
     * Belirli bir tarihi işaretler
     * @param dateKey "yyyy-MM-dd" formatında tarih
     * @param status Görev durumu
     */
    public void markDate(String dateKey, TaskStatus status) {
        markedDatesWithStatus.put(dateKey, status);
        updateCalendarAppearance();
    }
    
    /**
     * Tüm işaretleri temizler
     */
    public void clearMarkedDates() {
        markedDatesWithStatus.clear();
        updateCalendarAppearance();
    }
    
    /**
     * Birden fazla tarihi işaretler
     * @param dateKeysWithStatus "yyyy-MM-dd" formatında tarihler ve durumları
     */
    public void setMarkedDatesWithStatus(Map<String, TaskStatus> dateKeysWithStatus) {
        markedDatesWithStatus.clear();
        markedDatesWithStatus.putAll(dateKeysWithStatus);
        updateCalendarAppearance();
    }
    
    /**
     * Eski metodu koruyoruz, ama artık tüm tarihleri HAS_TASKS olarak işaretliyoruz
     * @param dateKeys "yyyy-MM-dd" formatında tarihler
     */
    public void setMarkedDates(Set<String> dateKeys) {
        markedDatesWithStatus.clear();
        for (String dateKey : dateKeys) {
            markedDatesWithStatus.put(dateKey, TaskStatus.HAS_TASKS);
        }
        updateCalendarAppearance();
    }
    
    /**
     * Belirli bir tarihin işaretli olup olmadığını kontrol eder
     * @param dateKey "yyyy-MM-dd" formatında tarih
     * @return Tarih işaretli ise true, değilse false
     */
    public boolean isDateMarked(String dateKey) {
        return markedDatesWithStatus.containsKey(dateKey);
    }
    
    /**
     * Belirli bir tarihin görev durumunu döndürür
     * @param dateKey "yyyy-MM-dd" formatında tarih
     * @return Görev durumu, tarih işaretli değilse null
     */
    public TaskStatus getDateStatus(String dateKey) {
        return markedDatesWithStatus.get(dateKey);
    }
    
    /**
     * Takvim görünümünü günceller
     */
    private void updateCalendarAppearance() {
        if (markedDatesWithStatus.isEmpty()) {
            infoTextView.setVisibility(GONE);
        } else {
            infoTextView.setVisibility(VISIBLE);
            infoTextView.setText(getResources().getString(R.string.calendar_marked_days_info, markedDatesWithStatus.size()));
        }
        
        // Not: Android'in standart CalendarView'ı doğrudan özelleştirmeye izin vermez
        // Bu nedenle, takvim günlerini özelleştiremiyoruz
        // Ancak, takvim altındaki lejant ile kullanıcıya bilgi veriyoruz
    }
    
    /**
     * Tarih değişikliği listener'ını ayarlar
     */
    public void setOnDateChangeListener(CalendarView.OnDateChangeListener listener) {
        this.dateChangeListener = listener;
        
        // Kendi listener'ımızı da ayarlayalım
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Önce kendi işlemlerimizi yapalım
            
            // Sonra kullanıcının listener'ını çağıralım
            if (dateChangeListener != null) {
                dateChangeListener.onSelectedDayChange(view, year, month, dayOfMonth);
            }
        });
    }
    
    /**
     * Takvim görünümünü döndürür
     */
    public CalendarView getCalendarView() {
        return calendarView;
    }
} 
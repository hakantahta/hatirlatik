package com.tht.hatirlatik.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.util.Date;

/**
 * Görev rutinlerinin ayarlarını tutan model sınıfı.
 * Bu sınıf, bir görevin tekrarlama ayarlarını (hangi günler, saatler, aralıklar vb.) tutar.
 */
@Entity(tableName = "routine_settings",
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("taskId")})
public class RoutineSettings {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "taskId")
    private long taskId;
    
    @ColumnInfo(name = "repeatType")
    private RepeatType repeatType;
    
    // Haftanın günleri (1-7, 1=Pazartesi, 7=Pazar)
    // Örnek: "1,2,3,4,5" (Pazartesi-Cuma)
    @ColumnInfo(name = "weekDays")
    private String weekDays;
    
    // Ayın günleri (1-31)
    // Örnek: "1,15" (Her ayın 1'i ve 15'i)
    @ColumnInfo(name = "monthDays")
    private String monthDays;
    
    // Günde kaç kez tekrarlanacak
    @ColumnInfo(name = "timesPerDay")
    private int timesPerDay;
    
    // Tekrarlamalar arası saat cinsinden süre
    @ColumnInfo(name = "intervalHours")
    private int intervalHours;
    
    // Başlangıç saati (saat:dakika formatında)
    @ColumnInfo(name = "startTime")
    private String startTime;
    
    // Bitiş saati (saat:dakika formatında)
    @ColumnInfo(name = "endTime")
    private String endTime;
    
    // Rutin başlangıç tarihi
    @ColumnInfo(name = "startDate")
    private Date startDate;
    
    // Rutin bitiş tarihi (null ise süresiz)
    @ColumnInfo(name = "endDate")
    private Date endDate;
    
    // Boş constructor (Room için gerekli)
    public RoutineSettings() {
        this.repeatType = RepeatType.NONE;
        this.timesPerDay = 1;
        this.intervalHours = 0;
        this.startDate = new Date();
    }
    
    // Getters
    public long getId() { return id; }
    public long getTaskId() { return taskId; }
    public RepeatType getRepeatType() { return repeatType; }
    public String getWeekDays() { return weekDays; }
    public String getMonthDays() { return monthDays; }
    public int getTimesPerDay() { return timesPerDay; }
    public int getIntervalHours() { return intervalHours; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    
    // Setters
    public void setId(long id) { this.id = id; }
    public void setTaskId(long taskId) { this.taskId = taskId; }
    public void setRepeatType(RepeatType repeatType) { this.repeatType = repeatType; }
    public void setWeekDays(String weekDays) { this.weekDays = weekDays; }
    public void setMonthDays(String monthDays) { this.monthDays = monthDays; }
    public void setTimesPerDay(int timesPerDay) { this.timesPerDay = timesPerDay; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
} 
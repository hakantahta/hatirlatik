package com.tht.hatirlatik.data.database.converter;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * Room veritabanı için Date tip dönüştürücü
 */
public class DateConverter {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
} 
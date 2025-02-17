package com.tht.hatirlatik.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.tht.hatirlatik.data.database.converter.DateConverter;
import com.tht.hatirlatik.data.database.converter.EnumConverter;
import com.tht.hatirlatik.data.database.dao.TaskDao;
import com.tht.hatirlatik.data.database.entity.TaskEntity;

/**
 * Room veritabanı sınıfı
 */
@Database(entities = {TaskEntity.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class, EnumConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "hatirlatik_db";
    private static volatile AppDatabase instance;

    // DAO getter metodları
    public abstract TaskDao taskDao();

    // Singleton pattern
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
} 
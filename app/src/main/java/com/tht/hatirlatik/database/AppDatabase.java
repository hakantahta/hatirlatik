package com.tht.hatirlatik.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.tht.hatirlatik.model.Task;

@Database(entities = {Task.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "hatirlatik_db";
    private static volatile AppDatabase instance;

    public abstract TaskDao taskDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    // Veritabanını uygulama verilerinin içine taşı
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    // Veritabanını yedekleme ve geri yükleme işlemlerinden hariç tut
                    .allowMainThreadQueries() // Sadece widget için gerekli
                    .build();
        }
        return instance;
    }

    public static void destroyInstance() {
        if (instance != null && instance.isOpen()) {
            instance.close();
        }
        instance = null;
    }
} 
package com.tht.hatirlatik.database;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.tht.hatirlatik.model.Task;
import com.tht.hatirlatik.model.RoutineSettings;

@Database(entities = {Task.class, RoutineSettings.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "hatirlatik_db";
    private static volatile AppDatabase instance;
    private static final String TAG = "AppDatabase";

    public abstract TaskDao taskDao();
    public abstract RoutineSettingsDao routineSettingsDao();

    // Veritabanı sürüm 1'den 2'ye migration
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Rutin ayarları tablosunu oluştur
            database.execSQL("CREATE TABLE IF NOT EXISTS `routine_settings` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`taskId` INTEGER NOT NULL, " +
                    "`repeatType` TEXT, " +
                    "`weekDays` TEXT, " +
                    "`monthDays` TEXT, " +
                    "`timesPerDay` INTEGER NOT NULL DEFAULT 1, " +
                    "`intervalHours` INTEGER NOT NULL DEFAULT 0, " +
                    "`startTime` TEXT, " +
                    "`endTime` TEXT, " +
                    "`startDate` INTEGER, " +
                    "`endDate` INTEGER, " +
                    "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            
            // taskId için indeks oluştur
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_settings_taskId` ON `routine_settings` (`taskId`)");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null || !instance.isOpen()) {
            try {
                Log.d(TAG, "Veritabanı oluşturuluyor veya yeniden açılıyor");
                instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .addMigrations(MIGRATION_1_2)
                        .allowMainThreadQueries() // Widget için gerekli
                        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                        .build();
                Log.d(TAG, "Veritabanı başarıyla oluşturuldu");
            } catch (Exception e) {
                Log.e(TAG, "Veritabanı oluşturulurken hata: " + e.getMessage(), e);
                return null; // Hata durumunda null döndür
            }
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
package com.example.lab6.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.lab6.data.dao.EquipmentDao;
import com.example.lab6.data.dao.WorkoutSetDao;
import com.example.lab6.data.entity.Equipment;
import com.example.lab6.data.entity.WorkoutSet;

import java.util.concurrent.TimeUnit;

/**
 * Потокобезопасный синглтон базы данных Room.
 * При первом создании базы сразу наполняю её реалистичной историей тренировок,
 * чтобы графики, рекорды, поиск, фильтры и автоочистка работали сразу после установки.
 */
@Database(entities = {Equipment.class, WorkoutSet.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract EquipmentDao equipmentDao();

    public abstract WorkoutSetDao workoutSetDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "progress_zal.db"
                            )
                            .addCallback(new SeedCallback())
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * Стартовое наполнение базы в рамках транзакции создания, чтобы первый экран сразу открылся с данными.
     */
    private static class SeedCallback extends RoomDatabase.Callback {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            long now = System.currentTimeMillis();
            long dayMs = TimeUnit.DAYS.toMillis(1);
            long hourMs = TimeUnit.HOURS.toMillis(1);

            long benchId = insertEquipment(
                    db,
                    "Жим штанги лёжа",
                    "Грудь",
                    "Хват чуть шире плеч, пауза в нижней точке 1 сек.",
                    now - 10 * dayMs
            );
            long crossoverId = insertEquipment(
                    db,
                    "Сведение рук в кроссовере",
                    "Грудь",
                    "Локти слегка согнуты, плавное растяжение.",
                    now - 9 * dayMs
            );
            long latPullId = insertEquipment(
                    db,
                    "Тяга верхнего блока",
                    "Спина",
                    "Сведение лопаток внизу, без рывков корпусом.",
                    now - 8 * dayMs
            );
            long squatId = insertEquipment(
                    db,
                    "Приседания со штангой",
                    "Ноги",
                    "Глубина ниже параллели, спина ровная.",
                    now - 7 * dayMs
            );
            long shoulderPressId = insertEquipment(
                    db,
                    "Жим гантелей сидя",
                    "Плечи",
                    "Спинка скамьи под углом 80 градусов.",
                    now - 6 * dayMs
            );
            long bicepsId = insertEquipment(
                    db,
                    "Подъём штанги на бицепс",
                    "Руки",
                    "EZ-гриф, локти прижаты к корпусу.",
                    now - 5 * dayMs
            );

            // Подходы за последние 7 дней (20 штук), чтобы сводка и график прогресса сразу ожили
            // 6 дней назад — Грудь + Руки
            insertSet(db, benchId, 75f, 10, now - 6 * dayMs);
            insertSet(db, benchId, 80f, 8, now - 6 * dayMs + 15 * 60_000L);
            insertSet(db, bicepsId, 30f, 12, now - 6 * dayMs + 30 * 60_000L);

            // 5 дней назад — Спина
            insertSet(db, latPullId, 55f, 12, now - 5 * dayMs);
            insertSet(db, latPullId, 60f, 10, now - 5 * dayMs + 12 * 60_000L);
            insertSet(db, latPullId, 65f, 8, now - 5 * dayMs + 24 * 60_000L);

            // 4 дня назад — Ноги
            insertSet(db, squatId, 95f, 8, now - 4 * dayMs);
            insertSet(db, squatId, 105f, 6, now - 4 * dayMs + 18 * 60_000L);
            insertSet(db, squatId, 110f, 5, now - 4 * dayMs + 35 * 60_000L);

            // 3 дня назад — Плечи + Грудь
            insertSet(db, shoulderPressId, 20f, 12, now - 3 * dayMs);
            insertSet(db, shoulderPressId, 24f, 10, now - 3 * dayMs + 15 * 60_000L);
            insertSet(db, crossoverId, 25f, 15, now - 3 * dayMs + 30 * 60_000L);

            // 2 дня назад — Спина + Руки
            insertSet(db, latPullId, 65f, 10, now - 2 * dayMs);
            insertSet(db, bicepsId, 32.5f, 10, now - 2 * dayMs + 20 * 60_000L);

            // Вчера — Ноги + Плечи
            insertSet(db, squatId, 110f, 6, now - 1 * dayMs);
            insertSet(db, shoulderPressId, 26f, 8, now - 1 * dayMs + 20 * 60_000L);

            // Сегодня — свежая тренировка (4 подхода)
            insertSet(db, benchId, 82.5f, 8, now - 2 * hourMs);
            insertSet(db, benchId, 87.5f, 6, now - 90 * 60_000L);
            insertSet(db, crossoverId, 30f, 12, now - 45 * 60_000L);
            insertSet(db, bicepsId, 35f, 10, now - 20 * 60_000L);

            // Две старые записи 45 дней назад — нужны, чтобы в настройках автоочистки сразу было что удалить
            insertSet(db, benchId, 65f, 10, now - 45 * dayMs);
            insertSet(db, squatId, 85f, 8, now - 45 * dayMs + hourMs);
        }

        private long insertEquipment(
                SupportSQLiteDatabase db,
                String name,
                String muscleGroup,
                String notes,
                long createdAt
        ) {
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("muscleGroup", muscleGroup);
            cv.put("notes", notes);
            cv.put("photoPath", (String) null);
            cv.put("createdAt", createdAt);
            return db.insert("equipment", SQLiteDatabase.CONFLICT_NONE, cv);
        }

        private void insertSet(
                SupportSQLiteDatabase db,
                long equipmentId,
                float weightKg,
                int reps,
                long dateMs
        ) {
            ContentValues cv = new ContentValues();
            cv.put("equipmentId", equipmentId);
            cv.put("weightKg", weightKg);
            cv.put("reps", reps);
            cv.put("dateMs", dateMs);
            db.insert("workout_sets", SQLiteDatabase.CONFLICT_NONE, cv);
        }
    }
}

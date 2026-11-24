package com.example.lab6.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.lab6.data.dao.EquipmentDao;
import com.example.lab6.data.dao.WorkoutSetDao;
import com.example.lab6.data.db.AppDatabase;
import com.example.lab6.data.entity.Equipment;
import com.example.lab6.data.entity.WorkoutSet;
import com.example.lab6.data.projection.DailyVolumePoint;
import com.example.lab6.data.projection.EquipmentWithMaxSet;
import com.example.lab6.data.projection.WorkoutSetWithEquipmentName;
import com.example.lab6.util.CsvExporter;
import com.example.lab6.util.DateUtils;
import com.example.lab6.util.PrDetector;
import com.example.lab6.worker.CleanupWorker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Репозиторий — единая точка доступа к базе данных и файловым операциям.
 * Все чтения возвращаю через LiveData, а записи, экспорт CSV и очистку выполняю в пуле потоков.
 */
public class WorkoutRepository {

    public interface OnSetAddedCallback {
        void onAdded(boolean isNewRecord, float improvementKg);
    }

    public interface OnCsvExportCallback {
        void onSuccess(File csvFile, int rowCount);

        void onError(String message);
    }

    public interface OnOldCountCallback {
        void onCountLoaded(int count);
    }

    public interface OnCleanupDoneCallback {
        void onDone(CleanupWorker.CleanupResult result);
    }

    private static final Locale RU_LOCALE = new Locale("ru", "RU");

    private final Context appContext;
    private final AppDatabase database;
    private final EquipmentDao equipmentDao;
    private final WorkoutSetDao workoutSetDao;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WorkoutRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.database = AppDatabase.getInstance(appContext);
        this.equipmentDao = database.equipmentDao();
        this.workoutSetDao = database.workoutSetDao();
    }

    /**
     * Комбинированный поиск по названию и фильтрация по группе мышц на стороне SQL.
     * Подставляю % в параметры и передаю варианты регистра, чтобы кириллический LIKE работал надёжно.
     */
    public LiveData<List<EquipmentWithMaxSet>> searchEquipmentWithStats(String rawQuery, String muscleGroup) {
        String safeGroup = muscleGroup == null ? "" : muscleGroup.trim();
        String trimmed = rawQuery == null ? "" : rawQuery.trim();
        if (trimmed.isEmpty()) {
            return equipmentDao.searchEquipmentWithStats(safeGroup, "", "", "");
        }
        String lower = trimmed.toLowerCase(RU_LOCALE);
        String cap = lower.substring(0, 1).toUpperCase(RU_LOCALE) + lower.substring(1);
        return equipmentDao.searchEquipmentWithStats(
                safeGroup,
                "%" + trimmed + "%",
                "%" + lower + "%",
                "%" + cap + "%"
        );
    }

    public LiveData<Equipment> getEquipmentById(int equipmentId) {
        return equipmentDao.getById(equipmentId);
    }

    public LiveData<List<WorkoutSet>> getSetsForEquipment(int equipmentId) {
        return workoutSetDao.getSetsForEquipment(equipmentId);
    }

    public LiveData<Integer> getTodaySetsCount() {
        return workoutSetDao.getTodaySetsCount(DateUtils.getStartOfTodayMs());
    }

    public LiveData<Float> getTodayVolumeKg() {
        return workoutSetDao.getTodayVolumeKg(DateUtils.getStartOfTodayMs());
    }

    public LiveData<WorkoutSetWithEquipmentName> getBestOverallRecord() {
        return workoutSetDao.getBestOverallRecord();
    }

    public LiveData<List<DailyVolumePoint>> getLast7DaysVolume() {
        return workoutSetDao.getDailyVolumeSince(DateUtils.getDaysAgoStartMs(6));
    }

    public void addEquipment(String name, String muscleGroup, String notes) {
        executor.execute(() -> {
            Equipment equipment = new Equipment(
                    name.trim(),
                    muscleGroup.trim(),
                    notes == null ? "" : notes.trim(),
                    null,
                    System.currentTimeMillis()
            );
            equipmentDao.insert(equipment);
        });
    }

    public void deleteEquipment(Equipment equipment, Runnable onDone) {
        executor.execute(() -> {
            if (equipment.photoPath != null && !equipment.photoPath.isEmpty()) {
                File photo = new File(equipment.photoPath);
                if (photo.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    photo.delete();
                }
            }
            equipmentDao.delete(equipment);
            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }

    /**
     * Добавляю подход в фоне, предварительно проверяя текущий максимум в SQL,
     * чтобы сразу сообщить экрану о новом личном рекорде.
     */
    public void addSetWithPrCheck(int equipmentId, float weightKg, int reps, OnSetAddedCallback callback) {
        executor.execute(() -> {
            Float previousMax = workoutSetDao.getMaxWeightForEquipmentSync(equipmentId);
            boolean isPr = PrDetector.isNewRecord(previousMax, weightKg);
            float improvement = PrDetector.getImprovementKg(previousMax, weightKg);

            WorkoutSet set = new WorkoutSet(equipmentId, weightKg, reps, System.currentTimeMillis());
            workoutSetDao.insert(set);

            if (callback != null) {
                mainHandler.post(() -> callback.onAdded(isPr, improvement));
            }
        });
    }

    public void deleteSet(WorkoutSet set) {
        executor.execute(() -> workoutSetDao.delete(set));
    }

    /**
     * Читаю полную историю через JOIN и записываю CSV-файл в фоне.
     */
    public void exportHistoryToCsv(OnCsvExportCallback callback) {
        executor.execute(() -> {
            try {
                List<WorkoutSetWithEquipmentName> rows = workoutSetDao.getAllSetsWithEquipmentNameSync();
                File file = CsvExporter.exportToCacheFile(appContext, rows);
                int count = rows == null ? 0 : rows.size();
                mainHandler.post(() -> callback.onSuccess(file, count));
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Считаю в SQL количество устаревших подходов для выбранного срока хранения.
     */
    public void countOldSets(int retentionDays, OnOldCountCallback callback) {
        executor.execute(() -> {
            long thresholdMs = DateUtils.calculateCutoffMs(System.currentTimeMillis(), retentionDays);
            int count = workoutSetDao.countOlderThanSync(thresholdMs);
            mainHandler.post(() -> callback.onCountLoaded(count));
        });
    }

    /**
     * Ручной запуск очистки из диалога настроек в фоновом потоке.
     */
    public void runCleanupNow(int retentionDays, OnCleanupDoneCallback callback) {
        executor.execute(() -> {
            CleanupWorker.CleanupResult result = CleanupWorker.executeCleanupSync(
                    appContext,
                    database,
                    retentionDays
            );
            if (callback != null) {
                mainHandler.post(() -> callback.onDone(result));
            }
        });
    }
}

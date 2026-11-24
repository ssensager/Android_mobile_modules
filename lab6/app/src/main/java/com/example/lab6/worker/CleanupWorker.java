package com.example.lab6.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.lab6.data.db.AppDatabase;
import com.example.lab6.util.CleanupPrefs;
import com.example.lab6.util.DateUtils;
import com.example.lab6.util.NotificationHelper;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Фоновый воркер периодической автоочистки устаревших подходов и связанных фото.
 * Запускается раз в сутки через WorkManager и уведомляет пользователя об удалённых данных.
 */
public class CleanupWorker extends Worker {

    public static final String WORK_NAME = "workout_cleanup";

    public static class CleanupResult {
        public final int deletedSets;
        public final int deletedFiles;

        public CleanupResult(int deletedSets, int deletedFiles) {
            this.deletedSets = deletedSets;
            this.deletedFiles = deletedFiles;
        }
    }

    public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        int retentionDays = CleanupPrefs.getRetentionDays(context);
        AppDatabase db = AppDatabase.getInstance(context);
        executeCleanupSync(context, db, retentionDays);
        return Result.success();
    }

    /**
     * Ставлю ежедневную уникальную задачу в очередь WorkManager с политикой KEEP,
     * чтобы не плодить дубликаты при каждом открытии приложения.
     */
    public static void schedulePeriodicCleanup(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CleanupWorker.class,
                1,
                TimeUnit.DAYS
        ).build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    /**
     * Единая логика очистки для фонового расписания и ручного запуска из настроек:
     * удаляю старые строки SQLite и подчищаю связанные файлы фото на диске.
     */
    public static CleanupResult executeCleanupSync(Context context, AppDatabase db, int retentionDays) {
        long now = System.currentTimeMillis();
        long thresholdMs = DateUtils.calculateCutoffMs(now, retentionDays);

        int deletedSets = db.workoutSetDao().deleteOlderThan(thresholdMs);
        int deletedFiles = deleteLinkedAndOrphanedPhotos(context, db, thresholdMs);

        CleanupPrefs.saveCleanupRun(context, now, deletedSets);

        if (deletedSets > 0 || deletedFiles > 0) {
            NotificationHelper.showCleanupNotification(context, deletedSets, deletedFiles, retentionDays);
        }
        return new CleanupResult(deletedSets, deletedFiles);
    }

    private static int deleteLinkedAndOrphanedPhotos(Context context, AppDatabase db, long thresholdMs) {
        int removed = 0;

        // Удаляю файлы фото у тренажёров, чьи подходы полностью ушли после очистки
        List<String> expiredPaths = db.equipmentDao().getExpiredUnusedPhotoPathsSync(thresholdMs);
        if (expiredPaths != null) {
            for (String path : expiredPaths) {
                if (path != null && !path.isEmpty()) {
                    File photo = new File(path);
                    if (photo.exists() && photo.delete()) {
                        removed++;
                    }
                }
            }
            if (!expiredPaths.isEmpty()) {
                db.equipmentDao().clearExpiredUnusedPhotoPathsSync(thresholdMs);
            }
        }

        // Проверяю каталог фото приложения на старые «осиротевшие» файлы
        File photosDir = new File(context.getFilesDir(), "workout_photos");
        if (photosDir.exists() && photosDir.isDirectory()) {
            File[] files = photosDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.lastModified() < thresholdMs && file.delete()) {
                        removed++;
                    }
                }
            }
        }
        return removed;
    }
}

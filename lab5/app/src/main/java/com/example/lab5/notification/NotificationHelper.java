package com.example.lab5.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.lab5.R;
import com.example.lab5.ui.MedicineDetailActivity;

import java.util.concurrent.TimeUnit;

// Собрал всю работу с каналом уведомлений, показом и постановкой задач в WorkManager в одном помощнике.
public final class NotificationHelper {

    public static final String CHANNEL_ID = "medcontrol_reminders";

    public static final String KEY_MEDICINE_ID = "medicine_id";
    public static final String KEY_INTAKE_ID = "intake_id";
    public static final String KEY_MEDICINE_NAME = "medicine_name";
    public static final String KEY_DOSAGE = "dosage";
    public static final String KEY_SUBSTANCE_NAME = "substance_name";

    public static final String EXTRA_INTAKE_ID = "extra_intake_id";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    private NotificationHelper() {
    }

    // Создаю канал один раз при запуске; повторный вызов безопасен и не сбрасывает настройки пользователя.
    public static void createChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static boolean hasNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Планирую напоминание через WorkManager, чтобы оно сработало даже после закрытия приложения или перезагрузки.
    public static void scheduleReminder(
            @NonNull Context context,
            int medicineId,
            int intakeId,
            @NonNull String medicineName,
            @NonNull String dosage,
            @NonNull String substanceName,
            long delaySeconds
    ) {
        long safeDelay = Math.max(1L, delaySeconds);
        Data inputData = new Data.Builder()
                .putInt(KEY_MEDICINE_ID, medicineId)
                .putInt(KEY_INTAKE_ID, intakeId)
                .putString(KEY_MEDICINE_NAME, medicineName)
                .putString(KEY_DOSAGE, dosage)
                .putString(KEY_SUBSTANCE_NAME, substanceName)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(safeDelay, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("medicine_reminder_" + medicineId)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
    }

    public static void showIntakeReminder(
            @NonNull Context context,
            int medicineId,
            int intakeId,
            @NonNull String medicineName,
            @NonNull String dosage,
            @NonNull String substanceName
    ) {
        createChannel(context);

        if (!hasNotificationPermission(context)) {
            return;
        }

        int notificationId = intakeId > 0 ? (10000 + intakeId) : (20000 + medicineId);

        // По нажатию открываю сразу карточку нужного препарата, а кнопка «Назад» вернёт на главный экран.
        Intent detailIntent = new Intent(context, MedicineDetailActivity.class);
        detailIntent.putExtra(MedicineDetailActivity.EXTRA_MEDICINE_ID, medicineId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(detailIntent);
        PendingIntent contentIntent = stackBuilder.getPendingIntent(
                notificationId,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = context.getString(R.string.notification_title_format, medicineName);
        String body = context.getString(R.string.notification_body_format, dosage, substanceName);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true);

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }

        // Добавляю кнопку «Принято», чтобы отметить приём в базе без открытия экрана.
        if (intakeId > 0) {
            Intent markTakenIntent = new Intent(context, IntakeActionReceiver.class);
            markTakenIntent.setAction(IntakeActionReceiver.ACTION_MARK_TAKEN);
            markTakenIntent.putExtra(EXTRA_INTAKE_ID, intakeId);
            markTakenIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 50000,
                    markTakenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.addAction(
                    R.drawable.ic_check_safe,
                    context.getString(R.string.action_mark_taken),
                    actionPendingIntent
            );
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Если пользователь отозвал разрешение прямо перед показом, не роняю приложение.
        }
    }
}

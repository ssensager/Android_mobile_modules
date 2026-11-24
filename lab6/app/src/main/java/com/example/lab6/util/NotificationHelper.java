package com.example.lab6.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.lab6.R;

/**
 * Помощник для отправки уведомления после выполнения автоочистки.
 * Всегда сообщаю пользователю, сколько старых записей и файлов было удалено.
 */
public final class NotificationHelper {

    private static final String CHANNEL_ID = "workout_cleanup_channel";
    private static final int NOTIFICATION_ID = 601;

    private NotificationHelper() {
    }

    public static void showCleanupNotification(Context context, int deletedSets, int deletedFiles, int retentionDays) {
        Context appContext = context.getApplicationContext();
        ensureChannel(appContext);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String text = appContext.getString(
                R.string.cleanup_notification_text,
                deletedSets,
                deletedFiles,
                retentionDays
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cleanup_notify)
                .setContentTitle(appContext.getString(R.string.cleanup_notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build());
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.cleanup_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(context.getString(R.string.cleanup_channel_desc));
            manager.createNotificationChannel(channel);
        }
    }
}

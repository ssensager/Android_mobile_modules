package com.example.lab5.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.example.lab5.data.AppDatabase;

// Обрабатываю нажатие «Принято» прямо из уведомления, не заставляя пользователя открывать приложение.
public class IntakeActionReceiver extends BroadcastReceiver {

    public static final String ACTION_MARK_TAKEN = "com.example.lab5.action.MARK_INTAKE_TAKEN";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_MARK_TAKEN.equals(intent.getAction())) {
            return;
        }

        int intakeId = intent.getIntExtra(NotificationHelper.EXTRA_INTAKE_ID, -1);
        int notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1);

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        if (intakeId <= 0) {
            return;
        }

        // Использую goAsync(), чтобы система не убила приёмник до завершения записи в Room.
        PendingResult pendingResult = goAsync();
        AppDatabase db = AppDatabase.getInstance(context);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                db.intakeDao().markTaken(intakeId, true);
            } finally {
                pendingResult.finish();
            }
        });
    }
}

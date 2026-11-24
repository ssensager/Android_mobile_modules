package com.example.lab5.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.lab5.data.AppDatabase;
import com.example.lab5.data.entity.Intake;
import com.example.lab5.data.entity.MedicineWithDetails;

// Фоновая задача WorkManager проверяет актуальность приёма в базе и показывает уведомление.
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int medicineId = getInputData().getInt(NotificationHelper.KEY_MEDICINE_ID, -1);
        int intakeId = getInputData().getInt(NotificationHelper.KEY_INTAKE_ID, -1);
        String medicineName = getInputData().getString(NotificationHelper.KEY_MEDICINE_NAME);
        String dosage = getInputData().getString(NotificationHelper.KEY_DOSAGE);
        String substanceName = getInputData().getString(NotificationHelper.KEY_SUBSTANCE_NAME);

        if (medicineId <= 0) {
            return Result.success();
        }

        // Сверюсь с Room прямо здесь (Worker уже выполняется в фоне): если препарат удалили или приём уже отмечен, молчу.
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        MedicineWithDetails details = db.medicineDao().getByIdWithDetailsSync(medicineId);
        if (details == null || details.medicine == null) {
            return Result.success();
        }

        if (intakeId > 0) {
            Intake intake = db.intakeDao().getByIdSync(intakeId);
            if (intake == null || intake.isTaken) {
                return Result.success();
            }
        }

        String actualName = details.medicine.name;
        String actualDosage = details.medicine.dosage;
        String actualSubstance = details.substance != null ? details.substance.name
                : (substanceName != null ? substanceName : "");

        NotificationHelper.showIntakeReminder(
                getApplicationContext(),
                medicineId,
                intakeId,
                actualName != null ? actualName : (medicineName != null ? medicineName : "Препарат"),
                actualDosage != null ? actualDosage : (dosage != null ? dosage : ""),
                actualSubstance
        );

        return Result.success();
    }
}

package com.example.lab5.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.lab5.data.dao.ActiveSubstanceDao;
import com.example.lab5.data.dao.ConflictRuleDao;
import com.example.lab5.data.dao.IntakeDao;
import com.example.lab5.data.dao.MedicineDao;
import com.example.lab5.data.entity.ActiveSubstance;
import com.example.lab5.data.entity.ConflictRule;
import com.example.lab5.data.entity.Intake;
import com.example.lab5.data.entity.Medicine;
import com.example.lab5.data.entity.MedicineWithDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

// Репозиторий скрывает детали работы с DAO и гарантирует, что любая запись уходит в фоновый поток.
public class MedicineRepository {

    public interface OnIdInsertedListener {
        void onInserted(int insertedId);
    }

    private final AppDatabase database;
    private final ActiveSubstanceDao substanceDao;
    private final MedicineDao medicineDao;
    private final ConflictRuleDao conflictRuleDao;
    private final IntakeDao intakeDao;
    private final ExecutorService executor;

    public MedicineRepository(@NonNull Context context) {
        this.database = AppDatabase.getInstance(context);
        this.substanceDao = database.activeSubstanceDao();
        this.medicineDao = database.medicineDao();
        this.conflictRuleDao = database.conflictRuleDao();
        this.intakeDao = database.intakeDao();
        this.executor = AppDatabase.databaseWriteExecutor;
    }

    public LiveData<List<MedicineWithDetails>> getAllMedicinesWithDetails() {
        return medicineDao.getAllWithDetails();
    }

    public LiveData<MedicineWithDetails> getMedicineWithDetails(int medicineId) {
        return medicineDao.getByIdWithDetails(medicineId);
    }

    public LiveData<List<ActiveSubstance>> getAllSubstances() {
        return substanceDao.getAll();
    }

    public LiveData<List<ConflictRule>> getAllConflictRules() {
        return conflictRuleDao.getAll();
    }

    public void insertMedicine(@NonNull Medicine medicine, @Nullable OnIdInsertedListener listener) {
        executor.execute(() -> {
            long id = medicineDao.insert(medicine);
            if (listener != null) {
                listener.onInserted((int) id);
            }
        });
    }

    public void updateMedicine(@NonNull Medicine medicine) {
        executor.execute(() -> medicineDao.update(medicine));
    }

    public void deleteMedicine(@NonNull Medicine medicine) {
        executor.execute(() -> medicineDao.delete(medicine));
    }

    // Сохраняю копию удалённого препарата и его приёмов, чтобы кнопка «Отменить» вернула всё целиком.
    public void restoreMedicineWithIntakes(@NonNull Medicine deletedMedicine, @Nullable List<Intake> deletedIntakes) {
        executor.execute(() -> database.runInTransaction(() -> {
            Medicine restored = new Medicine(
                    deletedMedicine.name,
                    deletedMedicine.dosage,
                    deletedMedicine.substanceId,
                    deletedMedicine.scheduleTime,
                    deletedMedicine.notes
            );
            int newMedicineId = (int) medicineDao.insert(restored);
            if (deletedIntakes != null && !deletedIntakes.isEmpty()) {
                List<Intake> restoredIntakes = new ArrayList<>();
                for (Intake oldIntake : deletedIntakes) {
                    restoredIntakes.add(new Intake(newMedicineId, oldIntake.scheduledAtMs, oldIntake.isTaken));
                }
                intakeDao.insertAll(restoredIntakes);
            }
        }));
    }

    public void insertIntake(@NonNull Intake intake, @Nullable OnIdInsertedListener listener) {
        executor.execute(() -> {
            long id = intakeDao.insert(intake);
            if (listener != null) {
                listener.onInserted((int) id);
            }
        });
    }

    public void markIntakeTaken(int intakeId, boolean taken) {
        executor.execute(() -> intakeDao.markTaken(intakeId, taken));
    }

    public void deleteIntake(@NonNull Intake intake) {
        executor.execute(() -> intakeDao.delete(intake));
    }
}

package com.example.lab5.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.lab5.data.MedicineRepository;
import com.example.lab5.data.entity.ActiveSubstance;
import com.example.lab5.data.entity.ConflictRule;
import com.example.lab5.data.entity.Intake;
import com.example.lab5.data.entity.Medicine;
import com.example.lab5.data.entity.MedicineWithDetails;
import com.example.lab5.domain.InteractionChecker;
import com.example.lab5.domain.RiskSummary;

import java.util.List;

// Связываю данные из репозитория и доменную проверку совместимости через реактивный MediatorLiveData.
public class MainViewModel extends AndroidViewModel {

    private final MedicineRepository repository;
    private final InteractionChecker interactionChecker;

    private final LiveData<List<MedicineWithDetails>> medicinesWithDetails;
    private final LiveData<List<ActiveSubstance>> substances;
    private final LiveData<List<ConflictRule>> conflictRules;
    private final MediatorLiveData<RiskSummary> riskSummary = new MediatorLiveData<>();

    private List<MedicineWithDetails> latestMedicines;
    private List<ConflictRule> latestRules;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new MedicineRepository(application);
        interactionChecker = new InteractionChecker();

        medicinesWithDetails = repository.getAllMedicinesWithDetails();
        substances = repository.getAllSubstances();
        conflictRules = repository.getAllConflictRules();

        riskSummary.setValue(RiskSummary.empty());

        riskSummary.addSource(medicinesWithDetails, list -> {
            latestMedicines = list;
            recalculateRisk();
        });
        riskSummary.addSource(conflictRules, rules -> {
            latestRules = rules;
            recalculateRisk();
        });
    }

    private void recalculateRisk() {
        riskSummary.setValue(interactionChecker.evaluate(latestMedicines, latestRules));
    }

    public LiveData<List<MedicineWithDetails>> getMedicinesWithDetails() {
        return medicinesWithDetails;
    }

    public LiveData<MedicineWithDetails> getMedicineWithDetails(int medicineId) {
        return repository.getMedicineWithDetails(medicineId);
    }

    public LiveData<List<ActiveSubstance>> getSubstances() {
        return substances;
    }

    public LiveData<List<ConflictRule>> getConflictRules() {
        return conflictRules;
    }

    public LiveData<RiskSummary> getRiskSummary() {
        return riskSummary;
    }

    public InteractionChecker getInteractionChecker() {
        return interactionChecker;
    }

    public void insertMedicine(@NonNull Medicine medicine, @Nullable MedicineRepository.OnIdInsertedListener listener) {
        repository.insertMedicine(medicine, listener);
    }

    public void updateMedicine(@NonNull Medicine medicine) {
        repository.updateMedicine(medicine);
    }

    public void deleteMedicine(@NonNull Medicine medicine) {
        repository.deleteMedicine(medicine);
    }

    public void restoreMedicineWithIntakes(@NonNull Medicine medicine, @Nullable List<Intake> intakes) {
        repository.restoreMedicineWithIntakes(medicine, intakes);
    }

    public void insertIntake(@NonNull Intake intake, @Nullable MedicineRepository.OnIdInsertedListener listener) {
        repository.insertIntake(intake, listener);
    }

    public void markIntakeTaken(int intakeId, boolean taken) {
        repository.markIntakeTaken(intakeId, taken);
    }

    public void deleteIntake(@NonNull Intake intake) {
        repository.deleteIntake(intake);
    }
}

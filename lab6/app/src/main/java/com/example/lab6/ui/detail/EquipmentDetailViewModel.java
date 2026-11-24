package com.example.lab6.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lab6.R;
import com.example.lab6.data.entity.Equipment;
import com.example.lab6.data.entity.WorkoutSet;
import com.example.lab6.data.repository.WorkoutRepository;
import com.example.lab6.util.DateUtils;

import java.util.List;

/**
 * ViewModel экрана деталей тренажёра.
 * Защищаю init от повторной инициализации при повороте экрана, чтобы не пересоздавать подписки LiveData.
 */
public class EquipmentDetailViewModel extends AndroidViewModel {

    private final WorkoutRepository repository;

    private int currentEquipmentId = -1;
    private LiveData<Equipment> equipment;
    private LiveData<List<WorkoutSet>> workoutSets;
    private final MutableLiveData<String> prMessage = new MutableLiveData<>(null);

    public EquipmentDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new WorkoutRepository(application);
    }

    public void init(int equipmentId) {
        if (currentEquipmentId == equipmentId) {
            return;
        }
        currentEquipmentId = equipmentId;
        equipment = repository.getEquipmentById(equipmentId);
        workoutSets = repository.getSetsForEquipment(equipmentId);
    }

    public LiveData<Equipment> getEquipment() {
        return equipment;
    }

    public LiveData<List<WorkoutSet>> getWorkoutSets() {
        return workoutSets;
    }

    public LiveData<String> getPrMessage() {
        return prMessage;
    }

    public void addSet(float weightKg, int reps) {
        if (currentEquipmentId == -1) {
            return;
        }
        Equipment current = equipment != null ? equipment.getValue() : null;
        String eqName = current != null ? current.name : "";

        repository.addSetWithPrCheck(currentEquipmentId, weightKg, reps, (isNewRecord, improvementKg) -> {
            if (isNewRecord) {
                Application app = getApplication();
                String weightText = DateUtils.formatNumber(weightKg);
                if (improvementKg > 0f) {
                    prMessage.setValue(app.getString(
                            R.string.pr_banner_with_delta,
                            eqName,
                            weightText,
                            reps,
                            DateUtils.formatNumber(improvementKg)
                    ));
                } else {
                    prMessage.setValue(app.getString(
                            R.string.pr_banner_first,
                            eqName,
                            weightText,
                            reps
                    ));
                }
            }
        });
    }

    public void deleteSet(WorkoutSet set) {
        repository.deleteSet(set);
    }

    public void deleteEquipment(Runnable onDone) {
        Equipment current = equipment != null ? equipment.getValue() : null;
        if (current != null) {
            repository.deleteEquipment(current, onDone);
        }
    }
}

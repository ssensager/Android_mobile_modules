package com.example.lab6.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lab6.R;
import com.example.lab6.data.projection.DailyVolumePoint;
import com.example.lab6.data.projection.EquipmentWithMaxSet;
import com.example.lab6.data.projection.WorkoutSetWithEquipmentName;
import com.example.lab6.data.repository.WorkoutRepository;
import com.example.lab6.util.DateUtils;

import java.util.List;

/**
 * ViewModel главного экрана: держит реактивные потоки сводки, графика и отфильтрованного списка.
 * Переключаю источник списка через MediatorLiveData при изменении поиска или выбранной группы мышц.
 */
public class MainViewModel extends AndroidViewModel {

    private final WorkoutRepository repository;

    private final LiveData<Integer> todaySetsCount;
    private final LiveData<Float> todayVolumeKg;
    private final LiveData<WorkoutSetWithEquipmentName> bestOverallRecord;
    private final LiveData<List<DailyVolumePoint>> weeklyVolume;

    private final MediatorLiveData<List<EquipmentWithMaxSet>> equipmentList = new MediatorLiveData<>();
    private final MutableLiveData<String> prBannerMessage = new MutableLiveData<>(null);

    private LiveData<List<EquipmentWithMaxSet>> currentListSource;
    private String currentQuery = "";
    private String currentMuscleGroup = "";

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new WorkoutRepository(application);
        todaySetsCount = repository.getTodaySetsCount();
        todayVolumeKg = repository.getTodayVolumeKg();
        bestOverallRecord = repository.getBestOverallRecord();
        weeklyVolume = repository.getLast7DaysVolume();
        reloadEquipmentSource();
    }

    public LiveData<List<EquipmentWithMaxSet>> getEquipmentList() {
        return equipmentList;
    }

    public LiveData<Integer> getTodaySetsCount() {
        return todaySetsCount;
    }

    public LiveData<Float> getTodayVolumeKg() {
        return todayVolumeKg;
    }

    public LiveData<WorkoutSetWithEquipmentName> getBestOverallRecord() {
        return bestOverallRecord;
    }

    public LiveData<List<DailyVolumePoint>> getWeeklyVolume() {
        return weeklyVolume;
    }

    public LiveData<String> getPrBannerMessage() {
        return prBannerMessage;
    }

    public String getCurrentQuery() {
        return currentQuery;
    }

    public String getCurrentMuscleGroup() {
        return currentMuscleGroup;
    }

    public boolean hasActiveFilters() {
        return !currentQuery.isEmpty() || !currentMuscleGroup.isEmpty();
    }

    public void setSearchQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.equals(currentQuery)) {
            return;
        }
        currentQuery = normalized;
        reloadEquipmentSource();
    }

    public void setMuscleGroup(String muscleGroup) {
        String normalized = muscleGroup == null ? "" : muscleGroup.trim();
        if (normalized.equals(currentMuscleGroup)) {
            return;
        }
        currentMuscleGroup = normalized;
        reloadEquipmentSource();
    }

    public void resetFilters() {
        if (currentQuery.isEmpty() && currentMuscleGroup.isEmpty()) {
            return;
        }
        currentQuery = "";
        currentMuscleGroup = "";
        reloadEquipmentSource();
    }

    private void reloadEquipmentSource() {
        if (currentListSource != null) {
            equipmentList.removeSource(currentListSource);
        }
        currentListSource = repository.searchEquipmentWithStats(currentQuery, currentMuscleGroup);
        equipmentList.addSource(currentListSource, equipmentList::setValue);
    }

    public void addEquipment(String name, String muscleGroup, String notes) {
        repository.addEquipment(name, muscleGroup, notes);
    }

    /**
     * Быстрое добавление подхода с проверкой нового личного рекорда.
     */
    public void addQuickSet(int equipmentId, String equipmentName, float weightKg, int reps) {
        repository.addSetWithPrCheck(equipmentId, weightKg, reps, (isNewRecord, improvementKg) -> {
            if (isNewRecord) {
                Application app = getApplication();
                String formattedWeight = DateUtils.formatNumber(weightKg);
                String text;
                if (improvementKg > 0f) {
                    text = app.getString(
                            R.string.pr_banner_with_delta,
                            equipmentName,
                            formattedWeight,
                            reps,
                            DateUtils.formatNumber(improvementKg)
                    );
                } else {
                    text = app.getString(
                            R.string.pr_banner_first,
                            equipmentName,
                            formattedWeight,
                            reps
                    );
                }
                prBannerMessage.setValue(text);
            }
        });
    }

    public void dismissPrBanner() {
        prBannerMessage.setValue(null);
    }

    public void exportCsv(WorkoutRepository.OnCsvExportCallback callback) {
        repository.exportHistoryToCsv(callback);
    }

    public void countOldSets(int retentionDays, WorkoutRepository.OnOldCountCallback callback) {
        repository.countOldSets(retentionDays, callback);
    }

    public void runManualCleanup(int retentionDays, WorkoutRepository.OnCleanupDoneCallback callback) {
        repository.runCleanupNow(retentionDays, callback);
    }
}

package com.example.lab1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Держу здесь всё состояние поездки и расчёт цены, чтобы при повороте экрана ничего не сбрасывалось.
 * Ограничения счётчика пассажиров и пересчёт стоимости тоже делаю здесь, а Activity только рисует UI.
 */
public class MainViewModel extends ViewModel {

    private final MutableLiveData<TaxiOrderState> stateLiveData =
            new MutableLiveData<>(TaxiOrderState.initial());
    private final MutableLiveData<PriceBreakdown> breakdownLiveData = new MutableLiveData<>();

    private int nextOrderNumber = 1041;

    public MainViewModel() {
        recalculate(TaxiOrderState.initial());
    }

    public LiveData<TaxiOrderState> getState() {
        return stateLiveData;
    }

    public LiveData<PriceBreakdown> getBreakdown() {
        return breakdownLiveData;
    }

    public TaxiOrderState getCurrentState() {
        TaxiOrderState current = stateLiveData.getValue();
        return current != null ? current : TaxiOrderState.initial();
    }

    public PriceBreakdown getCurrentBreakdown() {
        PriceBreakdown current = breakdownLiveData.getValue();
        return current != null ? current : calculateBreakdown(getCurrentState());
    }

    public void selectCarClass(int index) {
        if (index < 0 || index >= TaxiTariffs.CAR_CLASSES.length) return;
        TaxiOrderState current = getCurrentState();
        if (current.carClassIndex == index) return;

        // Если было выбрано 6 человек в Минивэне, а потом переключил на Эконом (макс 4),
        // сразу подрезаю число пассажиров до допустимого максимума
        int maxAllowed = TaxiTariffs.CAR_CLASSES[index].maxPassengers;
        int clampedPassengers = Math.min(current.passengers, maxAllowed);

        updateState(current.withCarClass(index, clampedPassengers));
    }

    public void selectZone(int index) {
        if (index < 0 || index >= TaxiTariffs.TARIFF_ZONES.length) return;
        TaxiOrderState current = getCurrentState();
        if (current.zoneIndex == index) return;
        updateState(current.withZone(index));
    }

    public void selectDemand(int index) {
        if (index < 0 || index >= TaxiTariffs.DEMAND_LEVELS.length) return;
        TaxiOrderState current = getCurrentState();
        if (current.demandIndex == index) return;
        updateState(current.withDemand(index));
    }

    public void setChildSeat(boolean enabled) {
        TaxiOrderState current = getCurrentState();
        if (current.hasChildSeat == enabled) return;
        updateState(current.withOptions(enabled, current.hasLuggage, current.hasWaiting));
    }

    public void setLuggage(boolean enabled) {
        TaxiOrderState current = getCurrentState();
        if (current.hasLuggage == enabled) return;
        updateState(current.withOptions(current.hasChildSeat, enabled, current.hasWaiting));
    }

    public void setWaiting(boolean enabled) {
        TaxiOrderState current = getCurrentState();
        if (current.hasWaiting == enabled) return;
        updateState(current.withOptions(current.hasChildSeat, current.hasLuggage, enabled));
    }

    // Проверяю границы числа пассажиров здесь (от 1 до вместимости машины), а не в разметке
    public void incrementPassengers() {
        TaxiOrderState current = getCurrentState();
        int maxAllowed = TaxiTariffs.CAR_CLASSES[current.carClassIndex].maxPassengers;
        if (current.passengers < maxAllowed) {
            updateState(current.withPassengers(current.passengers + 1));
        }
    }

    public void decrementPassengers() {
        TaxiOrderState current = getCurrentState();
        if (current.passengers > 1) {
            updateState(current.withPassengers(current.passengers - 1));
        }
    }

    // Повторное нажатие на тот же сегмент шкалы снимает выделение
    public void toggleSegmentSelection(int segmentIndex) {
        TaxiOrderState current = getCurrentState();
        int nextSelection = (current.selectedSegmentIndex == segmentIndex) ? -1 : segmentIndex;
        stateLiveData.setValue(current.withSelectedSegment(nextSelection));
    }

    public int generateNextOrderId() {
        nextOrderNumber++;
        return nextOrderNumber;
    }

    public void onOrderConfirmed(int orderId) {
        TaxiOrderState current = getCurrentState();
        stateLiveData.setValue(current.withLastOrderId(orderId));
    }

    public void cancelLastOrder() {
        TaxiOrderState current = getCurrentState();
        stateLiveData.setValue(current.withLastOrderId(0));
    }

    private void updateState(TaxiOrderState newState) {
        stateLiveData.setValue(newState);
        recalculate(newState);
    }

    private void recalculate(TaxiOrderState state) {
        breakdownLiveData.setValue(calculateBreakdown(state));
    }

    // Считаю итоговую стоимость в одном статическом методе, чтобы логику было легко тестировать
    // и при любом изменении параметров цена пересчитывалась одинаково
    public static PriceBreakdown calculateBreakdown(TaxiOrderState s) {
        TaxiTariffs.CarClass car = TaxiTariffs.CAR_CLASSES[s.carClassIndex];
        TaxiTariffs.TariffZone zone = TaxiTariffs.TARIFF_ZONES[s.zoneIndex];
        TaxiTariffs.DemandLevel demand = TaxiTariffs.DEMAND_LEVELS[s.demandIndex];

        int rawOptions = 0;
        if (s.hasChildSeat) rawOptions += TaxiTariffs.PRICE_CHILD_SEAT;
        if (s.hasLuggage) rawOptions += TaxiTariffs.PRICE_LUGGAGE;
        if (s.hasWaiting) rawOptions += TaxiTariffs.PRICE_WAITING;
        if (s.passengers > 1) {
            rawOptions += (s.passengers - 1) * TaxiTariffs.PRICE_EXTRA_PASSENGER;
        }

        int baseCost = Math.round(car.baseFare * demand.multiplier);
        int kmCost = Math.round(zone.distanceKm * car.pricePerKm * demand.multiplier);
        int minCost = Math.round(zone.durationMin * car.pricePerMin * demand.multiplier);
        int optionsCost = Math.round(rawOptions * demand.multiplier);

        return new PriceBreakdown(baseCost, kmCost, minCost, optionsCost);
    }
}

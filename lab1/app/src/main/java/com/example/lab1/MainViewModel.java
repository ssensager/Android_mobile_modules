package com.example.lab1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel — слой хранения состояния калькулятора такси.
 * Переживает поворот экрана и пересоздание Activity, поэтому выбранные параметры
 * никогда не сбрасываются. Все расчёты цены и проверки границ (например, числа пассажиров)
 * сосредоточены здесь, а Activity только подписывается на LiveData и обновляет интерфейс.
 */
public class MainViewModel extends ViewModel {

    private final MutableLiveData<TaxiOrderState> stateLiveData =
            new MutableLiveData<>(TaxiOrderState.initial());
    private final MutableLiveData<PriceBreakdown> breakdownLiveData = new MutableLiveData<>();

    // Простой счётчик номеров заказов для демонстрации возврата результата со второго экрана
    private int nextOrderNumber = 1041;

    public MainViewModel() {
        // Сразу считаем начальную стоимость, чтобы при открытии экрана итог был актуален
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

        // Если раньше был выбран Минивэн (до 6 мест), а пользователь переключился на легковую (до 4),
        // аккуратно ограничиваем число пассажиров, чтобы не получилось невозможное состояние
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

    // Ограничение счётчика пассажиров (от 1 до maxPassengers выбранного авто) выполняется
    // строго внутри ViewModel, а не на уровне кнопок в разметке
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

    // Выделение сегмента на кастомной шкале цены (повторный тап снимает выделение)
    public void toggleSegmentSelection(int segmentIndex) {
        TaxiOrderState current = getCurrentState();
        int nextSelection = (current.selectedSegmentIndex == segmentIndex) ? -1 : segmentIndex;
        stateLiveData.setValue(current.withSelectedSegment(nextSelection));
    }

    // Генерируем номер нового заказа для второго экрана или планшетной панели
    public int generateNextOrderId() {
        nextOrderNumber++;
        return nextOrderNumber;
    }

    // Фиксируем подтверждённый заказ, который вернул второй экран через ActivityResultContracts
    public void onOrderConfirmed(int orderId) {
        TaxiOrderState current = getCurrentState();
        stateLiveData.setValue(current.withLastOrderId(orderId));
    }

    // Отмена последнего заказа (вызывается по нажатию кнопки «Отменить» в Snackbar)
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

    /**
     * Единственная точка пересчёта стоимости поездки.
     * Любое изменение класса, зоны, спроса, опций или числа пассажиров проходит сюда,
     * поэтому итоговую сумму невозможно «забыть» обновить.
     */
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

        // Умножаем составляющие на коэффициент спроса и округляем до целого рубля
        int baseCost = Math.round(car.baseFare * demand.multiplier);
        int kmCost = Math.round(zone.distanceKm * car.pricePerKm * demand.multiplier);
        int minCost = Math.round(zone.durationMin * car.pricePerMin * demand.multiplier);
        int optionsCost = Math.round(rawOptions * demand.multiplier);

        return new PriceBreakdown(baseCost, kmCost, minCost, optionsCost);
    }
}

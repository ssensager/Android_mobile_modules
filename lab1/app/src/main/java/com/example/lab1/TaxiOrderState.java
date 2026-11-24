package com.example.lab1;

/**
 * Собрал всё состояние экрана в один неизменяемый класс.
 * Сделал поля final и обновляю их через методы with..., чтобы при каждом клике
 * в LiveData попадал новый согласованный объект и ничего не рассинхронизировалось.
 */
public final class TaxiOrderState {

    public final int carClassIndex;
    public final int zoneIndex;
    public final int demandIndex;
    public final boolean hasChildSeat;
    public final boolean hasLuggage;
    public final boolean hasWaiting;
    public final int passengers;
    // Индекс выделенного сегмента на шкале (-1, если ничего не выделено)
    public final int selectedSegmentIndex;
    // Номер последнего подтверждённого заказа (0 — если ещё не оформлял или отменил)
    public final int lastOrderId;

    public TaxiOrderState(int carClassIndex,
                          int zoneIndex,
                          int demandIndex,
                          boolean hasChildSeat,
                          boolean hasLuggage,
                          boolean hasWaiting,
                          int passengers,
                          int selectedSegmentIndex,
                          int lastOrderId) {
        this.carClassIndex = carClassIndex;
        this.zoneIndex = zoneIndex;
        this.demandIndex = demandIndex;
        this.hasChildSeat = hasChildSeat;
        this.hasLuggage = hasLuggage;
        this.hasWaiting = hasWaiting;
        this.passengers = passengers;
        this.selectedSegmentIndex = selectedSegmentIndex;
        this.lastOrderId = lastOrderId;
    }

    // Стартовые настройки при запуске: Комфорт, Город, обычный спрос 1.0x, 1 пассажир
    public static TaxiOrderState initial() {
        return new TaxiOrderState(1, 1, 0, false, false, false, 1, -1, 0);
    }

    public TaxiOrderState withCarClass(int newCarClassIndex, int clampedPassengers) {
        return new TaxiOrderState(newCarClassIndex, zoneIndex, demandIndex,
                hasChildSeat, hasLuggage, hasWaiting, clampedPassengers,
                selectedSegmentIndex, lastOrderId);
    }

    public TaxiOrderState withZone(int newZoneIndex) {
        return new TaxiOrderState(carClassIndex, newZoneIndex, demandIndex,
                hasChildSeat, hasLuggage, hasWaiting, passengers,
                selectedSegmentIndex, lastOrderId);
    }

    public TaxiOrderState withDemand(int newDemandIndex) {
        return new TaxiOrderState(carClassIndex, zoneIndex, newDemandIndex,
                hasChildSeat, hasLuggage, hasWaiting, passengers,
                selectedSegmentIndex, lastOrderId);
    }

    public TaxiOrderState withOptions(boolean childSeat, boolean luggage, boolean waiting) {
        return new TaxiOrderState(carClassIndex, zoneIndex, demandIndex,
                childSeat, luggage, waiting, passengers,
                selectedSegmentIndex, lastOrderId);
    }

    public TaxiOrderState withPassengers(int newPassengers) {
        return new TaxiOrderState(carClassIndex, zoneIndex, demandIndex,
                hasChildSeat, hasLuggage, hasWaiting, newPassengers,
                selectedSegmentIndex, lastOrderId);
    }

    public TaxiOrderState withSelectedSegment(int segmentIndex) {
        return new TaxiOrderState(carClassIndex, zoneIndex, demandIndex,
                hasChildSeat, hasLuggage, hasWaiting, passengers,
                segmentIndex, lastOrderId);
    }

    public TaxiOrderState withLastOrderId(int orderId) {
        return new TaxiOrderState(carClassIndex, zoneIndex, demandIndex,
                hasChildSeat, hasLuggage, hasWaiting, passengers,
                selectedSegmentIndex, orderId);
    }
}

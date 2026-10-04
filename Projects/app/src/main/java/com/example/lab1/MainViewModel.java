package com.example.lab1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// держим тут все состояние чтобы оно не терялось при повороте
public class MainViewModel extends ViewModel {

    // выбранный размер пиццы
    private final MutableLiveData<Integer> selectedSize = new MutableLiveData<>(1);

    // нужен ли дополнительный сыр
    private final MutableLiveData<Boolean> extraCheese = new MutableLiveData<>(false);

    // количество пицц
    private final MutableLiveData<Integer> quantity = new MutableLiveData<>(1);

    // общая сумма заказа
    private final MutableLiveData<Integer> totalPrice = new MutableLiveData<>(300);

    public LiveData<Integer> getSelectedSize() {
        return selectedSize;
    }

    public LiveData<Boolean> getExtraCheese() {
        return extraCheese;
    }

    public LiveData<Integer> getQuantity() {
        return quantity;
    }

    public LiveData<Integer> getTotalPrice() {
        return totalPrice;
    }

    public void setSelectedSize(int size) {
        selectedSize.setValue(size);
        calculateTotal();
    }

    public void setExtraCheese(boolean hasCheese) {
        extraCheese.setValue(hasCheese);
        calculateTotal();
    }

    public void incrementQuantity() {
        int current = quantity.getValue() != null ? quantity.getValue() : 1;
        quantity.setValue(current + 1);
        calculateTotal();
    }

    // чтобы количество не уходило в ноль или минус
    public void decrementQuantity() {
        int current = quantity.getValue() != null ? quantity.getValue() : 1;
        if (current > 1) {
            quantity.setValue(current - 1);
            calculateTotal();
        }
    }

    // считаем итоговую цену заказа в одном месте
    private void calculateTotal() {
        int size = selectedSize.getValue() != null ? selectedSize.getValue() : 1;
        boolean cheese = extraCheese.getValue() != null && extraCheese.getValue();
        int qty = quantity.getValue() != null ? quantity.getValue() : 1;

        int basePrice = 300;
        if (size == 2) basePrice = 450;
        if (size == 3) basePrice = 600;

        if (cheese) {
            basePrice += 100;
        }

        int total = basePrice * qty;
        totalPrice.setValue(total);
    }
}

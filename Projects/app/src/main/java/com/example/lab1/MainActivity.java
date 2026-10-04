package com.example.lab1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

// главный экран приложения
public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private PizzaCustomView pizzaView;
    private RadioGroup rgSizes;
    private CheckBox cbCheese;
    private TextView tvQuantity;
    private TextView tvTotal;

    // запуск второго экрана с ожиданием результата
    private final ActivityResultLauncher<Intent> confirmLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String message = result.getData().getStringExtra("RESULT_MESSAGE");
                    if (message != null) {
                        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // получаем вьюмодель чтобы данные не стирались при повороте экрана
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // находим все элементы на экране
        pizzaView = findViewById(R.id.pizza_view);
        rgSizes = findViewById(R.id.rg_sizes);
        cbCheese = findViewById(R.id.cb_cheese);
        tvQuantity = findViewById(R.id.tv_quantity);
        tvTotal = findViewById(R.id.tv_total);
        Button btnMinus = findViewById(R.id.btn_minus);
        Button btnPlus = findViewById(R.id.btn_plus);
        Button btnOrder = findViewById(R.id.btn_order);

        // следим за выбором размера пиццы
        rgSizes.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_small) {
                viewModel.setSelectedSize(1);
            } else if (checkedId == R.id.rb_medium) {
                viewModel.setSelectedSize(2);
            } else if (checkedId == R.id.rb_large) {
                viewModel.setSelectedSize(3);
            }
        });

        // следим за галочкой сыра
        cbCheese.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setExtraCheese(isChecked);
        });

        // кнопки плюс и минус для количества
        btnPlus.setOnClickListener(v -> viewModel.incrementQuantity());
        btnMinus.setOnClickListener(v -> viewModel.decrementQuantity());

        // подписываемся на изменения во вьюмодели чтобы обновлять экран
        viewModel.getSelectedSize().observe(this, size -> {
            pizzaView.setProgressSize(size);
        });

        viewModel.getQuantity().observe(this, qty -> {
            tvQuantity.setText(String.valueOf(qty));
        });

        viewModel.getTotalPrice().observe(this, total -> {
            tvTotal.setText("Итого: " + total + " руб");
        });

        // собираем данные заказа и открываем второй экран
        btnOrder.setOnClickListener(v -> {
            String sizeStr = "Маленькая";
            Integer sizeVal = viewModel.getSelectedSize().getValue();
            if (sizeVal != null) {
                if (sizeVal == 2) sizeStr = "Средняя";
                if (sizeVal == 3) sizeStr = "Большая";
            }
            boolean cheeseVal = Boolean.TRUE.equals(viewModel.getExtraCheese().getValue());
            Integer totalVal = viewModel.getTotalPrice().getValue();
            Integer qtyVal = viewModel.getQuantity().getValue();

            String orderDetails = "Пицца: Пепперони\n" +
                    "Размер: " + sizeStr + "\n" +
                    "Доп. сыр: " + (cheeseVal ? "Да" : "Нет") + "\n" +
                    "Количество: " + qtyVal + "\n" +
                    "Сумма: " + totalVal + " руб";

            Intent intent = new Intent(MainActivity.this, ConfirmActivity.class);
            intent.putExtra("ORDER_INFO", orderDetails);
            confirmLauncher.launch(intent);
        });
    }
}

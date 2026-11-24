package com.example.lab1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

/**
 * Второй экран подтверждения вызова.
 * Забираю данные поездки из Intent и возвращаю обратно номер заказа через setResult().
 */
public class ConfirmActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_CAR_NAME = "extra_car_name";
    public static final String EXTRA_ZONE_TEXT = "extra_zone_text";
    public static final String EXTRA_DEMAND_TEXT = "extra_demand_text";
    public static final String EXTRA_PASSENGERS = "extra_passengers";
    public static final String EXTRA_OPTIONS_TEXT = "extra_options_text";
    public static final String EXTRA_BASE_COST = "extra_base_cost";
    public static final String EXTRA_KM_COST = "extra_km_cost";
    public static final String EXTRA_MIN_COST = "extra_min_cost";
    public static final String EXTRA_OPTIONS_COST = "extra_options_cost";
    public static final String EXTRA_TOTAL = "extra_total";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        View root = findViewById(R.id.confirm_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        Intent source = getIntent();
        int orderId = source.getIntExtra(EXTRA_ORDER_ID, 1001);
        String carName = source.getStringExtra(EXTRA_CAR_NAME);
        String zoneText = source.getStringExtra(EXTRA_ZONE_TEXT);
        String demandText = source.getStringExtra(EXTRA_DEMAND_TEXT);
        int passengers = source.getIntExtra(EXTRA_PASSENGERS, 1);
        String optionsText = source.getStringExtra(EXTRA_OPTIONS_TEXT);
        int baseCost = source.getIntExtra(EXTRA_BASE_COST, 0);
        int kmCost = source.getIntExtra(EXTRA_KM_COST, 0);
        int minCost = source.getIntExtra(EXTRA_MIN_COST, 0);
        int optionsCost = source.getIntExtra(EXTRA_OPTIONS_COST, 0);
        int total = source.getIntExtra(EXTRA_TOTAL, 0);

        ((TextView) findViewById(R.id.tv_receipt_car)).setText(carName != null ? carName : "");
        ((TextView) findViewById(R.id.tv_receipt_zone)).setText(zoneText != null ? zoneText : "");
        ((TextView) findViewById(R.id.tv_receipt_demand)).setText(demandText != null ? demandText : "");
        ((TextView) findViewById(R.id.tv_receipt_passengers)).setText(String.valueOf(passengers));
        ((TextView) findViewById(R.id.tv_receipt_options))
                .setText(optionsText != null ? optionsText : getString(R.string.options_none));

        ((TextView) findViewById(R.id.tv_receipt_base)).setText(getString(R.string.price_fmt, baseCost));
        ((TextView) findViewById(R.id.tv_receipt_km)).setText(getString(R.string.price_fmt, kmCost));
        ((TextView) findViewById(R.id.tv_receipt_min)).setText(getString(R.string.price_fmt, minCost));
        ((TextView) findViewById(R.id.tv_receipt_options_cost)).setText(getString(R.string.price_fmt, optionsCost));
        ((TextView) findViewById(R.id.tv_receipt_total)).setText(getString(R.string.price_fmt, total));

        MaterialButton btnConfirm = findViewById(R.id.btn_confirm_order);
        MaterialButton btnCancel = findViewById(R.id.btn_cancel_order);

        btnConfirm.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_ORDER_ID, orderId);
            resultIntent.putExtra(EXTRA_TOTAL, total);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}

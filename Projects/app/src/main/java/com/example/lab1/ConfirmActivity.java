package com.example.lab1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// второй экран подтверждения заказа
public class ConfirmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        TextView tvDetails = findViewById(R.id.tv_order_details);
        Button btnConfirm = findViewById(R.id.btn_confirm_order);
        Button btnCancel = findViewById(R.id.btn_cancel_order);

        // получаем переданный текст заказа с первого экрана
        String orderInfo = getIntent().getStringExtra("ORDER_INFO");
        if (orderInfo != null) {
            tvDetails.setText(orderInfo);
        }

        // если подтверждаем то возвращаем успех
        btnConfirm.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("RESULT_MESSAGE", "Заказ успешно оформлен!");
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // если отменяем то просто закрываем экран
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}

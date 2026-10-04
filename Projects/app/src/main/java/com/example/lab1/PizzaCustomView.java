package com.example.lab1;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// свой компонент пиццы для лабы
public class PizzaCustomView extends View {

    private final Paint paintCircle;
    private final Paint paintText;
    private final String pizzaTitle;
    private int progressSize = 1;

    public PizzaCustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        int pizzaColor = Color.RED;
        String titleTemp = "Пицца";

        // читаем настройки из xml если они там есть
        if (attrs != null) {
            try (TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PizzaCustomView, 0, 0)) {
                pizzaColor = a.getColor(R.styleable.PizzaCustomView_pizzaColor, Color.RED);
                String t = a.getString(R.styleable.PizzaCustomView_pizzaTitle);
                if (t != null) {
                    titleTemp = t;
                }
            }
        }
        pizzaTitle = titleTemp;

        // кисти создаем один раз тут чтоб не тормозило в ondraw
        paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircle.setColor(pizzaColor);
        paintCircle.setStyle(Paint.Style.FILL);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(36f);
        paintText.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgressSize(int size) {
        this.progressSize = size;
        invalidate(); // обновляем экран чтобы перерисовалось
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // считаем размеры вьюхи
        int desiredWidth = 400;
        int desiredHeight = 350;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = (widthMode == MeasureSpec.EXACTLY) ? widthSize :
                (widthMode == MeasureSpec.AT_MOST) ? Math.min(desiredWidth, widthSize) : desiredWidth;

        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize :
                (heightMode == MeasureSpec.AT_MOST) ? Math.min(desiredHeight, heightSize) : desiredHeight;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        // делаем пиццу больше или меньше когда выбирают размер
        float maxPossibleRadius = Math.min(cx, cy) - 15f;
        float radius = 70f + (progressSize * 25f);
        if (radius > maxPossibleRadius) {
            radius = maxPossibleRadius;
        }

        // рисуем кругляш пиццы
        canvas.drawCircle(cx, cy, radius, paintCircle);

        // подгоняем размер текста под размер пиццы
        float textSize = 26f + (progressSize * 4f);
        paintText.setTextSize(textSize);

        String titleText = pizzaTitle;
        String sizeText = "Размер: " + progressSize;

        // проверяем чтобы текст не вылазил за края
        float maxAllowedWidth = radius * 1.6f;
        while (paintText.measureText(titleText) > maxAllowedWidth && textSize > 14f) {
            textSize -= 2f;
            paintText.setTextSize(textSize);
        }

        Paint.FontMetrics fm = paintText.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;

        // маленькую пиццу пишем в одну строку, а большую в две
        if (progressSize == 1) {
            float textY = cy - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(pizzaTitle + " (" + progressSize + ")", cx, textY, paintText);
        } else {
            canvas.drawText(titleText, cx, cy - textHeight * 0.35f, paintText);

            paintText.setTextSize(textSize * 0.75f);
            canvas.drawText(sizeText, cx, cy + textHeight * 0.75f, paintText);

            paintText.setTextSize(textSize);
        }
    }
}

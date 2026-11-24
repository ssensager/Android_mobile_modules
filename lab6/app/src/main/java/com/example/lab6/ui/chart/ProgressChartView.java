package com.example.lab6.ui.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.lab6.R;
import com.example.lab6.data.projection.DailyVolumePoint;
import com.example.lab6.util.DateUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Собственный график столбцов динамики тренировочного объёма за последние 7 дней.
 * Все кисти, прямоугольники и массивы подписей создаю заранее, чтобы в onDraw не было аллокаций.
 */
public class ProgressChartView extends View {

    private static final int DAYS_COUNT = 7;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyStatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF barRect = new RectF();
    private final float[] volumes = new float[DAYS_COUNT];
    private final String[] dayLabels = new String[DAYS_COUNT];
    private final String[] valueLabels = new String[DAYS_COUNT];

    private float maxVolume = 0f;
    private boolean hasAnyData = false;

    private float cornerRadiusPx;
    private float topPaddingPx;
    private float bottomLabelSpacePx;
    private float sidePaddingPx;
    private float minBarHeightPx;
    private float valueTextOffsetPx;
    private float labelBottomMarginPx;
    private String emptyMessage;

    public ProgressChartView(Context context) {
        super(context);
        init(context);
    }

    public ProgressChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProgressChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        cornerRadiusPx = dp(6f);
        topPaddingPx = dp(24f);
        bottomLabelSpacePx = dp(24f);
        sidePaddingPx = dp(8f);
        minBarHeightPx = dp(4f);
        valueTextOffsetPx = dp(6f);
        labelBottomMarginPx = dp(6f);
        emptyMessage = context.getString(R.string.chart_empty_message);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.chart_grid));
        gridPaint.setStrokeWidth(dp(1f));

        barPaint.setColor(ContextCompat.getColor(context, R.color.chart_bar_regular));
        barPaint.setStyle(Paint.Style.FILL);

        todayBarPaint.setColor(ContextCompat.getColor(context, R.color.chart_bar_today));
        todayBarPaint.setStyle(Paint.Style.FILL);

        emptyBarPaint.setColor(ContextCompat.getColor(context, R.color.chart_bar_empty));
        emptyBarPaint.setStyle(Paint.Style.FILL);

        valueTextPaint.setColor(ContextCompat.getColor(context, R.color.chart_value_text));
        valueTextPaint.setTextSize(sp(10f));
        valueTextPaint.setTextAlign(Paint.Align.CENTER);
        valueTextPaint.setFakeBoldText(true);

        labelTextPaint.setColor(ContextCompat.getColor(context, R.color.chart_axis_text));
        labelTextPaint.setTextSize(sp(10.5f));
        labelTextPaint.setTextAlign(Paint.Align.CENTER);

        emptyStatePaint.setColor(ContextCompat.getColor(context, R.color.chart_axis_text));
        emptyStatePaint.setTextSize(sp(13f));
        emptyStatePaint.setTextAlign(Paint.Align.CENTER);

        setData(null);
    }

    /**
     * Обновляю данные за последние 7 дней и заранее форматирую все текстовые подписи вне onDraw.
     */
    public void setData(@Nullable List<DailyVolumePoint> points) {
        List<String> keys = DateUtils.buildLast7DayKeys(System.currentTimeMillis());
        Map<String, Float> volumeByDay = new HashMap<>();
        if (points != null) {
            for (DailyVolumePoint p : points) {
                if (p != null && p.dayKey != null) {
                    volumeByDay.put(p.dayKey, p.totalVolume);
                }
            }
        }

        maxVolume = 0f;
        hasAnyData = false;

        for (int i = 0; i < DAYS_COUNT; i++) {
            String key = keys.get(i);
            Float vol = volumeByDay.get(key);
            float safeVol = vol != null ? Math.max(0f, vol) : 0f;
            volumes[i] = safeVol;
            dayLabels[i] = DateUtils.formatDayShort(key);
            valueLabels[i] = safeVol > 0f ? DateUtils.formatCompactVolume(safeVol) : "";
            if (safeVol > 0f) {
                hasAnyData = true;
            }
            if (safeVol > maxVolume) {
                maxVolume = safeVol;
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (!hasAnyData) {
            canvas.drawText(emptyMessage, width * 0.5f, height * 0.5f, emptyStatePaint);
            return;
        }

        float chartLeft = sidePaddingPx;
        float chartRight = width - sidePaddingPx;
        float chartTop = topPaddingPx;
        float chartBottom = height - bottomLabelSpacePx;
        float chartHeight = Math.max(1f, chartBottom - chartTop);

        // Рисую три мягкие горизонтальные линии сетки для визуального масштаба
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, gridPaint);
        canvas.drawLine(chartLeft, chartTop + chartHeight * 0.5f, chartRight, chartTop + chartHeight * 0.5f, gridPaint);
        canvas.drawLine(chartLeft, chartTop, chartRight, chartTop, gridPaint);

        float slotWidth = (chartRight - chartLeft) / DAYS_COUNT;
        float barWidth = slotWidth * 0.54f;
        float labelY = height - labelBottomMarginPx;

        for (int i = 0; i < DAYS_COUNT; i++) {
            float centerX = chartLeft + slotWidth * i + slotWidth * 0.5f;
            float left = centerX - barWidth * 0.5f;
            float right = centerX + barWidth * 0.5f;

            float volume = volumes[i];
            Paint activePaint;
            float top;
            if (volume > 0f && maxVolume > 0f) {
                float ratio = volume / maxVolume;
                float barH = Math.max(minBarHeightPx, chartHeight * ratio);
                top = chartBottom - barH;
                activePaint = (i == DAYS_COUNT - 1) ? todayBarPaint : barPaint;
            } else {
                top = chartBottom - minBarHeightPx;
                activePaint = emptyBarPaint;
            }

            barRect.set(left, top, right, chartBottom);
            canvas.drawRoundRect(barRect, cornerRadiusPx, cornerRadiusPx, activePaint);

            if (!valueLabels[i].isEmpty()) {
                canvas.drawText(valueLabels[i], centerX, top - valueTextOffsetPx, valueTextPaint);
            }
            canvas.drawText(dayLabels[i], centerX, labelY, labelTextPaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}

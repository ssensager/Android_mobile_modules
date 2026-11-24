package com.example.lab5.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.lab5.R;
import com.example.lab5.domain.RiskSummary;

// Нарисовал шкалу риска взаимодействий прямо на Canvas без выделения памяти в onDraw.
public class RiskGaugeView extends View {

    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zoneLowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zoneMediumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zoneHighPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF barRect = new RectF();
    private final RectF segRect = new RectF();

    private float paddingH;
    private float paddingV;
    private float barHeight;
    private float barCorner;
    private float markerRadius;
    private float dotRadius;
    private float legendGap;
    private float desiredHeightPx;

    private int colorSafe;
    private int colorLow;
    private int colorMedium;
    private int colorHigh;
    private int colorCard;

    private int riskScore = 0;
    private int activeBadgeColor;
    private String titleText = "Индекс риска взаимодействий";
    private String badgeText = "Безопасно · 0/100";
    private String highLegendText = "Критичных: 0";
    private String mediumLegendText = "Средних: 0";
    private String lowLegendText = "Низких: 0";

    public RiskGaugeView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public RiskGaugeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RiskGaugeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        paddingH = dp(dm, 16f);
        paddingV = dp(dm, 14f);
        barHeight = dp(dm, 12f);
        barCorner = dp(dm, 6f);
        markerRadius = dp(dm, 8f);
        dotRadius = dp(dm, 4.5f);
        legendGap = dp(dm, 14f);
        desiredHeightPx = dp(dm, 108f);

        int textPrimary = ContextCompat.getColor(context, R.color.text_primary);
        int textSecondary = ContextCompat.getColor(context, R.color.text_secondary);
        int trackColor = ContextCompat.getColor(context, R.color.gauge_track);
        colorSafe = ContextCompat.getColor(context, R.color.risk_safe_fg);
        colorLow = ContextCompat.getColor(context, R.color.risk_low_fg);
        colorMedium = ContextCompat.getColor(context, R.color.risk_medium_fg);
        colorHigh = ContextCompat.getColor(context, R.color.risk_high_fg);
        colorCard = ContextCompat.getColor(context, R.color.surface_card);
        activeBadgeColor = colorSafe;

        titlePaint.setColor(textPrimary);
        titlePaint.setTextSize(sp(dm, 14f));
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        badgePaint.setColor(activeBadgeColor);
        badgePaint.setTextSize(sp(dm, 13f));
        badgePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        badgePaint.setTextAlign(Paint.Align.RIGHT);

        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);

        zoneLowPaint.setColor(colorSafe);
        zoneLowPaint.setStyle(Paint.Style.FILL);

        zoneMediumPaint.setColor(colorMedium);
        zoneMediumPaint.setStyle(Paint.Style.FILL);

        zoneHighPaint.setColor(colorHigh);
        zoneHighPaint.setStyle(Paint.Style.FILL);

        markerFillPaint.setColor(colorCard);
        markerFillPaint.setStyle(Paint.Style.FILL);

        markerStrokePaint.setColor(activeBadgeColor);
        markerStrokePaint.setStyle(Paint.Style.STROKE);
        markerStrokePaint.setStrokeWidth(dp(dm, 3f));

        legendTextPaint.setColor(textSecondary);
        legendTextPaint.setTextSize(sp(dm, 12f));

        dotPaint.setStyle(Paint.Style.FILL);

        updateAccessibilityText();
    }

    // Обновляю все подписи заранее при смене данных, чтобы не создавать строки во время перерисовки.
    public void setRiskSummary(@Nullable RiskSummary summary) {
        RiskSummary safe = summary != null ? summary : RiskSummary.empty();
        riskScore = Math.max(0, Math.min(100, safe.riskScore));

        String levelLabel;
        if (RiskSummary.LEVEL_HIGH.equals(safe.overallLevel)) {
            levelLabel = "Высокий";
            activeBadgeColor = colorHigh;
        } else if (RiskSummary.LEVEL_MEDIUM.equals(safe.overallLevel)) {
            levelLabel = "Средний";
            activeBadgeColor = colorMedium;
        } else if (RiskSummary.LEVEL_LOW.equals(safe.overallLevel)) {
            levelLabel = "Низкий";
            activeBadgeColor = colorLow;
        } else {
            levelLabel = "Безопасно";
            activeBadgeColor = colorSafe;
        }

        badgeText = levelLabel + " · " + riskScore + "/100";
        highLegendText = "Критичных: " + safe.highCount;
        mediumLegendText = "Средних: " + safe.mediumCount;
        lowLegendText = "Низких: " + safe.lowCount;

        badgePaint.setColor(activeBadgeColor);
        markerStrokePaint.setColor(activeBadgeColor);

        updateAccessibilityText();
        invalidate();
    }

    private void updateAccessibilityText() {
        setContentDescription(titleText + ": " + badgeText + ". "
                + highLegendText + ", " + mediumLegendText + ", " + lowLegendText);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) Math.ceil(desiredHeightPx);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        if (width <= paddingH * 2f) {
            return;
        }

        float left = paddingH;
        float right = width - paddingH;
        float barWidth = right - left;

        // Верхняя строка: заголовок слева и итоговый индекс справа.
        float headerBaseline = paddingV - titlePaint.ascent();
        canvas.drawText(titleText, left, headerBaseline, titlePaint);
        canvas.drawText(badgeText, right, headerBaseline, badgePaint);

        // Полоса шкалы из трёх цветовых зон с плавным бегунком текущего риска.
        float barTop = headerBaseline + paddingV * 0.95f;
        float barBottom = barTop + barHeight;
        barRect.set(left, barTop, right, barBottom);
        canvas.drawRoundRect(barRect, barCorner, barCorner, trackPaint);

        float filledWidth = barWidth * (riskScore / 100f);
        if (filledWidth > 0f) {
            float zone1End = left + Math.min(filledWidth, barWidth * 0.34f);
            segRect.set(left, barTop, zone1End, barBottom);
            canvas.drawRoundRect(segRect, barCorner, barCorner, zoneLowPaint);

            if (filledWidth > barWidth * 0.34f) {
                float zone2Start = left + barWidth * 0.30f;
                float zone2End = left + Math.min(filledWidth, barWidth * 0.67f);
                segRect.set(zone2Start, barTop, zone2End, barBottom);
                canvas.drawRoundRect(segRect, barCorner, barCorner, zoneMediumPaint);
            }

            if (filledWidth > barWidth * 0.67f) {
                float zone3Start = left + barWidth * 0.63f;
                float zone3End = left + filledWidth;
                segRect.set(zone3Start, barTop, zone3End, barBottom);
                canvas.drawRoundRect(segRect, barCorner, barCorner, zoneHighPaint);
            }
        }

        float markerX = left + filledWidth;
        float minMarkerX = left + markerRadius;
        float maxMarkerX = right - markerRadius;
        if (markerX < minMarkerX) {
            markerX = minMarkerX;
        } else if (markerX > maxMarkerX) {
            markerX = maxMarkerX;
        }
        float markerY = (barTop + barBottom) * 0.5f;
        canvas.drawCircle(markerX, markerY, markerRadius, markerFillPaint);
        canvas.drawCircle(markerX, markerY, markerRadius, markerStrokePaint);

        // Нижняя строка легенды со счётчиками по каждому уровню риска.
        float legendBaseline = barBottom + paddingV + (-legendTextPaint.ascent());
        float dotY = legendBaseline + (legendTextPaint.ascent() + legendTextPaint.descent()) * 0.45f;
        float cursorX = left;

        cursorX = drawLegendItem(canvas, cursorX, dotY, legendBaseline, colorHigh, highLegendText);
        cursorX = drawLegendItem(canvas, cursorX, dotY, legendBaseline, colorMedium, mediumLegendText);
        drawLegendItem(canvas, cursorX, dotY, legendBaseline, colorLow, lowLegendText);
    }

    private float drawLegendItem(
            @NonNull Canvas canvas,
            float startX,
            float dotY,
            float textBaseline,
            int dotColor,
            @NonNull String label
    ) {
        dotPaint.setColor(dotColor);
        canvas.drawCircle(startX + dotRadius, dotY, dotRadius, dotPaint);
        float textX = startX + dotRadius * 2f + dotRadius * 1.4f;
        canvas.drawText(label, textX, textBaseline, legendTextPaint);
        return textX + legendTextPaint.measureText(label) + legendGap;
    }

    private static float dp(@NonNull DisplayMetrics dm, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm);
    }

    private static float sp(@NonNull DisplayMetrics dm, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, dm);
    }
}

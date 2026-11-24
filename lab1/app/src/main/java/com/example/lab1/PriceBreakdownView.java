package com.example.lab1;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Мой собственный компонент для отображения структуры стоимости поездки:
 * Подача, Километры, Минуты и Опции.
 * Все кисти, прямоугольники и строки я создаю заранее в конструкторе и в setBreakdown(),
 * чтобы в onDraw() не создавалось ни одного объекта и анимация работала плавно.
 */
public class PriceBreakdownView extends View {

    public static final int SEGMENT_COUNT = 4;

    public interface OnSegmentSelectedListener {
        void onSegmentTapped(int segmentIndex);
    }

    // Создаю все кисти и геометрию один раз, чтобы сборщик мусора не тормозил перерисовку
    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cellBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPercentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();

    private final RectF barRect = new RectF();
    private final RectF segmentRect = new RectF();
    private final RectF cellRect = new RectF();
    private final Path barClipPath = new Path();

    // Области для определения, по какому сегменту или карточке нажал пользователь
    private final RectF[] barHitRects = new RectF[SEGMENT_COUNT];
    private final RectF[] cellHitRects = new RectF[SEGMENT_COUNT];

    private final int[] segmentColors = new int[SEGMENT_COUNT];
    private final int[] segmentOnBarTextColors = new int[SEGMENT_COUNT];
    private final String[] segmentNames = new String[]{"Подача", "Километры", "Минуты", "Опции"};

    // Подписи собираю заранее при обновлении данных, чтобы не клеить строки в onDraw
    private final String[] percentLabels = new String[]{"0%", "0%", "0%", "0%"};
    private final String[] valueLabels = new String[]{"0 ₽ · 0%", "0 ₽ · 0%", "0 ₽ · 0%", "0 ₽ · 0%"};

    // Буфер расчётных ширин сегментов — переиспользую на каждом кадре без new float[]
    private final float[] segmentWidths = new float[SEGMENT_COUNT];
    private final boolean[] clampedSegments = new boolean[SEGMENT_COUNT];

    private final float[] currentShares = new float[]{0.25f, 0.35f, 0.25f, 0.15f};
    private final float[] startShares = new float[]{0.25f, 0.35f, 0.25f, 0.15f};
    private final float[] targetShares = new float[]{0.25f, 0.35f, 0.25f, 0.15f};

    private final int[] amounts = new int[SEGMENT_COUNT];
    private int selectedSegmentIndex = -1;
    private boolean hasInitialData = false;

    private final float barHeightPx;
    private final float barCornerRadiusPx;
    private final float gapPx;
    private final float dotRadiusPx;
    private final float cellHeightPx;
    private final float cellCornerRadiusPx;
    private final float cellPaddingPx;
    private final float minVisibleSegmentWidthPx;
    private final float barPercentBaseSizePx;
    private final float barPercentMinSizePx;
    private final float barLabelHorizontalPadPx;

    private final ValueAnimator shareAnimator;
    private OnSegmentSelectedListener segmentListener;

    public PriceBreakdownView(Context context) {
        this(context, null);
    }

    public PriceBreakdownView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PriceBreakdownView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            barHitRects[i] = new RectF();
            cellHitRects[i] = new RectF();
        }

        int colorBase = Color.parseColor("#E0A000");
        int colorKm = Color.parseColor("#1D4ED8");
        int colorMin = Color.parseColor("#0F766E");
        int colorOptions = Color.parseColor("#7C3AED");
        int labelColor = Color.parseColor("#1C1B1F");
        int secondaryColor = Color.parseColor("#49454F");
        float defaultBarHeight = dpToPx(34f);
        float defaultCornerRadius = dpToPx(12f);

        // Вытаскиваю цвета и размеры из XML-атрибутов, чтобы шкалу можно было настраивать из разметки
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.PriceBreakdownView, defStyleAttr, 0);
            try {
                colorBase = a.getColor(R.styleable.PriceBreakdownView_colorBase, colorBase);
                colorKm = a.getColor(R.styleable.PriceBreakdownView_colorKm, colorKm);
                colorMin = a.getColor(R.styleable.PriceBreakdownView_colorMin, colorMin);
                colorOptions = a.getColor(R.styleable.PriceBreakdownView_colorOptions, colorOptions);
                labelColor = a.getColor(R.styleable.PriceBreakdownView_labelTextColor, labelColor);
                secondaryColor = a.getColor(R.styleable.PriceBreakdownView_secondaryTextColor, secondaryColor);
                defaultBarHeight = a.getDimension(R.styleable.PriceBreakdownView_barHeight, defaultBarHeight);
                defaultCornerRadius = a.getDimension(R.styleable.PriceBreakdownView_barCornerRadius, defaultCornerRadius);
            } finally {
                a.recycle();
            }
        }

        segmentColors[0] = colorBase;
        segmentColors[1] = colorKm;
        segmentColors[2] = colorMin;
        segmentColors[3] = colorOptions;

        // Для светлых сегментов (например жёлтой подачи) ставлю тёмный текст, для тёмных — белый
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            segmentOnBarTextColors[i] = isLightColor(segmentColors[i])
                    ? Color.parseColor("#1C1B1F")
                    : Color.WHITE;
        }

        barHeightPx = defaultBarHeight;
        barCornerRadiusPx = defaultCornerRadius;
        gapPx = dpToPx(8f);
        dotRadiusPx = dpToPx(5f);
        cellHeightPx = dpToPx(50f);
        cellCornerRadiusPx = dpToPx(10f);
        cellPaddingPx = dpToPx(10f);
        // Гарантирую минимальную ширину ненулевого сегмента, чтобы проценты (например 8% или 10%)
        // всегда помещались внутри шкалы и не обрезались
        minVisibleSegmentWidthPx = dpToPx(40f);
        barPercentBaseSizePx = spToPx(12f);
        barPercentMinSizePx = spToPx(9f);
        barLabelHorizontalPadPx = dpToPx(6f);

        segmentPaint.setStyle(Paint.Style.FILL);

        trackBgPaint.setStyle(Paint.Style.FILL);
        trackBgPaint.setColor(Color.argb(32, 128, 128, 128));

        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(dpToPx(2f));
        dividerPaint.setColor(Color.WHITE);

        highlightStrokePaint.setStyle(Paint.Style.STROKE);
        highlightStrokePaint.setStrokeWidth(dpToPx(2.5f));
        highlightStrokePaint.setColor(labelColor);

        cellBgPaint.setStyle(Paint.Style.FILL);
        cellBgPaint.setColor(Color.argb(20, 128, 128, 128));

        barPercentPaint.setTextSize(barPercentBaseSizePx);
        barPercentPaint.setFakeBoldText(true);
        barPercentPaint.setTextAlign(Paint.Align.CENTER);

        titleTextPaint.setColor(labelColor);
        titleTextPaint.setTextSize(spToPx(13f));
        titleTextPaint.setFakeBoldText(true);
        titleTextPaint.setTextAlign(Paint.Align.LEFT);

        valueTextPaint.setColor(secondaryColor);
        valueTextPaint.setTextSize(spToPx(12f));
        valueTextPaint.setTextAlign(Paint.Align.LEFT);

        // Один переиспользуемый аниматор для плавного перетекания долей при смене тарифа
        shareAnimator = ValueAnimator.ofFloat(0f, 1f);
        shareAnimator.setDuration(300L);
        shareAnimator.setInterpolator(new DecelerateInterpolator());
        shareAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            for (int i = 0; i < SEGMENT_COUNT; i++) {
                currentShares[i] = startShares[i] + (targetShares[i] - startShares[i]) * fraction;
            }
            invalidate();
        });

        setClickable(true);
        setFocusable(true);
    }

    public void setOnSegmentSelectedListener(@Nullable OnSegmentSelectedListener listener) {
        this.segmentListener = listener;
    }

    /**
     * Обновляю суммы, проценты и запускаю плавную анимацию шкалы.
     * Заодно корректирую округление у самого крупного сегмента, чтобы сумма процентов всегда была ровно 100%.
     */
    public void setBreakdown(PriceBreakdown breakdown, int selectedIndex) {
        if (breakdown == null) return;

        this.selectedSegmentIndex = selectedIndex;

        int[] roundedPercents = new int[SEGMENT_COUNT];
        int percentSum = 0;
        int largestIndex = 0;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            amounts[i] = breakdown.amounts[i];
            int pct = Math.round(breakdown.shares[i] * 100f);
            // Если сумма не ноль, но доля очень маленькая, показываю хотя бы 1%
            if (amounts[i] > 0 && pct == 0) {
                pct = 1;
            }
            roundedPercents[i] = pct;
            percentSum += pct;
            if (amounts[i] > amounts[largestIndex]) {
                largestIndex = i;
            }
        }
        if (breakdown.totalCost > 0 && percentSum != 100) {
            roundedPercents[largestIndex] = Math.max(1, roundedPercents[largestIndex] + (100 - percentSum));
        }

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            percentLabels[i] = roundedPercents[i] + "%";
            valueLabels[i] = amounts[i] + " ₽ · " + roundedPercents[i] + "%";
        }

        setContentDescription(
                "Структура цены: Подача " + valueLabels[0]
                        + ", Километры " + valueLabels[1]
                        + ", Минуты " + valueLabels[2]
                        + ", Опции " + valueLabels[3]
        );

        boolean changed = false;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            if (Math.abs(targetShares[i] - breakdown.shares[i]) > 0.0005f) {
                changed = true;
            }
            startShares[i] = currentShares[i];
            targetShares[i] = breakdown.shares[i];
        }

        boolean animatorsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || ValueAnimator.areAnimatorsEnabled();
        if (!hasInitialData || !animatorsEnabled) {
            hasInitialData = true;
            System.arraycopy(targetShares, 0, currentShares, 0, SEGMENT_COUNT);
            invalidate();
            return;
        }

        if (changed) {
            shareAnimator.cancel();
            shareAnimator.start();
        } else {
            invalidate();
        }
    }

    public String getSegmentName(int index) {
        if (index < 0 || index >= SEGMENT_COUNT) return "";
        return segmentNames[index];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Считаю желаемую высоту с запасом под увеличенный системный шрифт
        int desiredWidth = Math.round(dpToPx(320f)) + getPaddingLeft() + getPaddingRight();
        float textScaleHeight = Math.max(cellHeightPx, (titleTextPaint.getTextSize() + valueTextPaint.getTextSize()) * 2.15f);
        int desiredHeight = Math.round(
                barHeightPx + dpToPx(14f) + textScaleHeight * 2f + gapPx + dpToPx(6f)
        ) + getPaddingTop() + getPaddingBottom();

        int measuredWidth = resolveSize(desiredWidth, widthMeasureSpec);
        int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    /**
     * Рассчитываю ширину каждого сегмента так, чтобы узкие ненулевые сегменты (Подача, Опции)
     * получали минимальную читаемую ширину, а остальное место честно делилось между крупными.
     */
    private void computeSegmentWidths(float availableWidth) {
        int activeCount = 0;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            clampedSegments[i] = false;
            segmentWidths[i] = 0f;
            if (currentShares[i] > 0.001f) {
                activeCount++;
            }
        }
        if (activeCount == 0) return;

        float effectiveMinWidth = Math.min(minVisibleSegmentWidthPx, availableWidth / activeCount);
        float reservedWidth = 0f;
        float freeSharesSum = 0f;

        // Первый проход: фиксирую слишком узкие сегменты на минимальной ширине
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            if (currentShares[i] <= 0.001f) continue;
            float rawWidth = availableWidth * currentShares[i];
            if (rawWidth < effectiveMinWidth) {
                clampedSegments[i] = true;
                segmentWidths[i] = effectiveMinWidth;
                reservedWidth += effectiveMinWidth;
            } else {
                freeSharesSum += currentShares[i];
            }
        }

        // Второй проход: распределяю оставшуюся ширину пропорционально между крупными сегментами
        float remainingWidth = Math.max(0f, availableWidth - reservedWidth);
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            if (currentShares[i] <= 0.001f || clampedSegments[i]) continue;
            if (freeSharesSum > 0f) {
                segmentWidths[i] = remainingWidth * (currentShares[i] / freeSharesSum);
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float left = getPaddingLeft();
        float top = getPaddingTop() + dpToPx(2f);
        float right = getWidth() - getPaddingRight();
        float availableWidth = right - left;
        if (availableWidth <= 0f) return;

        // 1. Фоновая подложка шкалы
        barRect.set(left, top, right, top + barHeightPx);
        canvas.drawRoundRect(barRect, barCornerRadiusPx, barCornerRadiusPx, trackBgPaint);

        // 2. Цветные сегменты внутри скруглённого контура
        computeSegmentWidths(availableWidth);
        barClipPath.reset();
        barClipPath.addRoundRect(barRect, barCornerRadiusPx, barCornerRadiusPx, Path.Direction.CW);

        int saveCount = canvas.save();
        canvas.clipPath(barClipPath);

        int lastActiveIndex = -1;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            if (segmentWidths[i] > 0.5f) lastActiveIndex = i;
        }

        float cursorX = left;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float segWidth = segmentWidths[i];
            if (segWidth > 0.5f) {
                float segRight = (i == lastActiveIndex) ? right : cursorX + segWidth;
                float actualWidth = segRight - cursorX;
                segmentRect.set(cursorX, top, segRight, top + barHeightPx);
                barHitRects[i].set(segmentRect);

                segmentPaint.setColor(segmentColors[i]);
                boolean dimmed = (selectedSegmentIndex != -1 && selectedSegmentIndex != i);
                segmentPaint.setAlpha(dimmed ? 95 : 255);
                canvas.drawRect(segmentRect, segmentPaint);

                if (cursorX > left) {
                    canvas.drawLine(cursorX, top, cursorX, top + barHeightPx, dividerPaint);
                }

                // Подгоняю размер шрифта под ширину сегмента, чтобы проценты всегда влезали
                float maxTextWidth = actualWidth - barLabelHorizontalPadPx;
                if (maxTextWidth > 0f) {
                    float chosenSize = barPercentBaseSizePx;
                    barPercentPaint.setTextSize(chosenSize);
                    while (barPercentPaint.measureText(percentLabels[i]) > maxTextWidth
                            && chosenSize > barPercentMinSizePx) {
                        chosenSize -= 1f;
                        barPercentPaint.setTextSize(chosenSize);
                    }
                    if (barPercentPaint.measureText(percentLabels[i]) <= maxTextWidth) {
                        barPercentPaint.setColor(segmentOnBarTextColors[i]);
                        barPercentPaint.setAlpha(dimmed ? 160 : 255);
                        barPercentPaint.getFontMetrics(fontMetrics);
                        float textY = top + barHeightPx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
                        canvas.drawText(percentLabels[i], cursorX + actualWidth / 2f, textY, barPercentPaint);
                    }
                    barPercentPaint.setTextSize(barPercentBaseSizePx);
                }

                cursorX = segRight;
            } else {
                barHitRects[i].setEmpty();
            }
        }
        canvas.restoreToCount(saveCount);

        // Обводка выделенного касанием сегмента на самой шкале
        if (selectedSegmentIndex >= 0 && selectedSegmentIndex < SEGMENT_COUNT) {
            RectF selectedBar = barHitRects[selectedSegmentIndex];
            if (!selectedBar.isEmpty()) {
                highlightStrokePaint.setColor(titleTextPaint.getColor());
                canvas.drawRoundRect(selectedBar, dpToPx(6f), dpToPx(6f), highlightStrokePaint);
            }
        }

        // 3. Карточки-подписи 2x2 под шкалой, чтобы всё читалось без отдельной легенды
        float gridTop = top + barHeightPx + dpToPx(14f);
        float cellWidth = (availableWidth - gapPx) / 2f;
        float dynamicCellHeight = Math.max(cellHeightPx,
                (titleTextPaint.getTextSize() + valueTextPaint.getTextSize()) * 2.15f);

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            int col = i % 2;
            int row = i / 2;

            float cLeft = left + col * (cellWidth + gapPx);
            float cTop = gridTop + row * (dynamicCellHeight + gapPx);
            float cRight = cLeft + cellWidth;
            float cBottom = cTop + dynamicCellHeight;

            cellRect.set(cLeft, cTop, cRight, cBottom);
            cellHitRects[i].set(cellRect);

            boolean isSelected = (selectedSegmentIndex == i);
            boolean isEmptyAmount = (amounts[i] == 0);

            cellBgPaint.setAlpha(isSelected ? 42 : 18);
            canvas.drawRoundRect(cellRect, cellCornerRadiusPx, cellCornerRadiusPx, cellBgPaint);

            if (isSelected) {
                highlightStrokePaint.setColor(segmentColors[i]);
                canvas.drawRoundRect(cellRect, cellCornerRadiusPx, cellCornerRadiusPx, highlightStrokePaint);
            }

            float dotCenterX = cLeft + cellPaddingPx + dotRadiusPx;
            float dotCenterY = cTop + dynamicCellHeight / 2f;
            segmentPaint.setColor(segmentColors[i]);
            segmentPaint.setAlpha(isEmptyAmount ? 100 : 255);
            canvas.drawCircle(dotCenterX, dotCenterY, isSelected ? dotRadiusPx * 1.25f : dotRadiusPx, segmentPaint);

            float textLeft = dotCenterX + dotRadiusPx + dpToPx(8f);
            titleTextPaint.setAlpha(isEmptyAmount ? 140 : 255);
            titleTextPaint.getFontMetrics(fontMetrics);
            float titleBaseline = cTop + dynamicCellHeight * 0.40f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
            canvas.drawText(segmentNames[i], textLeft, titleBaseline, titleTextPaint);

            valueTextPaint.setAlpha(isEmptyAmount ? 130 : 255);
            valueTextPaint.getFontMetrics(fontMetrics);
            float valueBaseline = cTop + dynamicCellHeight * 0.74f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
            canvas.drawText(valueLabels[i], textLeft, valueBaseline, valueTextPaint);
        }
    }

    // Обрабатываю касания как по цветным отрезкам шкалы, так и по карточкам с подписями ниже
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int tappedIndex = findSegmentAt(event.getX(), event.getY());
            if (tappedIndex != -1) {
                performClick();
                if (segmentListener != null) {
                    segmentListener.onSegmentTapped(tappedIndex);
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int findSegmentAt(float x, float y) {
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            if (barHitRects[i].contains(x, y) || cellHitRects[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        shareAnimator.cancel();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private boolean isLightColor(int color) {
        double luminance = (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.62;
    }
}

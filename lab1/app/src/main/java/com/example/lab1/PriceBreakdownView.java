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
 * Собственный графический компонент: горизонтальная шкала структуры цены поездки в такси
 * (Подача, Километры, Минуты, Опции).
 *
 * Что реализовано:
 * 1. Чтение собственных XML-атрибутов из attrs.xml через TypedArray.
 * 2. Корректный расчёт размеров в onMeasure с переводом dp/sp в пиксели.
 * 3. Отрисовка на Canvas в onDraw БЕЗ единого создания объектов (все Paint, RectF, Path,
 *    FontMetrics и строки подписей создаются заранее в конструкторе и в setData).
 * 4. Бонус +1: плавная анимация изменения долей через ValueAnimator.
 * 5. Бонус +1: обработка касаний в onTouchEvent для выделения сегмента по нажатию.
 */
public class PriceBreakdownView extends View {

    public static final int SEGMENT_COUNT = 4;

    public interface OnSegmentSelectedListener {
        void onSegmentTapped(int segmentIndex);
    }

    // Все объекты для рисования создаём один раз в конструкторе, чтобы в onDraw не работал сборщик мусора
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

    // Координаты прямоугольников сегментов на шкале и ячеек с подписями (для обработки касаний)
    private final RectF[] barHitRects = new RectF[SEGMENT_COUNT];
    private final RectF[] cellHitRects = new RectF[SEGMENT_COUNT];

    private final int[] segmentColors = new int[SEGMENT_COUNT];
    private final int[] segmentOnBarTextColors = new int[SEGMENT_COUNT];
    private final String[] segmentNames = new String[]{"Подача", "Километры", "Минуты", "Опции"};

    // Строки для отрисовки пересчитываются только при смене данных в setData, а не в onDraw
    private final String[] percentLabels = new String[]{"0%", "0%", "0%", "0%"};
    private final String[] valueLabels = new String[]{"0 ₽ · 0%", "0 ₽ · 0%", "0 ₽ · 0%", "0 ₽ · 0%"};

    // Текущие (анимируемые), стартовые и целевые доли каждого сегмента (сумма = 1.0)
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
    private final float minBarLabelWidthPx;

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

        // Инициализируем прямоугольники для определения попадания пальцем
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            barHitRects[i] = new RectF();
            cellHitRects[i] = new RectF();
        }

        // Значения по умолчанию с гарантированным контрастом (>= 4.5:1 для текста)
        int colorBase = Color.parseColor("#E0A000");    // Янтарный — подача
        int colorKm = Color.parseColor("#1D4ED8");      // Синий — километры
        int colorMin = Color.parseColor("#0F766E");     // Бирюзовый — минуты
        int colorOptions = Color.parseColor("#7C3AED"); // Фиолетовый — опции
        int labelColor = Color.parseColor("#1C1B1F");
        int secondaryColor = Color.parseColor("#49454F");
        float defaultBarHeight = dpToPx(34f);
        float defaultCornerRadius = dpToPx(12f);

        // Читаем кастомные атрибуты из XML-разметки через TypedArray
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

        // Подбираем цвет текста внутри каждого сегмента (тёмный на жёлтом, белый на тёмных)
        // чтобы соблюдать стандарт контрастности WCAG 4.5:1
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
        minBarLabelWidthPx = dpToPx(38f);

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

        barPercentPaint.setTextSize(spToPx(12f));
        barPercentPaint.setFakeBoldText(true);
        barPercentPaint.setTextAlign(Paint.Align.CENTER);

        titleTextPaint.setColor(labelColor);
        titleTextPaint.setTextSize(spToPx(13f));
        titleTextPaint.setFakeBoldText(true);
        titleTextPaint.setTextAlign(Paint.Align.LEFT);

        valueTextPaint.setColor(secondaryColor);
        valueTextPaint.setTextSize(spToPx(12f));
        valueTextPaint.setTextAlign(Paint.Align.LEFT);

        // Настраиваем один ValueAnimator и переиспользуем его при каждом обновлении данных
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
     * Обновляем данные шкалы (суммы и доли) и запускаем плавную анимацию перехода.
     * Все текстовые подписи формируются здесь, чтобы в onDraw не создавались строки.
     */
    public void setBreakdown(PriceBreakdown breakdown, int selectedIndex) {
        if (breakdown == null) return;

        this.selectedSegmentIndex = selectedIndex;

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            amounts[i] = breakdown.amounts[i];
            int pct = Math.round(breakdown.shares[i] * 100f);
            percentLabels[i] = pct + "%";
            valueLabels[i] = amounts[i] + " ₽ · " + pct + "%";
        }

        // Обновляем описание для TalkBack (доступность)
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
        // Рассчитываем желаемые размеры в dp -> px с учётом padding и увеличенного шрифта
        int desiredWidth = Math.round(dpToPx(320f)) + getPaddingLeft() + getPaddingRight();
        // Высота: шкала + отступ + 2 ряда карточек-подписей (с учётом системного масштаба текста)
        float textScaleHeight = Math.max(cellHeightPx, (titleTextPaint.getTextSize() + valueTextPaint.getTextSize()) * 2.15f);
        int desiredHeight = Math.round(
                barHeightPx + dpToPx(14f) + textScaleHeight * 2f + gapPx + dpToPx(6f)
        ) + getPaddingTop() + getPaddingBottom();

        int measuredWidth = resolveSize(desiredWidth, widthMeasureSpec);
        int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float left = getPaddingLeft();
        float top = getPaddingTop() + dpToPx(2f);
        float right = getWidth() - getPaddingRight();
        float availableWidth = right - left;
        if (availableWidth <= 0f) return;

        // 1. Рисуем фоновую подложку шкалы
        barRect.set(left, top, right, top + barHeightPx);
        canvas.drawRoundRect(barRect, barCornerRadiusPx, barCornerRadiusPx, trackBgPaint);

        // 2. Рисуем цветные сегменты внутри скруглённой области (clipPath)
        barClipPath.reset();
        barClipPath.addRoundRect(barRect, barCornerRadiusPx, barCornerRadiusPx, Path.Direction.CW);

        int saveCount = canvas.save();
        canvas.clipPath(barClipPath);

        float cursorX = left;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float segWidth = availableWidth * currentShares[i];
            float segRight = (i == SEGMENT_COUNT - 1) ? right : cursorX + segWidth;

            if (segWidth > 0.5f) {
                segmentRect.set(cursorX, top, segRight, top + barHeightPx);
                barHitRects[i].set(segmentRect);

                segmentPaint.setColor(segmentColors[i]);
                // Если выбран другой сегмент — слегка приглушаем невыбранные, чтобы акцент был очевиден
                boolean dimmed = (selectedSegmentIndex != -1 && selectedSegmentIndex != i);
                segmentPaint.setAlpha(dimmed ? 95 : 255);
                canvas.drawRect(segmentRect, segmentPaint);

                // Тонкий разделитель между соседними сегментами
                if (cursorX > left) {
                    canvas.drawLine(cursorX, top, cursorX, top + barHeightPx, dividerPaint);
                }

                // Подписываем проценты прямо внутри сегмента, если он достаточно широкий
                if (segWidth >= minBarLabelWidthPx) {
                    barPercentPaint.setColor(segmentOnBarTextColors[i]);
                    barPercentPaint.setAlpha(dimmed ? 160 : 255);
                    barPercentPaint.getFontMetrics(fontMetrics);
                    float textY = top + barHeightPx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
                    canvas.drawText(percentLabels[i], cursorX + segWidth / 2f, textY, barPercentPaint);
                }
            } else {
                barHitRects[i].setEmpty();
            }

            cursorX = segRight;
        }
        canvas.restoreToCount(saveCount);

        // Если сегмент выделен касанием — обводим его на шкале контрастной рамкой (WCAG 1.4.1)
        if (selectedSegmentIndex >= 0 && selectedSegmentIndex < SEGMENT_COUNT) {
            RectF selectedBar = barHitRects[selectedSegmentIndex];
            if (!selectedBar.isEmpty()) {
                highlightStrokePaint.setColor(titleTextPaint.getColor());
                canvas.drawRoundRect(selectedBar, dpToPx(6f), dpToPx(6f), highlightStrokePaint);
            }
        }

        // 3. Рисуем подписи прямо на компоненте в виде сетки 2x2 (читается без внешней легенды)
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

            // Подложка ячейки
            cellBgPaint.setAlpha(isSelected ? 42 : 18);
            canvas.drawRoundRect(cellRect, cellCornerRadiusPx, cellCornerRadiusPx, cellBgPaint);

            // Контрастная рамка у выбранного сегмента (форма + цвет)
            if (isSelected) {
                highlightStrokePaint.setColor(segmentColors[i]);
                canvas.drawRoundRect(cellRect, cellCornerRadiusPx, cellCornerRadiusPx, highlightStrokePaint);
            }

            // Цветной маркер сегмента
            float dotCenterX = cLeft + cellPaddingPx + dotRadiusPx;
            float dotCenterY = cTop + dynamicCellHeight / 2f;
            segmentPaint.setColor(segmentColors[i]);
            segmentPaint.setAlpha(isEmptyAmount ? 100 : 255);
            canvas.drawCircle(dotCenterX, dotCenterY, isSelected ? dotRadiusPx * 1.25f : dotRadiusPx, segmentPaint);

            // Заголовок сегмента (Подача / Километры / Минуты / Опции)
            float textLeft = dotCenterX + dotRadiusPx + dpToPx(8f);
            titleTextPaint.setAlpha(isEmptyAmount ? 140 : 255);
            titleTextPaint.getFontMetrics(fontMetrics);
            float titleBaseline = cTop + dynamicCellHeight * 0.40f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
            canvas.drawText(segmentNames[i], textLeft, titleBaseline, titleTextPaint);

            // Сумма и процент (например, "216 ₽ · 32%")
            valueTextPaint.setAlpha(isEmptyAmount ? 130 : 255);
            valueTextPaint.getFontMetrics(fontMetrics);
            float valueBaseline = cTop + dynamicCellHeight * 0.74f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
            canvas.drawText(valueLabels[i], textLeft, valueBaseline, valueTextPaint);
        }
    }

    /**
     * Бонус +1: обработка касаний внутри собственного View.
     * Пользователь может нажать как на сам цветной сегмент шкалы, так и на его карточку-подпись ниже.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            int tappedIndex = findSegmentAt(x, y);
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

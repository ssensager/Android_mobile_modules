package com.example.lab4;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Пользовательский ImageView с масштабированием щипком (ScaleGestureDetector + Matrix),
 * перетаскиванием приближённого фото и двойным нажатием для быстрого сброса масштаба.
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float DOUBLE_TAP_ZOOM = 2.5f;

    private final Matrix drawMatrix = new Matrix();
    private final float[] matrixValues = new float[9];
    private final PointF lastTouch = new PointF();

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private float baseScale = 1.0f;
    private float currentZoom = 1.0f;
    private boolean isDragging = false;
    private boolean initialized = false;

    public ZoomableImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (currentZoom > MIN_ZOOM + 0.1f) {
                    resetZoom();
                } else {
                    float factor = DOUBLE_TAP_ZOOM / currentZoom;
                    applyScale(factor, e.getX(), e.getY());
                }
                return true;
            }
        });
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        initialized = false;
        post(this::fitImageToView);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initialized = false;
        fitImageToView();
    }

    // Вписываю картинку по центру экрана и запоминаю базовый масштаб 1.0x
    public void fitImageToView() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();
        if (dWidth <= 0 || dHeight <= 0) {
            return;
        }

        float viewW = getWidth();
        float viewH = getHeight();
        baseScale = Math.min(viewW / dWidth, viewH / dHeight);
        currentZoom = MIN_ZOOM;

        float dx = (viewW - dWidth * baseScale) * 0.5f;
        float dy = (viewH - dHeight * baseScale) * 0.5f;

        drawMatrix.reset();
        drawMatrix.postScale(baseScale, baseScale);
        drawMatrix.postTranslate(dx, dy);
        setImageMatrix(drawMatrix);
        initialized = true;
    }

    public void resetZoom() {
        fitImageToView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!initialized) {
            fitImageToView();
        }
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(event.getX(), event.getY());
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging && !scaleDetector.isInProgress() && currentZoom > MIN_ZOOM) {
                    float dx = event.getX() - lastTouch.x;
                    float dy = event.getY() - lastTouch.y;
                    drawMatrix.postTranslate(dx, dy);
                    clampTranslation();
                    setImageMatrix(drawMatrix);
                    lastTouch.set(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                isDragging = false;
                break;
            default:
                break;
        }
        return true;
    }

    private void applyScale(float scaleFactor, float focusX, float focusY) {
        float targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentZoom * scaleFactor));
        float appliedFactor = targetZoom / currentZoom;
        currentZoom = targetZoom;

        drawMatrix.postScale(appliedFactor, appliedFactor, focusX, focusY);
        clampTranslation();
        setImageMatrix(drawMatrix);
    }

    // Не даю пользователю увести увеличенное изображение за пределы экрана
    private void clampTranslation() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        RectF rect = new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawMatrix.mapRect(rect);

        float viewW = getWidth();
        float viewH = getHeight();
        float deltaX = 0f;
        float deltaY = 0f;

        if (rect.width() <= viewW) {
            deltaX = (viewW - rect.width()) * 0.5f - rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < viewW) {
            deltaX = viewW - rect.right;
        }

        if (rect.height() <= viewH) {
            deltaY = (viewH - rect.height()) * 0.5f - rect.top;
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewH) {
            deltaY = viewH - rect.bottom;
        }

        drawMatrix.postTranslate(deltaX, deltaY);
        drawMatrix.getValues(matrixValues);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            applyScale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            return true;
        }
    }
}

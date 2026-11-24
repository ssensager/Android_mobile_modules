package com.example.lab4;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Генератор стартовых ботанических иллюстраций для первых четырёх растений.
 * Рисую картинки программно и сохраняю их через ImageCompressor, чтобы при первом запуске
 * каталог сразу выглядел живым и показывал реальные локальные JPEG-файлы.
 */
public final class SamplePlantArt {

    private SamplePlantArt() {
    }

    public static Bitmap createPlantIllustration(int plantIndex) {
        int size = 1024;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        switch (plantIndex) {
            case 0:
                drawMonstera(canvas, size);
                break;
            case 1:
                drawFicus(canvas, size);
                break;
            case 2:
                drawSansevieria(canvas, size);
                break;
            default:
                drawZamioculcas(canvas, size);
                break;
        }
        return bitmap;
    }

    private static void drawBackdrop(Canvas canvas, int size, int topColor, int bottomColor, int glowColor) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new LinearGradient(0, 0, size, size, topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, size, size, bg);

        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new RadialGradient(
                size * 0.5f, size * 0.38f, size * 0.48f,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(size * 0.5f, size * 0.38f, size * 0.48f, glow);

        // Добавляю лёгкий подоконник снизу, чтобы горшок визуально стоял на опоре
        Paint shelf = new Paint(Paint.ANTI_ALIAS_FLAG);
        shelf.setColor(Color.argb(42, 45, 25, 85));
        canvas.drawRect(0, size * 0.78f, size, size, shelf);
    }

    private static void drawCeramicPot(Canvas canvas, int size, int mainColor, int shadowColor, int rimColor) {
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(Color.argb(65, 20, 12, 38));
        canvas.drawOval(new RectF(size * 0.30f, size * 0.85f, size * 0.70f, size * 0.91f), shadow);

        Path body = new Path();
        body.moveTo(size * 0.33f, size * 0.64f);
        body.lineTo(size * 0.67f, size * 0.64f);
        body.lineTo(size * 0.61f, size * 0.87f);
        body.lineTo(size * 0.39f, size * 0.87f);
        body.close();

        Paint potPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        potPaint.setShader(new LinearGradient(
                size * 0.33f, size * 0.64f, size * 0.67f, size * 0.87f,
                mainColor, shadowColor, Shader.TileMode.CLAMP
        ));
        canvas.drawPath(body, potPaint);

        Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimPaint.setColor(rimColor);
        canvas.drawRoundRect(
                new RectF(size * 0.31f, size * 0.60f, size * 0.69f, size * 0.66f),
                18f, 18f, rimPaint
        );
    }

    // Монстера деликатесная с крупными резными тропическими листьями
    private static void drawMonstera(Canvas canvas, int size) {
        drawBackdrop(canvas, size, Color.parseColor("#EDE9FE"), Color.parseColor("#D1FAE5"), Color.parseColor("#88FFFFFF"));

        Paint stem = new Paint(Paint.ANTI_ALIAS_FLAG);
        stem.setColor(Color.parseColor("#166534"));
        stem.setStrokeWidth(18f);
        stem.setStyle(Paint.Style.STROKE);
        stem.setStrokeCap(Paint.Cap.ROUND);

        float[][] stems = {
                {0.50f, 0.62f, 0.32f, 0.42f},
                {0.50f, 0.62f, 0.68f, 0.40f},
                {0.50f, 0.62f, 0.50f, 0.28f},
                {0.50f, 0.62f, 0.38f, 0.25f},
                {0.50f, 0.62f, 0.63f, 0.27f}
        };
        for (float[] s : stems) {
            Path p = new Path();
            p.moveTo(size * s[0], size * s[1]);
            p.quadTo(size * 0.50f, size * 0.46f, size * s[2], size * s[3]);
            canvas.drawPath(p, stem);
        }

        drawMonsteraLeaf(canvas, size * 0.31f, size * 0.40f, 155f, -35f, Color.parseColor("#15803D"));
        drawMonsteraLeaf(canvas, size * 0.69f, size * 0.39f, 160f, 34f, Color.parseColor("#166534"));
        drawMonsteraLeaf(canvas, size * 0.39f, size * 0.25f, 130f, -18f, Color.parseColor("#22C55E"));
        drawMonsteraLeaf(canvas, size * 0.62f, size * 0.26f, 135f, 20f, Color.parseColor("#15803D"));
        drawMonsteraLeaf(canvas, size * 0.50f, size * 0.31f, 175f, 0f, Color.parseColor("#14532D"));

        drawCeramicPot(canvas, size, Color.parseColor("#F5F3FF"), Color.parseColor("#C4B5FD"), Color.parseColor("#FFFFFF"));
    }

    private static void drawMonsteraLeaf(Canvas canvas, float cx, float cy, float r, float angle, int color) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angle);

        Paint leaf = new Paint(Paint.ANTI_ALIAS_FLAG);
        leaf.setColor(color);
        RectF oval = new RectF(-r * 0.78f, -r, r * 0.78f, r * 0.85f);
        canvas.drawOval(oval, leaf);

        // Светлые прожилки и характерные прорези на листе монстеры
        Paint vein = new Paint(Paint.ANTI_ALIAS_FLAG);
        vein.setColor(Color.argb(120, 187, 247, 208));
        vein.setStrokeWidth(7f);
        canvas.drawLine(0, -r * 0.82f, 0, r * 0.75f, vein);

        Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        cut.setColor(Color.parseColor("#DCFCE7"));
        for (int i = -1; i <= 1; i++) {
            float y = i * r * 0.38f;
            canvas.drawRoundRect(new RectF(-r * 0.82f, y - 10f, -r * 0.22f, y + 10f), 10f, 10f, cut);
            canvas.drawRoundRect(new RectF(r * 0.22f, y - 10f, r * 0.82f, y + 10f), 10f, 10f, cut);
        }
        canvas.restore();
    }

    // Фикус Бенджамина с изящным стволом и пышной кроной
    private static void drawFicus(Canvas canvas, int size) {
        drawBackdrop(canvas, size, Color.parseColor("#F3E8FF"), Color.parseColor("#FEF3C7"), Color.parseColor("#99FFFFFF"));

        Paint trunk = new Paint(Paint.ANTI_ALIAS_FLAG);
        trunk.setColor(Color.parseColor("#78350F"));
        trunk.setStrokeWidth(26f);
        trunk.setStyle(Paint.Style.STROKE);
        trunk.setStrokeCap(Paint.Cap.ROUND);

        Path p1 = new Path();
        p1.moveTo(size * 0.48f, size * 0.62f);
        p1.cubicTo(size * 0.44f, size * 0.50f, size * 0.55f, size * 0.42f, size * 0.50f, size * 0.30f);
        canvas.drawPath(p1, trunk);

        trunk.setStrokeWidth(16f);
        canvas.drawLine(size * 0.50f, size * 0.46f, size * 0.36f, size * 0.35f, trunk);
        canvas.drawLine(size * 0.50f, size * 0.44f, size * 0.65f, size * 0.34f, trunk);

        int[] greens = {
                Color.parseColor("#14532D"),
                Color.parseColor("#166534"),
                Color.parseColor("#15803D"),
                Color.parseColor("#22C55E"),
                Color.parseColor("#4ADE80")
        };
        float[][] foliage = {
                {0.50f, 0.28f, 130f, 0}, {0.36f, 0.33f, 110f, 1}, {0.64f, 0.33f, 112f, 1},
                {0.42f, 0.21f, 95f, 2}, {0.58f, 0.21f, 98f, 2}, {0.30f, 0.40f, 82f, 3},
                {0.70f, 0.40f, 85f, 3}, {0.50f, 0.17f, 86f, 3}, {0.45f, 0.31f, 75f, 4},
                {0.57f, 0.30f, 72f, 4}
        };
        Paint leafPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (float[] f : foliage) {
            leafPaint.setColor(greens[(int) f[3]]);
            canvas.drawCircle(size * f[0], size * f[1], f[2], leafPaint);
        }

        drawCeramicPot(canvas, size, Color.parseColor("#F59E0B"), Color.parseColor("#B45309"), Color.parseColor("#FDE68A"));
    }

    // Сансевиерия трёхполосная с вертикальными полосатыми листьями-мечами
    private static void drawSansevieria(Canvas canvas, int size) {
        drawBackdrop(canvas, size, Color.parseColor("#E0E7FF"), Color.parseColor("#ECFDF5"), Color.parseColor("#88FFFFFF"));

        float[][] blades = {
                {0.37f, 0.62f, 0.31f, 0.22f, 38f},
                {0.44f, 0.62f, 0.42f, 0.14f, 46f},
                {0.51f, 0.62f, 0.52f, 0.11f, 50f},
                {0.58f, 0.62f, 0.60f, 0.17f, 44f},
                {0.64f, 0.62f, 0.70f, 0.26f, 36f}
        };

        for (float[] b : blades) {
            Path rim = new Path();
            rim.moveTo(size * b[0] - b[4], size * b[1]);
            rim.quadTo(size * b[0] - b[4] * 0.7f, size * 0.36f, size * b[2], size * b[3]);
            rim.quadTo(size * b[0] + b[4] * 0.7f, size * 0.36f, size * b[0] + b[4], size * b[1]);
            rim.close();

            Paint yellowRim = new Paint(Paint.ANTI_ALIAS_FLAG);
            yellowRim.setColor(Color.parseColor("#FACC15"));
            canvas.drawPath(rim, yellowRim);

            Path inner = new Path();
            inner.moveTo(size * b[0] - b[4] * 0.72f, size * b[1]);
            inner.quadTo(size * b[0] - b[4] * 0.45f, size * 0.38f, size * b[2], size * b[3] + 24f);
            inner.quadTo(size * b[0] + b[4] * 0.45f, size * 0.38f, size * b[0] + b[4] * 0.72f, size * b[1]);
            inner.close();

            Paint greenBody = new Paint(Paint.ANTI_ALIAS_FLAG);
            greenBody.setColor(Color.parseColor("#166534"));
            canvas.drawPath(inner, greenBody);

            // Поперечные светлые полосы на листьях сансевиерии
            Paint stripe = new Paint(Paint.ANTI_ALIAS_FLAG);
            stripe.setColor(Color.argb(95, 134, 239, 172));
            stripe.setStrokeWidth(10f);
            for (int k = 1; k <= 5; k++) {
                float y = size * b[3] + k * 72f;
                if (y < size * 0.60f) {
                    float cx = size * (b[2] + (b[0] - b[2]) * (k / 6f));
                    canvas.drawLine(cx - b[4] * 0.42f, y, cx + b[4] * 0.42f, y - 6f, stripe);
                }
            }
        }

        drawCeramicPot(canvas, size, Color.parseColor("#6D28D9"), Color.parseColor("#4C1D95"), Color.parseColor("#A78BFA"));
    }

    // Замиокулькас с глянцевыми тёмно-зелёными стеблями и парными листочками
    private static void drawZamioculcas(Canvas canvas, int size) {
        drawBackdrop(canvas, size, Color.parseColor("#F5F3FF"), Color.parseColor("#E0F2FE"), Color.parseColor("#88FFFFFF"));

        Paint stemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stemPaint.setColor(Color.parseColor("#14532D"));
        stemPaint.setStrokeWidth(16f);
        stemPaint.setStyle(Paint.Style.STROKE);
        stemPaint.setStrokeCap(Paint.Cap.ROUND);

        float[][] branches = {
                {0.46f, 0.62f, 0.27f, 0.24f},
                {0.50f, 0.62f, 0.47f, 0.16f},
                {0.53f, 0.62f, 0.64f, 0.19f},
                {0.56f, 0.62f, 0.74f, 0.30f}
        };

        Paint leafletPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glossPaint.setColor(Color.argb(95, 255, 255, 255));

        for (int idx = 0; idx < branches.length; idx++) {
            float[] br = branches[idx];
            float x0 = size * br[0];
            float y0 = size * br[1];
            float x1 = size * br[2];
            float y1 = size * br[3];
            canvas.drawLine(x0, y0, x1, y1, stemPaint);

            leafletPaint.setColor(idx % 2 == 0 ? Color.parseColor("#15803D") : Color.parseColor("#166534"));
            for (int step = 1; step <= 5; step++) {
                float t = step / 5.4f;
                float px = x0 + (x1 - x0) * t;
                float py = y0 + (y1 - y0) * t;

                drawGlossyLeaflet(canvas, px - 28f, py - 8f, -35f, leafletPaint, glossPaint);
                drawGlossyLeaflet(canvas, px + 28f, py - 8f, 35f, leafletPaint, glossPaint);
            }
        }

        drawCeramicPot(canvas, size, Color.parseColor("#334155"), Color.parseColor("#0F172A"), Color.parseColor("#64748B"));
    }

    private static void drawGlossyLeaflet(
            Canvas canvas, float cx, float cy, float angle, Paint bodyPaint, Paint glossPaint
    ) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angle);
        canvas.drawOval(new RectF(-24f, -44f, 24f, 44f), bodyPaint);
        canvas.drawOval(new RectF(-10f, -26f, 4f, 8f), glossPaint);
        canvas.restore();
    }
}

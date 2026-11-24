package com.example.lab4;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Утилита двухэтапного декодирования и сжатия фотографий во внутреннее хранилище.
 * Вынес сюда всю работу с BitmapFactory, чтобы не засорять экраны и легко тестировать расчёт масштаба.
 */
public final class ImageCompressor {

    public static final int MAX_DIMENSION = 1024;
    public static final int JPEG_QUALITY = 80;
    private static final String IMAGES_DIR_NAME = "images";
    private static final String CAMERA_DIR_NAME = "camera";

    private ImageCompressor() {
    }

    public static final class CompressionResult {
        public final String absolutePath;
        public final long fileSizeKb;
        public final int width;
        public final int height;

        public CompressionResult(String absolutePath, long fileSizeKb, int width, int height) {
            this.absolutePath = absolutePath;
            this.fileSizeKb = fileSizeKb;
            this.width = width;
            this.height = height;
        }
    }

    // Считаю кратный степени двойки коэффициент уменьшения для безопасного декодирования
    public static int calculateInSampleSize(int rawWidth, int rawHeight, int reqWidth, int reqHeight) {
        if (rawWidth <= 0 || rawHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) {
            return 1;
        }
        int inSampleSize = 1;
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            int halfHeight = rawHeight / 2;
            int halfWidth = rawWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        return calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);
    }

    // Выполняю двухэтапное декодирование, точное масштабирование до 1024 px и сохранение в JPEG 80%
    public static CompressionResult compressAndSave(Context context, Uri sourceUri) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        // Шаг 1: узнаю исходные размеры картинки, не загружая пиксели в оперативную память
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = resolver.openInputStream(sourceUri)) {
            if (is == null) {
                throw new IOException("Не удалось открыть поток изображения");
            }
            BitmapFactory.decodeStream(is, null, bounds);
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Файл не содержит корректного изображения");
        }

        // Шаг 2: декодирую с коэффициентом inSampleSize, чтобы сразу снизить расход памяти
        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = calculateInSampleSize(bounds, MAX_DIMENSION, MAX_DIMENSION);
        Bitmap decoded;
        try (InputStream is = resolver.openInputStream(sourceUri)) {
            if (is == null) {
                throw new IOException("Не удалось повторно открыть поток изображения");
            }
            decoded = BitmapFactory.decodeStream(is, null, decodeOpts);
        }

        if (decoded == null) {
            throw new IOException("Не удалось декодировать изображение");
        }

        try {
            return saveBitmapCompressed(context, decoded, "plant_" + System.currentTimeMillis());
        } finally {
            decoded.recycle();
        }
    }

    // Привожу изображение к лимиту по большей стороне и сохраняю в files/images
    public static CompressionResult saveBitmapCompressed(Context context, Bitmap source, String filePrefix)
            throws IOException {
        Bitmap scaled = scaleDownIfNeeded(source, MAX_DIMENSION);
        boolean createdScaledCopy = (scaled != source);

        try {
            File imagesDir = getImagesDir(context);
            File outFile = new File(imagesDir, filePrefix + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                boolean ok = scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
                if (!ok) {
                    throw new IOException("Ошибка сжатия изображения в JPEG");
                }
                fos.flush();
            }

            long sizeKb = Math.max(1L, (outFile.length() + 1023L) / 1024L);
            return new CompressionResult(
                    outFile.getAbsolutePath(),
                    sizeKb,
                    scaled.getWidth(),
                    scaled.getHeight()
            );
        } finally {
            if (createdScaledCopy) {
                scaled.recycle();
            }
        }
    }

    // Масштабирую картинку пропорционально, если одна из сторон превышает лимит
    private static Bitmap scaleDownIfNeeded(Bitmap source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();
        int largestSide = Math.max(width, height);
        if (largestSide <= maxSide) {
            return source;
        }
        float ratio = (float) maxSide / (float) largestSide;
        int targetW = Math.max(1, Math.round(width * ratio));
        int targetH = Math.max(1, Math.round(height * ratio));
        return Bitmap.createScaledBitmap(source, targetW, targetH, true);
    }

    public static File getImagesDir(Context context) {
        File dir = new File(context.getFilesDir(), IMAGES_DIR_NAME);
        if (!dir.exists()) {
            // Создаю внутреннюю папку images один раз при первом обращении
            dir.mkdirs();
        }
        return dir;
    }

    // Создаю временный файл в кэше для приёма кадра от системной камеры через FileProvider
    public static File createTempCameraFile(Context context) {
        File cameraDir = new File(context.getCacheDir(), CAMERA_DIR_NAME);
        if (!cameraDir.exists()) {
            cameraDir.mkdirs();
        }
        return new File(cameraDir, "capture_" + System.currentTimeMillis() + ".jpg");
    }

    // Удаляю локальный файл изображения вместе с записью, чтобы хранилище не засорялось
    public static boolean deleteImageFile(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }
        File file = new File(imagePath);
        return file.exists() && file.delete();
    }
}

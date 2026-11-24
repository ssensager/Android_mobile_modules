package com.example.lab4;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Полноэкранный просмотр фотографии растения с поддержкой приближения и панорамирования.
 */
public class ZoomImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String EXTRA_TITLE = "extra_title";

    private ZoomableImageView zoomImageView;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private Bitmap loadedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zoom_image);

        View zoomRoot = findViewById(R.id.zoomRoot);
        View zoomTopBar = findViewById(R.id.zoomTopBar);
        View tvZoomHint = findViewById(R.id.tvZoomHint);
        TextView tvZoomTitle = findViewById(R.id.tvZoomTitle);
        zoomImageView = findViewById(R.id.zoomImageView);

        int baseTop = zoomTopBar.getPaddingTop();
        int baseBottom = tvZoomHint.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(zoomRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            zoomTopBar.setPadding(
                    zoomTopBar.getPaddingLeft(),
                    baseTop + bars.top,
                    zoomTopBar.getPaddingRight(),
                    zoomTopBar.getPaddingBottom()
            );
            tvZoomHint.setPadding(
                    tvZoomHint.getPaddingLeft(),
                    tvZoomHint.getPaddingTop(),
                    tvZoomHint.getPaddingRight(),
                    baseBottom + bars.bottom
            );
            return insets;
        });

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        tvZoomTitle.setText(title != null ? title : getString(R.string.detail_title));

        findViewById(R.id.btnZoomBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnResetZoom).setOnClickListener(v -> zoomImageView.resetZoom());

        loadLocalPhoto(imagePath);
    }

    // Декодирую уже сжатую локальную копию в фоновом потоке и передаю в ZoomableImageView
    private void loadLocalPhoto(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            zoomImageView.setImageResource(R.drawable.placeholder_plant);
            return;
        }
        ioExecutor.execute(() -> {
            File file = new File(imagePath);
            Bitmap bmp = file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    if (bmp != null) {
                        bmp.recycle();
                    }
                    return;
                }
                if (bmp != null) {
                    loadedBitmap = bmp;
                    zoomImageView.setImageBitmap(bmp);
                } else {
                    zoomImageView.setImageResource(R.drawable.placeholder_plant);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadedBitmap != null && !loadedBitmap.isRecycled()) {
            loadedBitmap.recycle();
            loadedBitmap = null;
        }
        ioExecutor.shutdown();
    }
}

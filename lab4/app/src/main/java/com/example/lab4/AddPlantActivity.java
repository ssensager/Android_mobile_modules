package com.example.lab4;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Экран добавления нового растения.
 * Поддерживаю выбор фото из галереи без разрешений через PickVisualMedia и съёмку на камеру
 * через FileProvider, сжимая снимок в фоновом потоке с видимым индикатором прогресса.
 */
public class AddPlantActivity extends AppCompatActivity {

    private View addRoot;
    private View headerBar;
    private View scrollContent;
    private ImageView ivPhotoPreview;
    private View layoutCompressing;
    private TextView tvPhotoStatus;
    private MaterialButton btnPickGallery;
    private MaterialButton btnTakePhoto;
    private MaterialButton btnSavePlant;

    private TextInputLayout tilName;
    private TextInputEditText etName;
    private TextInputEditText etSpecies;
    private AutoCompleteTextView actvLight;
    private TextInputLayout tilWatering;
    private TextInputEditText etWatering;
    private TextInputEditText etTemperature;
    private TextInputEditText etNotes;

    private PlantStorage storage;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private String savedImagePath = "";
    private long savedFileSizeKb = 0L;
    private boolean isCompressing = false;
    private boolean savedSuccessfully = false;

    private File tempCameraFile;
    private Uri tempCameraUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    compressImageInBackground(uri, null);
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && tempCameraUri != null) {
                    compressImageInBackground(tempCameraUri, tempCameraFile);
                } else {
                    deleteTempCameraFile();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_plant);

        storage = new PlantStorage(this);
        bindViews();
        applyInsets();
        setupLightDropdown();
        setupListeners();
    }

    private void bindViews() {
        addRoot = findViewById(R.id.addRoot);
        headerBar = findViewById(R.id.headerBar);
        scrollContent = findViewById(R.id.scrollContent);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        layoutCompressing = findViewById(R.id.layoutCompressing);
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSavePlant = findViewById(R.id.btnSavePlant);

        tilName = findViewById(R.id.tilName);
        etName = findViewById(R.id.etName);
        etSpecies = findViewById(R.id.etSpecies);
        actvLight = findViewById(R.id.actvLight);
        tilWatering = findViewById(R.id.tilWatering);
        etWatering = findViewById(R.id.etWatering);
        etTemperature = findViewById(R.id.etTemperature);
        etNotes = findViewById(R.id.etNotes);
    }

    private void applyInsets() {
        int baseHeaderTop = headerBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(addRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            headerBar.setPadding(
                    headerBar.getPaddingLeft(),
                    baseHeaderTop + bars.top,
                    headerBar.getPaddingRight(),
                    headerBar.getPaddingBottom()
            );
            scrollContent.setPadding(
                    scrollContent.getPaddingLeft(),
                    scrollContent.getPaddingTop(),
                    scrollContent.getPaddingRight(),
                    Math.max(bars.bottom, ime.bottom)
            );
            return insets;
        });
    }

    private void setupLightDropdown() {
        String[] options = getResources().getStringArray(R.array.light_options);
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, options
        );
        actvLight.setAdapter(dropdownAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPickGallery.setOnClickListener(v -> {
            PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build();
            pickPhotoLauncher.launch(request);
        });

        btnTakePhoto.setOnClickListener(v -> launchCameraCapture());
        btnSavePlant.setOnClickListener(v -> savePlant());
    }

    private void launchCameraCapture() {
        try {
            tempCameraFile = ImageCompressor.createTempCameraFile(this);
            tempCameraUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", tempCameraFile
            );
            takePictureLauncher.launch(tempCameraUri);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            deleteTempCameraFile();
            Toast.makeText(this, R.string.error_camera_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    // Выполняю двухэтапное сжатие в фоновом потоке, чтобы интерфейс оставался отзывчивым
    private void compressImageInBackground(Uri sourceUri, File cameraFileToClean) {
        String previousSavedPath = savedImagePath;
        setCompressingState(true);

        ioExecutor.execute(() -> {
            try {
                ImageCompressor.CompressionResult result =
                        ImageCompressor.compressAndSave(getApplicationContext(), sourceUri);

                // Если пользователь заменил фото до сохранения карточки, удаляю старую черновую копию
                if (!previousSavedPath.isEmpty() && !previousSavedPath.equals(result.absolutePath)) {
                    ImageCompressor.deleteImageFile(previousSavedPath);
                }
                if (cameraFileToClean != null && cameraFileToClean.exists()) {
                    cameraFileToClean.delete();
                }

                runOnUiThread(() -> {
                    savedImagePath = result.absolutePath;
                    savedFileSizeKb = result.fileSizeKb;
                    setCompressingState(false);

                    Glide.with(this)
                            .load(new File(savedImagePath))
                            .centerCrop()
                            .placeholder(R.drawable.placeholder_plant)
                            .into(ivPhotoPreview);

                    tvPhotoStatus.setText(getString(
                            R.string.photo_ready_format,
                            result.fileSizeKb,
                            result.width,
                            result.height
                    ));
                });
            } catch (IOException e) {
                if (cameraFileToClean != null && cameraFileToClean.exists()) {
                    cameraFileToClean.delete();
                }
                runOnUiThread(() -> {
                    setCompressingState(false);
                    Toast.makeText(this, R.string.error_photo_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setCompressingState(boolean compressing) {
        isCompressing = compressing;
        layoutCompressing.setVisibility(compressing ? View.VISIBLE : View.GONE);
        btnPickGallery.setEnabled(!compressing);
        btnTakePhoto.setEnabled(!compressing);
        btnSavePlant.setEnabled(!compressing);
    }

    private void savePlant() {
        if (isCompressing) {
            return;
        }
        String name = textOf(etName);
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_name_required));
            etName.requestFocus();
            return;
        }
        tilName.setError(null);

        int wateringDays;
        try {
            wateringDays = Integer.parseInt(textOf(etWatering));
        } catch (NumberFormatException e) {
            wateringDays = -1;
        }
        if (wateringDays < 1 || wateringDays > 60) {
            tilWatering.setError(getString(R.string.error_watering_invalid));
            etWatering.requestFocus();
            return;
        }
        tilWatering.setError(null);

        String species = textOf(etSpecies);
        String light = actvLight.getText() != null && !actvLight.getText().toString().trim().isEmpty()
                ? actvLight.getText().toString().trim()
                : "Яркий рассеянный";
        String temperature = textOf(etTemperature).isEmpty() ? "20–25 °C" : textOf(etTemperature);
        String notes = textOf(etNotes);

        PlantItem newPlant = new PlantItem(
                UUID.randomUUID().toString(),
                name,
                species,
                light,
                wateringDays,
                temperature,
                notes,
                savedImagePath,
                -1L,
                savedFileSizeKb
        );

        btnSavePlant.setEnabled(false);
        ioExecutor.execute(() -> {
            storage.addPlant(newPlant);
            runOnUiThread(() -> {
                savedSuccessfully = true;
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void deleteTempCameraFile() {
        if (tempCameraFile != null && tempCameraFile.exists()) {
            tempCameraFile.delete();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Если пользователь ушёл без сохранения, убираю лишний сжатый файл, чтобы не копить мусор
        if (!savedSuccessfully && !isChangingConfigurations() && !savedImagePath.isEmpty()) {
            String orphanPath = savedImagePath;
            ioExecutor.execute(() -> ImageCompressor.deleteImageFile(orphanPath));
        }
        deleteTempCameraFile();
        ioExecutor.shutdown();
    }
}

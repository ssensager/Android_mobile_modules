package com.example.lab4;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Экран подробной карточки растения.
 * Показываю условия ухода, открываю фото в полноэкранном зуме и управляю серией событий полива в календаре.
 */
public class PlantDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLANT_ID = "extra_plant_id";
    public static final String EXTRA_DELETED_PLANT_ID = "extra_deleted_plant_id";

    private View detailRoot;
    private View headerBar;
    private View detailScroll;
    private View cardHeroPhoto;
    private ImageView ivDetailPhoto;
    private TextView tvDetailPhotoSize;
    private View badgeZoomHint;
    private TextView tvDetailName;
    private TextView tvDetailSpecies;
    private TextView tvDetailWatering;
    private TextView tvDetailLight;
    private TextView tvDetailTemperature;
    private TextView tvDetailNotes;

    private ImageView ivCalendarIcon;
    private TextView tvCalendarStatus;
    private ProgressBar progressCalendar;
    private MaterialButton btnCalendarAction;

    private PlantStorage storage;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private PlantItem currentPlant;
    private boolean dataChanged = false;

    private final ActivityResultLauncher<String[]> calendarPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean readOk = result.get(Manifest.permission.READ_CALENDAR);
                Boolean writeOk = result.get(Manifest.permission.WRITE_CALENDAR);
                if (Boolean.TRUE.equals(readOk) && Boolean.TRUE.equals(writeOk)) {
                    prepareSchedulePreview();
                } else {
                    // При отказе в разрешении отключаю только запись в календарь, остальной функционал работает
                    Snackbar.make(detailRoot, R.string.calendar_denied_snackbar, Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_plant_detail);

        storage = new PlantStorage(this);
        bindViews();
        applyInsets();
        setupListeners();

        String plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);
        loadPlant(plantId);
    }

    private void bindViews() {
        detailRoot = findViewById(R.id.detailRoot);
        headerBar = findViewById(R.id.headerBar);
        detailScroll = findViewById(R.id.detailScroll);
        cardHeroPhoto = findViewById(R.id.cardHeroPhoto);
        ivDetailPhoto = findViewById(R.id.ivDetailPhoto);
        tvDetailPhotoSize = findViewById(R.id.tvDetailPhotoSize);
        badgeZoomHint = findViewById(R.id.badgeZoomHint);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailSpecies = findViewById(R.id.tvDetailSpecies);
        tvDetailWatering = findViewById(R.id.tvDetailWatering);
        tvDetailLight = findViewById(R.id.tvDetailLight);
        tvDetailTemperature = findViewById(R.id.tvDetailTemperature);
        tvDetailNotes = findViewById(R.id.tvDetailNotes);

        ivCalendarIcon = findViewById(R.id.ivCalendarIcon);
        tvCalendarStatus = findViewById(R.id.tvCalendarStatus);
        progressCalendar = findViewById(R.id.progressCalendar);
        btnCalendarAction = findViewById(R.id.btnCalendarAction);
    }

    private void applyInsets() {
        int baseHeaderTop = headerBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(detailRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            headerBar.setPadding(
                    headerBar.getPaddingLeft(),
                    baseHeaderTop + bars.top,
                    headerBar.getPaddingRight(),
                    headerBar.getPaddingBottom()
            );
            detailScroll.setPadding(
                    detailScroll.getPaddingLeft(),
                    detailScroll.getPaddingTop(),
                    detailScroll.getPaddingRight(),
                    bars.bottom
            );
            return insets;
        });
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finishWithStatus());

        findViewById(R.id.btnDeleteDetail).setOnClickListener(v -> confirmDeletePlant());

        cardHeroPhoto.setOnClickListener(v -> {
            if (currentPlant != null && currentPlant.hasImage()) {
                Intent zoomIntent = new Intent(this, ZoomImageActivity.class);
                zoomIntent.putExtra(ZoomImageActivity.EXTRA_IMAGE_PATH, currentPlant.getImagePath());
                zoomIntent.putExtra(ZoomImageActivity.EXTRA_TITLE, currentPlant.getName());
                startActivity(zoomIntent);
            }
        });

        btnCalendarAction.setOnClickListener(v -> {
            if (currentPlant == null) {
                return;
            }
            if (currentPlant.hasCalendarEvent()) {
                removeCalendarSchedule();
            } else {
                requestCalendarAndSchedule();
            }
        });
    }

    private void loadPlant(String plantId) {
        ioExecutor.execute(() -> {
            PlantItem found = storage.findById(plantId);
            runOnUiThread(() -> {
                if (found == null) {
                    finish();
                    return;
                }
                currentPlant = found;
                renderPlant(found);
            });
        });
    }

    private void renderPlant(PlantItem plant) {
        tvDetailName.setText(plant.getName());
        tvDetailSpecies.setText(plant.getSpecies().isEmpty() ? "Комнатное растение" : plant.getSpecies());
        tvDetailWatering.setText(getString(R.string.badge_watering_format, plant.getWateringIntervalDays()));
        tvDetailLight.setText(plant.getLightNeeds());
        tvDetailTemperature.setText(plant.getTemperature());
        tvDetailNotes.setText(
                plant.getNotes().isEmpty() ? getString(R.string.detail_notes_empty) : plant.getNotes()
        );

        if (plant.hasImage()) {
            Glide.with(this)
                    .load(new File(plant.getImagePath()))
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_plant)
                    .error(R.drawable.placeholder_plant)
                    .into(ivDetailPhoto);
            tvDetailPhotoSize.setText(getString(R.string.detail_photo_info_format, plant.getFileSizeKb()));
            badgeZoomHint.setVisibility(View.VISIBLE);
        } else {
            ivDetailPhoto.setImageResource(R.drawable.placeholder_plant);
            tvDetailPhotoSize.setText(R.string.detail_photo_missing);
            badgeZoomHint.setVisibility(View.GONE);
        }

        renderCalendarBlock(plant);
    }

    private void renderCalendarBlock(PlantItem plant) {
        if (plant.hasCalendarEvent()) {
            ivCalendarIcon.setImageResource(R.drawable.ic_calendar_check);
            String rrule = CalendarHelper.buildRRule(plant.getWateringIntervalDays());
            tvCalendarStatus.setText(getString(
                    R.string.calendar_status_active_format,
                    plant.getWateringIntervalDays(),
                    rrule,
                    plant.getCalendarEventId()
            ));
            btnCalendarAction.setText(R.string.btn_remove_calendar);
            btnCalendarAction.setIconResource(R.drawable.ic_delete);
        } else {
            ivCalendarIcon.setImageResource(R.drawable.ic_calendar);
            tvCalendarStatus.setText(R.string.calendar_status_none);
            btnCalendarAction.setText(R.string.btn_schedule_calendar);
            btnCalendarAction.setIconResource(R.drawable.ic_calendar);
        }
    }

    private void requestCalendarAndSchedule() {
        if (CalendarHelper.hasCalendarPermissions(this)) {
            prepareSchedulePreview();
            return;
        }
        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.WRITE_CALENDAR
        );
        if (showRationale) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.calendar_rationale_title)
                    .setMessage(R.string.calendar_rationale_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.calendar_rationale_allow, (dialog, which) -> launchCalendarPermissionRequest())
                    .show();
        } else {
            launchCalendarPermissionRequest();
        }
    }

    private void launchCalendarPermissionRequest() {
        calendarPermissionLauncher.launch(new String[]{
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
        });
    }

    // Перед записью ищу доступный календарь и проверяю наложения в выбранном временном слоте
    private void prepareSchedulePreview() {
        if (currentPlant == null) {
            return;
        }
        setCalendarBusy(true);
        long startMs = CalendarHelper.computeNextMorningWateringMs();

        ioExecutor.execute(() -> {
            long writableCalendarId = CalendarHelper.findWritableCalendarId(getApplicationContext());
            List<CalendarHelper.CalendarOverlap> overlaps = CalendarHelper.findOverlappingEvents(
                    getApplicationContext(), startMs, CalendarHelper.EVENT_DURATION_MS
            );
            runOnUiThread(() -> {
                setCalendarBusy(false);
                if (writableCalendarId <= 0L) {
                    Snackbar.make(detailRoot, R.string.calendar_no_writable, Snackbar.LENGTH_LONG).show();
                    return;
                }
                showSchedulePreviewDialog(startMs, overlaps);
            });
        });
    }

    private void showSchedulePreviewDialog(long startMs, List<CalendarHelper.CalendarOverlap> overlaps) {
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy 'в' HH:mm", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String rrule = CalendarHelper.buildRRule(currentPlant.getWateringIntervalDays());

        StringBuilder msg = new StringBuilder();
        msg.append("Полив каждые ")
                .append(currentPlant.getWateringIntervalDays())
                .append(" дней, напоминание за ")
                .append(CalendarHelper.REMINDER_MINUTES)
                .append(" мин.\n\n")
                .append("Первый полив: ").append(dateFmt.format(new Date(startMs))).append(" (30 мин)\n")
                .append("Правило повторения: ").append(rrule);

        if (!overlaps.isEmpty()) {
            msg.append("\n\nВнимание: обнаружено наложение в календаре:");
            int shown = Math.min(overlaps.size(), 3);
            for (int i = 0; i < shown; i++) {
                CalendarHelper.CalendarOverlap ov = overlaps.get(i);
                msg.append("\n• ")
                        .append(ov.title)
                        .append(" (")
                        .append(timeFmt.format(new Date(ov.startMs)))
                        .append("–")
                        .append(timeFmt.format(new Date(ov.endMs)))
                        .append(")");
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calendar_preview_title)
                .setMessage(msg.toString())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.calendar_preview_confirm, (dialog, which) ->
                        createCalendarSeries(startMs)
                )
                .show();
    }

    private void createCalendarSeries(long startMs) {
        if (currentPlant == null) {
            return;
        }
        setCalendarBusy(true);
        PlantItem target = currentPlant;

        ioExecutor.execute(() -> {
            long eventId = CalendarHelper.createWateringEvent(getApplicationContext(), target, startMs);
            if (eventId > 0L) {
                PlantItem updated = target.withCalendarEventId(eventId);
                storage.updatePlant(updated);
                runOnUiThread(() -> {
                    setCalendarBusy(false);
                    currentPlant = updated;
                    dataChanged = true;
                    renderCalendarBlock(updated);
                    Snackbar.make(detailRoot, R.string.calendar_created_snackbar, Snackbar.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    setCalendarBusy(false);
                    Snackbar.make(detailRoot, R.string.calendar_create_error, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void removeCalendarSchedule() {
        if (currentPlant == null || !currentPlant.hasCalendarEvent()) {
            return;
        }
        if (!CalendarHelper.hasCalendarPermissions(this)) {
            Snackbar.make(detailRoot, R.string.calendar_denied_snackbar, Snackbar.LENGTH_LONG).show();
            return;
        }

        setCalendarBusy(true);
        PlantItem target = currentPlant;
        ioExecutor.execute(() -> {
            CalendarHelper.deleteEvent(getApplicationContext(), target.getCalendarEventId());
            PlantItem updated = target.withCalendarEventId(-1L);
            storage.updatePlant(updated);
            runOnUiThread(() -> {
                setCalendarBusy(false);
                currentPlant = updated;
                dataChanged = true;
                renderCalendarBlock(updated);
                Snackbar.make(detailRoot, R.string.calendar_removed_snackbar, Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    private void confirmDeletePlant() {
        if (currentPlant == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(getString(R.string.delete_dialog_message_format, currentPlant.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_DELETED_PLANT_ID, currentPlant.getId());
                    setResult(RESULT_OK, data);
                    finish();
                })
                .show();
    }

    private void setCalendarBusy(boolean busy) {
        progressCalendar.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnCalendarAction.setEnabled(!busy);
    }

    private void finishWithStatus() {
        if (dataChanged) {
            setResult(RESULT_OK);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}

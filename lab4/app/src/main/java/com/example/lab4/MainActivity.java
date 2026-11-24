package com.example.lab4;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Главный экран каталога «ФлораГид».
 * Управляю списком/сеткой растений, пустым состоянием, сохранением позиции прокрутки
 * и безопасным удалением с возможностью отменить действие через Snackbar.
 */
public class MainActivity extends AppCompatActivity implements PlantAdapter.OnPlantActionListener {

    private static final String STATE_SCROLL = "rv_scroll_state";

    private View rootCoordinator;
    private View headerBar;
    private TextView tvSubtitle;
    private ImageButton btnToggleLayout;
    private LinearProgressIndicator progressLoading;
    private RecyclerView rvPlants;
    private View emptyStateContainer;
    private ExtendedFloatingActionButton fabAddPlant;

    private PlantStorage storage;
    private PlantAdapter adapter;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final List<PlantItem> currentPlants = new ArrayList<>();
    private boolean isGridMode = false;
    private Parcelable savedScrollState;

    // Храню отложенно удалённую карточку, пока пользователь может нажать «Отменить» в Snackbar
    private PlantItem pendingDeletedPlant;
    private int pendingDeletedIndex = -1;

    private final ActivityResultLauncher<Intent> addPlantLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    reloadPlantsFromStorage(true);
                }
            });

    private final ActivityResultLauncher<Intent> detailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String deletedId = result.getData().getStringExtra(PlantDetailActivity.EXTRA_DELETED_PLANT_ID);
                    if (deletedId != null && !deletedId.isEmpty()) {
                        deletePlantWithUndoById(deletedId);
                        return;
                    }
                    // Обновляю данные без сброса позиции прокрутки
                    reloadPlantsFromStorage(false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            savedScrollState = savedInstanceState.getParcelable(STATE_SCROLL);
        }

        storage = new PlantStorage(this);
        isGridMode = storage.isGridMode();

        bindViews();
        applyEdgeToEdgeInsets();
        setupRecyclerView();
        setupListeners();
        updateLayoutToggleIcon();

        loadInitialCatalog();
    }

    private void bindViews() {
        rootCoordinator = findViewById(R.id.rootCoordinator);
        headerBar = findViewById(R.id.headerBar);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        btnToggleLayout = findViewById(R.id.btnToggleLayout);
        progressLoading = findViewById(R.id.progressLoading);
        rvPlants = findViewById(R.id.rvPlants);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        fabAddPlant = findViewById(R.id.fabAddPlant);
    }

    private void applyEdgeToEdgeInsets() {
        int baseHeaderTop = headerBar.getPaddingTop();
        int baseRvBottom = rvPlants.getPaddingBottom();
        ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fabAddPlant.getLayoutParams();
        int baseFabBottom = fabLp.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(rootCoordinator, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            headerBar.setPadding(
                    headerBar.getPaddingLeft(),
                    baseHeaderTop + bars.top,
                    headerBar.getPaddingRight(),
                    headerBar.getPaddingBottom()
            );
            rvPlants.setPadding(
                    rvPlants.getPaddingLeft(),
                    rvPlants.getPaddingTop(),
                    rvPlants.getPaddingRight(),
                    baseRvBottom + bars.bottom
            );
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fabAddPlant.getLayoutParams();
            lp.bottomMargin = baseFabBottom + bars.bottom;
            fabAddPlant.setLayoutParams(lp);
            return insets;
        });
    }

    private void setupRecyclerView() {
        adapter = new PlantAdapter(this, isGridMode);
        rvPlants.setHasFixedSize(false);
        applyLayoutManager(false);
        rvPlants.setAdapter(adapter);
    }

    private void applyLayoutManager(boolean preservePosition) {
        int firstVisible = 0;
        if (preservePosition && rvPlants.getLayoutManager() instanceof LinearLayoutManager) {
            firstVisible = ((LinearLayoutManager) rvPlants.getLayoutManager()).findFirstVisibleItemPosition();
        }
        RecyclerView.LayoutManager lm = isGridMode
                ? new GridLayoutManager(this, 2)
                : new LinearLayoutManager(this);
        rvPlants.setLayoutManager(lm);
        if (preservePosition) {
            adapter.setGridMode(isGridMode);
            rvPlants.setAdapter(adapter);
            if (firstVisible > 0) {
                lm.scrollToPosition(firstVisible);
            }
        }
    }

    private void setupListeners() {
        View.OnClickListener openAdd = v -> {
            commitPendingDeletion();
            addPlantLauncher.launch(new Intent(this, AddPlantActivity.class));
        };
        fabAddPlant.setOnClickListener(openAdd);
        findViewById(R.id.btnTopAdd).setOnClickListener(openAdd);
        findViewById(R.id.btnEmptyAdd).setOnClickListener(openAdd);

        btnToggleLayout.setOnClickListener(v -> {
            isGridMode = !isGridMode;
            storage.setGridMode(isGridMode);
            updateLayoutToggleIcon();
            applyLayoutManager(true);
        });
    }

    private void updateLayoutToggleIcon() {
        if (isGridMode) {
            btnToggleLayout.setImageResource(R.drawable.ic_view_list);
            btnToggleLayout.setContentDescription(getString(R.string.action_toggle_list));
        } else {
            btnToggleLayout.setImageResource(R.drawable.ic_view_grid);
            btnToggleLayout.setContentDescription(getString(R.string.action_toggle_grid));
        }
    }

    private void loadInitialCatalog() {
        progressLoading.setVisibility(View.VISIBLE);
        ioExecutor.execute(() -> {
            List<PlantItem> loaded = storage.loadOrSeedPlants();
            runOnUiThread(() -> {
                progressLoading.setVisibility(View.GONE);
                applyPlantList(loaded, false);
                if (savedScrollState != null && rvPlants.getLayoutManager() != null) {
                    rvPlants.getLayoutManager().onRestoreInstanceState(savedScrollState);
                    savedScrollState = null;
                }
            });
        });
    }

    private void reloadPlantsFromStorage(boolean scrollToTop) {
        ioExecutor.execute(() -> {
            List<PlantItem> loaded = storage.loadPlants();
            runOnUiThread(() -> applyPlantList(loaded, scrollToTop));
        });
    }

    private void applyPlantList(List<PlantItem> items, boolean scrollToTop) {
        currentPlants.clear();
        currentPlants.addAll(items);
        adapter.submitList(new ArrayList<>(currentPlants), () -> {
            if (scrollToTop && !currentPlants.isEmpty()) {
                rvPlants.scrollToPosition(0);
            }
        });
        updateEmptyAndSubtitle();
    }

    private void updateEmptyAndSubtitle() {
        boolean isEmpty = currentPlants.isEmpty();
        emptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvPlants.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        fabAddPlant.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            tvSubtitle.setText(R.string.catalog_subtitle_empty);
            return;
        }
        long totalKb = 0L;
        int photosCount = 0;
        for (PlantItem item : currentPlants) {
            if (item.getFileSizeKb() > 0L) {
                totalKb += item.getFileSizeKb();
                photosCount++;
            }
        }
        long avgKb = photosCount > 0 ? Math.round((double) totalKb / photosCount) : 0L;
        tvSubtitle.setText(getString(R.string.catalog_subtitle_format, currentPlants.size(), avgKb));
    }

    @Override
    public void onPlantClick(PlantItem plant) {
        commitPendingDeletion();
        Intent intent = new Intent(this, PlantDetailActivity.class);
        intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plant.getId());
        detailLauncher.launch(intent);
    }

    @Override
    public void onPlantDeleteClick(PlantItem plant) {
        // Перед удалением всегда спрашиваю подтверждение с безопасной кнопкой отмены
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(getString(R.string.delete_dialog_message_format, plant.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        deletePlantWithUndoById(plant.getId())
                )
                .show();
    }

    private void deletePlantWithUndoById(String plantId) {
        commitPendingDeletion();

        int index = -1;
        for (int i = 0; i < currentPlants.size(); i++) {
            if (currentPlants.get(i).getId().equals(plantId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }

        PlantItem removed = currentPlants.remove(index);
        pendingDeletedPlant = removed;
        pendingDeletedIndex = index;

        adapter.submitList(new ArrayList<>(currentPlants));
        updateEmptyAndSubtitle();

        List<PlantItem> snapshot = new ArrayList<>(currentPlants);
        ioExecutor.execute(() -> storage.savePlants(snapshot));

        Snackbar snackbar = Snackbar.make(
                rootCoordinator,
                getString(R.string.delete_snackbar_format, removed.getName()),
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.action_undo, v -> restorePendingDeletion());
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    commitPendingDeletion();
                }
            }
        });
        snackbar.show();
    }

    private void restorePendingDeletion() {
        if (pendingDeletedPlant == null) {
            return;
        }
        int insertIndex = Math.max(0, Math.min(pendingDeletedIndex, currentPlants.size()));
        currentPlants.add(insertIndex, pendingDeletedPlant);
        pendingDeletedPlant = null;
        pendingDeletedIndex = -1;

        adapter.submitList(new ArrayList<>(currentPlants));
        updateEmptyAndSubtitle();

        List<PlantItem> snapshot = new ArrayList<>(currentPlants);
        ioExecutor.execute(() -> storage.savePlants(snapshot));
    }

    // Окончательно удаляю локальное фото и связанное событие календаря после закрытия Snackbar
    private void commitPendingDeletion() {
        if (pendingDeletedPlant == null) {
            return;
        }
        PlantItem target = pendingDeletedPlant;
        pendingDeletedPlant = null;
        pendingDeletedIndex = -1;

        ioExecutor.execute(() -> {
            ImageCompressor.deleteImageFile(target.getImagePath());
            if (target.hasCalendarEvent()) {
                CalendarHelper.deleteEvent(getApplicationContext(), target.getCalendarEventId());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (rvPlants != null && rvPlants.getLayoutManager() != null) {
            outState.putParcelable(STATE_SCROLL, rvPlants.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            commitPendingDeletion();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}

package com.example.lab1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Главный экран калькулятора такси.
 * Параметры поездки в полях Activity не храню — всё уходит в MainViewModel,
 * а здесь я только подписываюсь на LiveData и обновляю интерфейс.
 */
public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;

    // Ставлю этот флаг на время программной синхронизации чипов, чтобы слушатели не сработали по кругу
    private boolean isSyncingUi = false;
    // Защита от быстрого двойного клика по кнопке вызова, чтобы не открылось два экрана подряд
    private boolean isLaunchingConfirm = false;

    private View mainRoot;
    private View bottomSummaryBar;
    private TextView tvDemandBadge;
    private PriceBreakdownView priceBreakdownView;
    private TextView tvSegmentHint;

    private TextView tvCarSubtitle;
    private ChipGroup cgCarClass;
    private TextView tvZoneSubtitle;
    private ChipGroup cgZone;
    private TextView tvDemandSubtitle;
    private MaterialButtonToggleGroup toggleDemand;

    private Chip chipChildSeat;
    private Chip chipLuggage;
    private Chip chipWaiting;

    private TextView tvPassengersHint;
    private TextView tvPassengersCount;
    private MaterialButton btnPassengersMinus;
    private MaterialButton btnPassengersPlus;

    private TextView tvTotalPrice;
    private TextView tvOrderSummary;
    private MaterialButton btnOrder;

    // Правая панель есть только в планшетной разметке sw600dp
    private boolean isTabletTwoPane = false;
    private TextView tvReceiptCar;
    private TextView tvReceiptZone;
    private TextView tvReceiptDemand;
    private TextView tvReceiptPassengers;
    private TextView tvReceiptOptions;
    private TextView tvReceiptBase;
    private TextView tvReceiptKm;
    private TextView tvReceiptMin;
    private TextView tvReceiptOptionsCost;
    private TextView tvReceiptTotal;

    private final int[] carChipIds = new int[]{
            R.id.chip_car_economy,
            R.id.chip_car_comfort,
            R.id.chip_car_comfort_plus,
            R.id.chip_car_business,
            R.id.chip_car_minivan
    };

    private final int[] zoneChipIds = new int[]{
            R.id.chip_zone_center,
            R.id.chip_zone_city,
            R.id.chip_zone_suburb,
            R.id.chip_zone_airport
    };

    private final int[] demandButtonIds = new int[]{
            R.id.btn_demand_10,
            R.id.btn_demand_13,
            R.id.btn_demand_16,
            R.id.btn_demand_20
    };

    // Жду результат со второго экрана ConfirmActivity через ActivityResultContracts
    private final ActivityResultLauncher<Intent> confirmLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                isLaunchingConfirm = false;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int orderId = result.getData().getIntExtra(ConfirmActivity.EXTRA_ORDER_ID, -1);
                    int total = result.getData().getIntExtra(ConfirmActivity.EXTRA_TOTAL, 0);
                    if (orderId > 0) {
                        viewModel.onOrderConfirmed(orderId);
                        showOrderConfirmedSnackbar(orderId, total);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        bindViews();
        applySystemWindowInsets();
        setupListeners();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLaunchingConfirm = false;
    }

    private void bindViews() {
        mainRoot = findViewById(R.id.main_root);
        bottomSummaryBar = findViewById(R.id.bottom_summary_bar);
        tvDemandBadge = findViewById(R.id.tv_demand_badge);
        priceBreakdownView = findViewById(R.id.price_breakdown_view);
        tvSegmentHint = findViewById(R.id.tv_segment_hint);

        tvCarSubtitle = findViewById(R.id.tv_car_subtitle);
        cgCarClass = findViewById(R.id.cg_car_class);
        tvZoneSubtitle = findViewById(R.id.tv_zone_subtitle);
        cgZone = findViewById(R.id.cg_zone);
        tvDemandSubtitle = findViewById(R.id.tv_demand_subtitle);
        toggleDemand = findViewById(R.id.toggle_demand);

        chipChildSeat = findViewById(R.id.chip_child_seat);
        chipLuggage = findViewById(R.id.chip_luggage);
        chipWaiting = findViewById(R.id.chip_waiting);

        tvPassengersHint = findViewById(R.id.tv_passengers_hint);
        tvPassengersCount = findViewById(R.id.tv_passengers_count);
        btnPassengersMinus = findViewById(R.id.btn_passengers_minus);
        btnPassengersPlus = findViewById(R.id.btn_passengers_plus);

        tvTotalPrice = findViewById(R.id.tv_total_price);
        tvOrderSummary = findViewById(R.id.tv_order_summary);
        btnOrder = findViewById(R.id.btn_order);

        // Если в разметке нашлась правая панель, значит экран открылся на планшете
        isTabletTwoPane = findViewById(R.id.tablet_confirm_pane) != null;
        if (isTabletTwoPane) {
            tvReceiptCar = findViewById(R.id.tv_receipt_car);
            tvReceiptZone = findViewById(R.id.tv_receipt_zone);
            tvReceiptDemand = findViewById(R.id.tv_receipt_demand);
            tvReceiptPassengers = findViewById(R.id.tv_receipt_passengers);
            tvReceiptOptions = findViewById(R.id.tv_receipt_options);
            tvReceiptBase = findViewById(R.id.tv_receipt_base);
            tvReceiptKm = findViewById(R.id.tv_receipt_km);
            tvReceiptMin = findViewById(R.id.tv_receipt_min);
            tvReceiptOptionsCost = findViewById(R.id.tv_receipt_options_cost);
            tvReceiptTotal = findViewById(R.id.tv_receipt_total);
        }
    }

    // Добавляю системные отступы сверху и снизу, чтобы статус-бар и панель навигации не залезали на кнопки
    private void applySystemWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
    }

    private void setupListeners() {
        cgCarClass.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isSyncingUi || checkedIds.isEmpty()) return;
            int index = findIndexById(carChipIds, checkedIds.get(0));
            if (index != -1) {
                viewModel.selectCarClass(index);
            }
        });

        cgZone.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isSyncingUi || checkedIds.isEmpty()) return;
            int index = findIndexById(zoneChipIds, checkedIds.get(0));
            if (index != -1) {
                viewModel.selectZone(index);
            }
        });

        toggleDemand.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isSyncingUi || !isChecked) return;
            int index = findIndexById(demandButtonIds, checkedId);
            if (index != -1) {
                viewModel.selectDemand(index);
            }
        });

        chipChildSeat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isSyncingUi) viewModel.setChildSeat(isChecked);
        });
        chipLuggage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isSyncingUi) viewModel.setLuggage(isChecked);
        });
        chipWaiting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isSyncingUi) viewModel.setWaiting(isChecked);
        });

        btnPassengersMinus.setOnClickListener(v -> viewModel.decrementPassengers());
        btnPassengersPlus.setOnClickListener(v -> viewModel.incrementPassengers());

        priceBreakdownView.setOnSegmentSelectedListener(
                segmentIndex -> viewModel.toggleSegmentSelection(segmentIndex)
        );

        btnOrder.setOnClickListener(v -> {
            if (isTabletTwoPane) {
                // На планшете экран подтверждения уже открыт справа, поэтому подтверждаю сразу
                int orderId = viewModel.generateNextOrderId();
                int total = viewModel.getCurrentBreakdown().totalCost;
                viewModel.onOrderConfirmed(orderId);
                showOrderConfirmedSnackbar(orderId, total);
            } else {
                openConfirmScreen();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getState().observe(this, state ->
                renderUi(state, viewModel.getCurrentBreakdown())
        );

        viewModel.getBreakdown().observe(this, breakdown ->
                renderUi(viewModel.getCurrentState(), breakdown)
        );
    }

    // Обновляю все элементы экрана из одного метода, чтобы после любого клика или поворота ничего не отставало
    private void renderUi(TaxiOrderState state, PriceBreakdown breakdown) {
        if (state == null || breakdown == null) return;

        TaxiTariffs.CarClass car = TaxiTariffs.CAR_CLASSES[state.carClassIndex];
        TaxiTariffs.TariffZone zone = TaxiTariffs.TARIFF_ZONES[state.zoneIndex];
        TaxiTariffs.DemandLevel demand = TaxiTariffs.DEMAND_LEVELS[state.demandIndex];

        isSyncingUi = true;
        cgCarClass.check(carChipIds[state.carClassIndex]);
        cgZone.check(zoneChipIds[state.zoneIndex]);
        toggleDemand.check(demandButtonIds[state.demandIndex]);
        chipChildSeat.setChecked(state.hasChildSeat);
        chipLuggage.setChecked(state.hasLuggage);
        chipWaiting.setChecked(state.hasWaiting);
        isSyncingUi = false;

        tvDemandBadge.setText(getString(R.string.demand_badge_fmt, demand.badge, demand.label));
        tvCarSubtitle.setText(car.shortDescription);
        tvZoneSubtitle.setText(zone.subtitle);
        tvDemandSubtitle.setText(getString(R.string.demand_badge_fmt, demand.badge, demand.label));

        tvPassengersCount.setText(String.valueOf(state.passengers));
        tvPassengersHint.setText(getString(R.string.passengers_hint_fmt, car.maxPassengers));
        btnPassengersMinus.setEnabled(state.passengers > 1);
        btnPassengersPlus.setEnabled(state.passengers < car.maxPassengers);

        priceBreakdownView.setBreakdown(breakdown, state.selectedSegmentIndex);
        if (state.selectedSegmentIndex >= 0 && state.selectedSegmentIndex < PriceBreakdownView.SEGMENT_COUNT) {
            String segName = priceBreakdownView.getSegmentName(state.selectedSegmentIndex);
            int segAmount = breakdown.amounts[state.selectedSegmentIndex];
            int segPct = Math.round(breakdown.shares[state.selectedSegmentIndex] * 100f);
            tvSegmentHint.setText(getString(R.string.breakdown_selected_fmt, segName, segAmount, segPct));
        } else {
            tvSegmentHint.setText(R.string.breakdown_hint_default);
        }

        tvTotalPrice.setText(getString(R.string.price_fmt, breakdown.totalCost));
        tvOrderSummary.setText(getString(R.string.summary_fmt, car.name, zone.name, state.passengers));

        if (isTabletTwoPane) {
            tvReceiptCar.setText(car.name);
            tvReceiptZone.setText(zone.name + " (" + zone.subtitle + ")");
            tvReceiptDemand.setText(getString(R.string.demand_badge_fmt, demand.badge, demand.label));
            tvReceiptPassengers.setText(String.valueOf(state.passengers));
            tvReceiptOptions.setText(formatOptionsList(state));
            tvReceiptBase.setText(getString(R.string.price_fmt, breakdown.baseCost));
            tvReceiptKm.setText(getString(R.string.price_fmt, breakdown.kmCost));
            tvReceiptMin.setText(getString(R.string.price_fmt, breakdown.minCost));
            tvReceiptOptionsCost.setText(getString(R.string.price_fmt, breakdown.optionsCost));
            tvReceiptTotal.setText(getString(R.string.price_fmt, breakdown.totalCost));
        }
    }

    // Передаю все выбранные параметры на экран подтверждения через Intent, без статических полей
    private void openConfirmScreen() {
        if (isLaunchingConfirm) return;
        isLaunchingConfirm = true;

        TaxiOrderState state = viewModel.getCurrentState();
        PriceBreakdown breakdown = viewModel.getCurrentBreakdown();
        TaxiTariffs.CarClass car = TaxiTariffs.CAR_CLASSES[state.carClassIndex];
        TaxiTariffs.TariffZone zone = TaxiTariffs.TARIFF_ZONES[state.zoneIndex];
        TaxiTariffs.DemandLevel demand = TaxiTariffs.DEMAND_LEVELS[state.demandIndex];

        Intent intent = new Intent(this, ConfirmActivity.class);
        intent.putExtra(ConfirmActivity.EXTRA_ORDER_ID, viewModel.generateNextOrderId());
        intent.putExtra(ConfirmActivity.EXTRA_CAR_NAME, car.name);
        intent.putExtra(ConfirmActivity.EXTRA_ZONE_TEXT, zone.name + " (" + zone.subtitle + ")");
        intent.putExtra(ConfirmActivity.EXTRA_DEMAND_TEXT,
                getString(R.string.demand_badge_fmt, demand.badge, demand.label));
        intent.putExtra(ConfirmActivity.EXTRA_PASSENGERS, state.passengers);
        intent.putExtra(ConfirmActivity.EXTRA_OPTIONS_TEXT, formatOptionsList(state));
        intent.putExtra(ConfirmActivity.EXTRA_BASE_COST, breakdown.baseCost);
        intent.putExtra(ConfirmActivity.EXTRA_KM_COST, breakdown.kmCost);
        intent.putExtra(ConfirmActivity.EXTRA_MIN_COST, breakdown.minCost);
        intent.putExtra(ConfirmActivity.EXTRA_OPTIONS_COST, breakdown.optionsCost);
        intent.putExtra(ConfirmActivity.EXTRA_TOTAL, breakdown.totalCost);

        confirmLauncher.launch(intent);
    }

    // Показываю Snackbar с номером заказа и возможностью отменить его, привязываю над нижней панелью
    private void showOrderConfirmedSnackbar(int orderId, int total) {
        Snackbar snackbar = Snackbar.make(
                mainRoot,
                getString(R.string.snackbar_order_confirmed, orderId, total),
                Snackbar.LENGTH_LONG
        );
        if (bottomSummaryBar != null) {
            snackbar.setAnchorView(bottomSummaryBar);
        }
        snackbar.setAction(R.string.snackbar_action_undo, v -> {
            viewModel.cancelLastOrder();
            Snackbar cancelBar = Snackbar.make(
                    mainRoot,
                    getString(R.string.snackbar_order_cancelled, orderId),
                    Snackbar.LENGTH_SHORT
            );
            if (bottomSummaryBar != null) {
                cancelBar.setAnchorView(bottomSummaryBar);
            }
            cancelBar.show();
        });
        snackbar.show();
    }

    private String formatOptionsList(TaxiOrderState state) {
        List<String> selected = new ArrayList<>();
        if (state.hasChildSeat) selected.add("Кресло");
        if (state.hasLuggage) selected.add("Багаж");
        if (state.hasWaiting) selected.add("Ожидание");
        if (selected.isEmpty()) {
            return getString(R.string.options_none);
        }
        return String.join(", ", selected);
    }

    private int findIndexById(int[] ids, int targetId) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == targetId) return i;
        }
        return -1;
    }
}

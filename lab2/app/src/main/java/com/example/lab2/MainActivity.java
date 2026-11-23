package com.example.lab2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Лабораторная работа №2 (Вариант 1): форма оформления доставки еды.
 *
 * Реализованы все 4 обязательных модуля и 3 бонуса:
 * 1. Инлайн-валидация с показом ошибок после потери фокуса и мгновенным снятием при исправлении.
 * 2. Сохранение и восстановление черновика через SharedPreferences (DraftStorage).
 * 3. Запрос разрешения POST_NOTIFICATIONS с предварительным объяснением и работой при отказе.
 * 4. Передача события в системный календарь через Intent.ACTION_INSERT с проверкой resolveActivity.
 * Бонусы: промокоды с пересчётом итога, плавная анимация ошибок + автопрокрутка, настраиваемое напоминание.
 */
public class MainActivity extends AppCompatActivity {

    private View rootView;
    private View bottomActionBar;
    private NestedScrollView formScroll;
    private ViewGroup formContainer;
    private View cardDraftBanner;

    private TextInputLayout tilName, tilPhone, tilAddress, tilPromo;
    private TextInputEditText etName, etPhone, etAddress, etComment, etPromo;
    private TextView tvSelectedDatetime, tvDatetimeError, tvPromoStatus, tvNotifStatus;
    private TextView tvTotalAmount, tvCheckFields;
    private ChipGroup cgPayment;
    private MaterialButton btnPickDatetime, btnRequestNotif, btnSubmit, btnClearDraft;

    private DraftStorage draftStorage;
    private long selectedDeliveryTimeMs = 0L;

    // Флаги «пользователь уже взаимодействовал с полем» — нужны, чтобы форма не краснела
    // сразу при открытии или на первой введённой букве (правило validate-input-after-interaction)
    private boolean nameTouched = false;
    private boolean phoneTouched = false;
    private boolean addressTouched = false;
    private boolean datetimeTouched = false;
    private boolean promoTouched = false;
    private boolean isRestoringDraft = false;

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("d MMMM, HH:mm", new Locale("ru"));

    // Запрос разрешения на уведомления во время выполнения (Android 13+)
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                updateNotificationStatusUi();
                if (granted) {
                    showSnackbar(getString(R.string.snackbar_notif_granted));
                } else {
                    // При отказе приложение продолжает работать полностью
                    showSnackbar(getString(R.string.snackbar_notif_denied));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        draftStorage = new DraftStorage(this);

        bindViews();
        applySystemInsets();
        restoreDraftIfPresent();
        setupListeners();
        updateNotificationStatusUi();
        updatePromoAndTotalUi();
        validateForm(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Сохраняем черновик при сворачивании, чтобы входящий звонок не стёр форму
        saveCurrentDraft();
    }

    private void bindViews() {
        rootView = findViewById(R.id.main);
        bottomActionBar = findViewById(R.id.bottom_action_bar);
        formScroll = findViewById(R.id.form_scroll);
        formContainer = findViewById(R.id.form_container);
        cardDraftBanner = findViewById(R.id.card_draft_banner);
        btnClearDraft = findViewById(R.id.btn_clear_draft);

        tilName = findViewById(R.id.til_name);
        tilPhone = findViewById(R.id.til_phone);
        tilAddress = findViewById(R.id.til_address);
        tilPromo = findViewById(R.id.til_promo);

        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        etComment = findViewById(R.id.et_comment);
        etPromo = findViewById(R.id.et_promo);

        tvSelectedDatetime = findViewById(R.id.tv_selected_datetime);
        tvDatetimeError = findViewById(R.id.tv_datetime_error);
        tvPromoStatus = findViewById(R.id.tv_promo_status);
        tvNotifStatus = findViewById(R.id.tv_notif_status);

        cgPayment = findViewById(R.id.cg_payment);
        btnPickDatetime = findViewById(R.id.btn_pick_datetime);
        btnRequestNotif = findViewById(R.id.btn_request_notif);

        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvCheckFields = findViewById(R.id.tv_check_fields);
        btnSubmit = findViewById(R.id.btn_submit);
    }

    private void applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
    }

    private void restoreDraftIfPresent() {
        DraftStorage.DraftData draft = draftStorage.loadDraft();
        if (!draft.hasSavedContent) {
            cardDraftBanner.setVisibility(View.GONE);
            return;
        }

        isRestoringDraft = true;
        etName.setText(draft.name);
        etPhone.setText(draft.phone);
        etAddress.setText(draft.address);
        etComment.setText(draft.comment);
        etPromo.setText(draft.promo);
        selectedDeliveryTimeMs = draft.deliveryTimeMs;
        updateDateTimeLabel();

        if (draft.paymentIndex == 1) {
            cgPayment.check(R.id.chip_pay_sbp);
        } else if (draft.paymentIndex == 2) {
            cgPayment.check(R.id.chip_pay_cash);
        } else {
            cgPayment.check(R.id.chip_pay_card);
        }

        cardDraftBanner.setVisibility(View.VISIBLE);
        isRestoringDraft = false;
    }

    private void setupListeners() {
        // Ошибки показываем при потере фокуса (а не с первой буквы),
        // чтобы форма воспринималась дружелюбно
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                nameTouched = true;
                validateForm(false);
            }
        });
        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                phoneTouched = true;
                validateForm(false);
            }
        });
        etAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                addressTouched = true;
                validateForm(false);
            }
        });
        etPromo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                promoTouched = true;
                validateForm(false);
            }
        });

        // При вводе текста: сразу снимаем ошибку, если поле стало валидным, пересчитываем промокод
        // и сохраняем черновик
        TextWatcher formWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isRestoringDraft) return;
                updatePromoAndTotalUi();
                validateForm(false);
                saveCurrentDraft();
            }
        };

        etName.addTextChangedListener(formWatcher);
        etPhone.addTextChangedListener(formWatcher);
        etAddress.addTextChangedListener(formWatcher);
        etComment.addTextChangedListener(formWatcher);
        etPromo.addTextChangedListener(formWatcher);

        cgPayment.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!isRestoringDraft) saveCurrentDraft();
        });

        btnPickDatetime.setOnClickListener(v -> openDateAndTimePickers());

        btnClearDraft.setOnClickListener(v -> {
            clearFormAndDraft();
            showSnackbar(getString(R.string.snackbar_draft_cleared));
        });

        btnRequestNotif.setOnClickListener(v -> requestNotificationsWithExplanation());

        // Бонус +1: подсказка «Что заполнить?» показывает все ошибки с плавной анимацией
        // и автоматически прокручивает экран к первому невалидному полю
        tvCheckFields.setOnClickListener(v -> {
            View firstInvalid = validateFormAndFindFirstInvalid();
            if (firstInvalid != null) {
                scrollToView(firstInvalid);
            }
        });

        btnSubmit.setOnClickListener(v -> {
            View firstInvalid = validateFormAndFindFirstInvalid();
            if (firstInvalid != null) {
                scrollToView(firstInvalid);
                return;
            }
            showConfirmOrderDialog();
        });
    }

    /**
     * Выбор даты и времени через MaterialDatePicker + MaterialTimePicker (не вручную строкой).
     */
    private void openDateAndTimePickers() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.label_delivery_datetime)
                .setSelection(selectedDeliveryTimeMs > 0
                        ? selectedDeliveryTimeMs
                        : MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints)
                .build();

        datePicker.addOnPositiveButtonClickListener(utcDateMs -> {
            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(utcDateMs);
            int year = utcCal.get(Calendar.YEAR);
            int month = utcCal.get(Calendar.MONTH);
            int day = utcCal.get(Calendar.DAY_OF_MONTH);

            Calendar now = Calendar.getInstance();
            int defaultHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24;

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(defaultHour)
                    .setMinute(0)
                    .setTitleText(R.string.label_delivery_datetime)
                    .build();

            timePicker.addOnPositiveButtonClickListener(view -> {
                Calendar localCal = Calendar.getInstance();
                localCal.set(year, month, day, timePicker.getHour(), timePicker.getMinute(), 0);
                localCal.set(Calendar.MILLISECOND, 0);

                selectedDeliveryTimeMs = localCal.getTimeInMillis();
                datetimeTouched = true;
                updateDateTimeLabel();
                validateForm(false);
                saveCurrentDraft();
            });

            timePicker.show(getSupportFragmentManager(), "delivery_time_picker");
        });

        datePicker.show(getSupportFragmentManager(), "delivery_date_picker");
    }

    /**
     * Единый метод валидации формы.
     * Возвращает true только если все обязательные поля корректны и промокод (если введён) известен.
     * Управляет доступностью кнопки «Оформить заказ» и плавно анимирует появление/снятие ошибок.
     */
    private boolean validateForm(boolean showAllErrors) {
        String name = getTextSafe(etName);
        String phone = getTextSafe(etPhone);
        String address = getTextSafe(etAddress);
        String promo = getTextSafe(etPromo);

        boolean nameOk = DeliveryValidator.isValidName(name);
        boolean phoneOk = DeliveryValidator.isValidPhone(phone);
        boolean addressOk = DeliveryValidator.isValidAddress(address);
        boolean dateSelected = selectedDeliveryTimeMs > 0;
        boolean dateNotPast = DeliveryValidator.isValidDeliveryTime(selectedDeliveryTimeMs, System.currentTimeMillis());
        boolean promoOk = DeliveryValidator.evaluatePromo(promo).isKnown;

        // Бонус +1: плавная анимация появления и скрытия текстов ошибок
        TransitionManager.beginDelayedTransition(formContainer);

        setFieldError(tilName, (showAllErrors || nameTouched) && !nameOk
                ? getString(R.string.error_name_empty) : null);
        setFieldError(tilPhone, (showAllErrors || phoneTouched) && !phoneOk
                ? getString(R.string.error_phone_invalid) : null);
        setFieldError(tilAddress, (showAllErrors || addressTouched) && !addressOk
                ? getString(R.string.error_address_empty) : null);
        setFieldError(tilPromo, (showAllErrors || promoTouched) && !promoOk
                ? getString(R.string.error_promo_unknown) : null);

        if ((showAllErrors || datetimeTouched) && !dateSelected) {
            tvDatetimeError.setText(R.string.error_datetime_required);
            tvDatetimeError.setVisibility(View.VISIBLE);
        } else if ((showAllErrors || datetimeTouched) && !dateNotPast) {
            tvDatetimeError.setText(R.string.error_datetime_past);
            tvDatetimeError.setVisibility(View.VISIBLE);
        } else {
            tvDatetimeError.setVisibility(View.GONE);
        }

        boolean allValid = nameOk && phoneOk && addressOk && dateSelected && dateNotPast && promoOk;
        btnSubmit.setEnabled(allValid);
        tvCheckFields.setVisibility(allValid ? View.GONE : View.VISIBLE);
        return allValid;
    }

    // Принудительно показываем все ошибки и возвращаем первое невалидное поле для автопрокрутки
    private View validateFormAndFindFirstInvalid() {
        nameTouched = true;
        phoneTouched = true;
        addressTouched = true;
        datetimeTouched = true;
        promoTouched = true;

        boolean valid = validateForm(true);
        if (valid) return null;

        if (!DeliveryValidator.isValidName(getTextSafe(etName))) return tilName;
        if (!DeliveryValidator.isValidPhone(getTextSafe(etPhone))) return tilPhone;
        if (!DeliveryValidator.isValidAddress(getTextSafe(etAddress))) return tilAddress;
        if (!DeliveryValidator.isValidDeliveryTime(selectedDeliveryTimeMs, System.currentTimeMillis())) {
            return findViewById(R.id.card_schedule);
        }
        if (!DeliveryValidator.evaluatePromo(getTextSafe(etPromo)).isKnown) return tilPromo;
        return null;
    }

    private void scrollToView(View target) {
        formScroll.post(() -> {
            int top = target.getTop();
            View parent = (View) target.getParent();
            while (parent != null && parent != formContainer) {
                top += parent.getTop();
                if (parent.getParent() instanceof View) {
                    parent = (View) parent.getParent();
                } else {
                    break;
                }
            }
            formScroll.smoothScrollTo(0, Math.max(0, top - 24));
            target.requestFocus();
        });
    }

    private void setFieldError(TextInputLayout layout, String errorText) {
        CharSequence current = layout.getError();
        if (errorText == null && current == null) return;
        if (errorText != null && current != null && errorText.contentEquals(current)) return;
        layout.setError(errorText);
    }

    private void updatePromoAndTotalUi() {
        String promo = getTextSafe(etPromo);
        DeliveryValidator.PromoResult result = DeliveryValidator.evaluatePromo(promo);
        int finalPrice = DeliveryValidator.calculateFinalPrice(promo);

        tvTotalAmount.setText(getString(R.string.price_fmt, finalPrice));
        if (result.discountRub > 0) {
            tvPromoStatus.setText(getString(R.string.promo_applied_fmt, result.discountRub, result.description));
            tvPromoStatus.setVisibility(View.VISIBLE);
        } else {
            tvPromoStatus.setVisibility(View.GONE);
        }
    }

    private void updateDateTimeLabel() {
        if (selectedDeliveryTimeMs > 0) {
            tvSelectedDatetime.setText(dateTimeFormat.format(new Date(selectedDeliveryTimeMs)));
        } else {
            tvSelectedDatetime.setText(R.string.datetime_not_selected);
        }
    }

    /**
     * Перед системным запросом разрешения объясняем пользователю, зачем оно нужно,
     * чтобы не пугать внезапным диалогом.
     */
    private void requestNotificationsWithExplanation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            updateNotificationStatusUi();
            showSnackbar(getString(R.string.snackbar_notif_granted));
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            updateNotificationStatusUi();
            showSnackbar(getString(R.string.snackbar_notif_granted));
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notif_rationale_title)
                .setMessage(R.string.notif_rationale_body)
                .setPositiveButton(R.string.notif_rationale_allow, (dialog, which) ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton(R.string.notif_rationale_later, (dialog, which) ->
                        showSnackbar(getString(R.string.snackbar_notif_denied)))
                .show();
    }

    private void updateNotificationStatusUi() {
        boolean granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            tvNotifStatus.setText(R.string.notif_status_granted);
            btnRequestNotif.setVisibility(View.GONE);
        } else {
            tvNotifStatus.setText(R.string.notif_status_denied);
            btnRequestNotif.setVisibility(View.VISIBLE);
        }
    }

    private void showConfirmOrderDialog() {
        String name = getTextSafe(etName).trim();
        String phone = getTextSafe(etPhone).trim();
        String address = getTextSafe(etAddress).trim();
        String comment = getTextSafe(etComment).trim();
        String promo = getTextSafe(etPromo).trim();
        String payment = getSelectedPaymentLabel();
        String dateText = dateTimeFormat.format(new Date(selectedDeliveryTimeMs));
        int finalPrice = DeliveryValidator.calculateFinalPrice(promo);

        StringBuilder summary = new StringBuilder();
        summary.append("Получатель: ").append(name).append("\n");
        summary.append("Телефон: ").append(phone).append("\n");
        summary.append("Адрес: ").append(address).append("\n");
        summary.append("Время доставки: ").append(dateText).append("\n");
        summary.append("Оплата: ").append(payment).append("\n");
        if (!comment.isEmpty()) {
            summary.append("Комментарий: ").append(comment).append("\n");
        }
        summary.append("\nИтого к оплате: ").append(finalPrice).append(" ₽");

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_confirm_title)
                .setMessage(summary.toString())
                .setPositiveButton(R.string.dialog_confirm_ok, (dialog, which) -> {
                    long savedTimeMs = selectedDeliveryTimeMs;
                    // Очищаем черновик после успешного оформления заказа
                    clearFormAndDraft();
                    // Предлагаем (не навязываем!) добавить доставку в календарь
                    showCalendarOfferDialog(name, address, payment, finalPrice, savedTimeMs);
                })
                .setNegativeButton(R.string.dialog_confirm_cancel, null)
                .show();
    }

    /**
     * После успешной отправки предлагаем добавить событие в календарь
     * с настраиваемым временем напоминания (Бонус +1: 15, 30 или 60 минут).
     */
    private void showCalendarOfferDialog(String name, String address, String payment,
                                         int finalPrice, long deliveryTimeMs) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calendar_offer, null);
        TextView tvBody = dialogView.findViewById(R.id.tv_dialog_success_body);
        ChipGroup cgReminder = dialogView.findViewById(R.id.cg_reminder);

        String dateText = dateTimeFormat.format(new Date(deliveryTimeMs));
        tvBody.setText(getString(R.string.dialog_success_body, address, dateText));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_success_title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_add_to_calendar, (dialog, which) -> {
                    int checkedId = cgReminder.getCheckedChipId();
                    int reminderMinutes = 15;
                    if (checkedId == R.id.chip_rem_30) reminderMinutes = 30;
                    else if (checkedId == R.id.chip_rem_60) reminderMinutes = 60;

                    openSystemCalendar(name, address, payment, finalPrice, deliveryTimeMs, reminderMinutes);
                })
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    /**
     * Передача события в системный календарь через Intent.ACTION_INSERT.
     * Разрешения не требуются, а проверка resolveActivity защищает от краша,
     * если приложения календаря нет на устройстве.
     */
    private void openSystemCalendar(String name, String address, String payment,
                                    int finalPrice, long beginTimeMs, int reminderMinutes) {
        // Время начала и окончания (1 час) передаём в миллисекундах, не строкой
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(beginTimeMs);
        long startMs = cal.getTimeInMillis();
        long endMs = startMs + 60L * 60L * 1000L;

        // Заголовок понятен сам по себе, даже если увидеть его в календаре через неделю
        String title = getString(R.string.calendar_title_fmt, address);
        String description = "Получатель: " + name
                + "\nОплата: " + payment + " (" + finalPrice + " ₽)"
                + "\nРекомендуемое напоминание: за " + reminderMinutes + " мин до доставки";

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, address)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
                .putExtra(CalendarContract.Events.HAS_ALARM, 1)
                .putExtra(CalendarContract.Reminders.MINUTES, reminderMinutes);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            showSnackbar(getString(R.string.snackbar_no_calendar));
        }
    }

    private void saveCurrentDraft() {
        draftStorage.saveDraft(
                getTextSafe(etName),
                getTextSafe(etPhone),
                getTextSafe(etAddress),
                getTextSafe(etComment),
                getTextSafe(etPromo),
                selectedDeliveryTimeMs,
                getSelectedPaymentIndex()
        );
    }

    private void clearFormAndDraft() {
        isRestoringDraft = true;
        draftStorage.clearDraft();
        etName.setText("");
        etPhone.setText("");
        etAddress.setText("");
        etComment.setText("");
        etPromo.setText("");
        selectedDeliveryTimeMs = 0L;
        cgPayment.check(R.id.chip_pay_card);

        nameTouched = false;
        phoneTouched = false;
        addressTouched = false;
        datetimeTouched = false;
        promoTouched = false;
        cardDraftBanner.setVisibility(View.GONE);
        isRestoringDraft = false;

        updateDateTimeLabel();
        updatePromoAndTotalUi();
        validateForm(false);
    }

    private int getSelectedPaymentIndex() {
        int id = cgPayment.getCheckedChipId();
        if (id == R.id.chip_pay_sbp) return 1;
        if (id == R.id.chip_pay_cash) return 2;
        return 0;
    }

    private String getSelectedPaymentLabel() {
        int index = getSelectedPaymentIndex();
        if (index == 1) return getString(R.string.pay_sbp);
        if (index == 2) return getString(R.string.pay_cash);
        return getString(R.string.pay_card);
    }

    private String getTextSafe(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        if (bottomActionBar != null) {
            snackbar.setAnchorView(bottomActionBar);
        }
        snackbar.show();
    }
}

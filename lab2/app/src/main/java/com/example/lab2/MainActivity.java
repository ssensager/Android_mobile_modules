package com.example.lab2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
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
 * Основной экран формы оформления доставки еды.
 * Здесь я собрал автоформатирование телефона, автоподсказки адресов с открытием на карте,
 * проверку всех полей с плавным появлением ошибок, сохранение черновика, запрос разрешения
 * на уведомления и добавление доставки в системный календарь.
 */
public class MainActivity extends AppCompatActivity {

    // Список популярных улиц Великого Новгорода для выпадающей подсказки адреса
    private static final String[] ADDRESS_SUGGESTIONS = new String[]{
            "ул. Большая Санкт-Петербургская, д. ",
            "ул. Газон, д. ",
            "ул. Ломоносова, д. ",
            "ул. Псковская, д. ",
            "пр. Мира, д. ",
            "ул. Фёдоровский Ручей, д. ",
            "ул. Чудинцева, д. ",
            "ул. Великая, д. ",
            "ул. Кочетова, д. ",
            "наб. Александра Невского, д. ",
            "ул. Зелинского, д. ",
            "пр. Александра Корсунова, д. ",
            "ул. Державина, д. ",
            "ул. Стратилатовская, д. ",
            "ул. Нехинская, д. "
    };

    private View rootView;
    private View bottomActionBar;
    private NestedScrollView formScroll;
    private ViewGroup formContainer;
    private View cardDraftBanner;

    private TextInputLayout tilName, tilPhone, tilAddress, tilPromo;
    private TextInputEditText etName, etPhone, etComment, etPromo;
    private MaterialAutoCompleteTextView etAddress;
    private TextView tvSelectedDatetime, tvDatetimeError, tvPromoStatus, tvNotifStatus;
    private TextView tvTotalAmount, tvCheckFields;
    private ChipGroup cgPayment;
    private MaterialButton btnPickDatetime, btnRequestNotif, btnSubmit, btnClearDraft, btnOpenMap;

    private DraftStorage draftStorage;
    private long selectedDeliveryTimeMs = 0L;

    // Отмечаю поля, с которых пользователь уже ушёл, чтобы ошибки не мелькали с первой введённой буквы
    private boolean nameTouched = false;
    private boolean phoneTouched = false;
    private boolean addressTouched = false;
    private boolean datetimeTouched = false;
    private boolean promoTouched = false;
    private boolean isRestoringDraft = false;
    // Флаг нужен, чтобы автоформатирование телефона не запускало TextWatcher рекурсивно
    private boolean isFormattingPhone = false;

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("d MMMM, HH:mm", new Locale("ru"));

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                updateNotificationStatusUi();
                if (granted) {
                    showSnackbar(getString(R.string.snackbar_notif_granted));
                } else {
                    showSnackbar(getString(R.string.snackbar_notif_denied));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        draftStorage = new DraftStorage(this);

        bindViews();
        setupAddressAutocomplete();
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
        // Сохраняю черновик при сворачивании экрана, чтобы введённые поля не пропали
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
        btnOpenMap = findViewById(R.id.btn_open_map);
        btnRequestNotif = findViewById(R.id.btn_request_notif);

        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvCheckFields = findViewById(R.id.tv_check_fields);
        btnSubmit = findViewById(R.id.btn_submit);
    }

    // Подключаю выпадающий список подсказок улиц к полю адреса
    private void setupAddressAutocomplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                ADDRESS_SUGGESTIONS
        );
        etAddress.setAdapter(adapter);
        etAddress.setThreshold(1);
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
        etPhone.setText(PhoneMaskFormatter.formatRussianPhone(draft.phone));
        etAddress.setText(draft.address, false);
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
        // Показываю ошибку только после потери фокуса, чтобы не мешать пока человек печатает
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

        // Отдельный TextWatcher на телефон: сам подставляет +7 (XXX) XXX-XX-XX по мере ввода цифр
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isRestoringDraft || isFormattingPhone) return;
                isFormattingPhone = true;
                String formatted = PhoneMaskFormatter.formatRussianPhone(s.toString());
                if (!formatted.equals(s.toString())) {
                    s.replace(0, s.length(), formatted);
                }
                isFormattingPhone = false;

                validateForm(false);
                saveCurrentDraft();
            }
        });

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
        etAddress.addTextChangedListener(formWatcher);
        etComment.addTextChangedListener(formWatcher);
        etPromo.addTextChangedListener(formWatcher);

        cgPayment.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!isRestoringDraft) saveCurrentDraft();
        });

        btnPickDatetime.setOnClickListener(v -> openDateAndTimePickers());
        btnOpenMap.setOnClickListener(v -> openAddressInMaps());

        btnClearDraft.setOnClickListener(v -> {
            clearFormAndDraft();
            showSnackbar(getString(R.string.snackbar_draft_cleared));
        });

        btnRequestNotif.setOnClickListener(v -> requestNotificationsWithExplanation());

        // По нажатию подсвечиваю незаполненные поля и плавно прокручиваю к первому из них
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

    // Открываю введённый адрес в Google Картах или другом картографическом приложении через geo-Intent
    private void openAddressInMaps() {
        String address = getTextSafe(etAddress).trim();
        String query = address.isEmpty()
                ? "Великий Новгород"
                : "Великий Новгород, " + address;
        Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            showSnackbar(getString(R.string.snackbar_no_maps));
        }
    }

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

    // Единая проверка всей формы: выставляю/снимаю ошибки и включаю кнопку оформления только когда всё верно
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

    // Сначала показываю короткое объяснение, зачем нужны уведомления, и только потом кидаю системный запрос
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
                    clearFormAndDraft();
                    showCalendarOfferDialog(name, address, payment, finalPrice, savedTimeMs);
                })
                .setNegativeButton(R.string.dialog_confirm_cancel, null)
                .show();
    }

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

    // Передаю событие в календарь через Intent, заранее проверяю resolveActivity, чтобы не упасть без приложения
    private void openSystemCalendar(String name, String address, String payment,
                                    int finalPrice, long beginTimeMs, int reminderMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(beginTimeMs);
        long startMs = cal.getTimeInMillis();
        long endMs = startMs + 60L * 60L * 1000L;

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
        etAddress.setText("", false);
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

    private String getTextSafe(EditText editText) {
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

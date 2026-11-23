package com.example.lab2;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Хранилище черновика формы на базе SharedPreferences («ключ-значение»).
 * Используем именно SharedPreferences, а не SQLite, потому что черновик — это
 * всего один набор строк и чисел, а сохранение через apply() работает асинхронно
 * и не тормозит главный поток интерфейса.
 */
public class DraftStorage {

    private static final String PREFS_NAME = "food_delivery_draft_prefs";
    private static final String KEY_NAME = "draft_name";
    private static final String KEY_PHONE = "draft_phone";
    private static final String KEY_ADDRESS = "draft_address";
    private static final String KEY_COMMENT = "draft_comment";
    private static final String KEY_PROMO = "draft_promo";
    private static final String KEY_DATETIME_MS = "draft_datetime_ms";
    private static final String KEY_PAYMENT_INDEX = "draft_payment_index";
    private static final String KEY_HAS_DRAFT = "draft_exists";

    public static final class DraftData {
        public final String name;
        public final String phone;
        public final String address;
        public final String comment;
        public final String promo;
        public final long deliveryTimeMs;
        public final int paymentIndex;
        public final boolean hasSavedContent;

        public DraftData(String name, String phone, String address, String comment,
                         String promo, long deliveryTimeMs, int paymentIndex, boolean hasSavedContent) {
            this.name = name;
            this.phone = phone;
            this.address = address;
            this.comment = comment;
            this.promo = promo;
            this.deliveryTimeMs = deliveryTimeMs;
            this.paymentIndex = paymentIndex;
            this.hasSavedContent = hasSavedContent;
        }
    }

    private final SharedPreferences prefs;

    public DraftStorage(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveDraft(String name, String phone, String address, String comment,
                          String promo, long deliveryTimeMs, int paymentIndex) {
        boolean hasAnyData = !name.trim().isEmpty()
                || !phone.trim().isEmpty()
                || !address.trim().isEmpty()
                || !comment.trim().isEmpty()
                || !promo.trim().isEmpty()
                || deliveryTimeMs > 0;

        prefs.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_PHONE, phone)
                .putString(KEY_ADDRESS, address)
                .putString(KEY_COMMENT, comment)
                .putString(KEY_PROMO, promo)
                .putLong(KEY_DATETIME_MS, deliveryTimeMs)
                .putInt(KEY_PAYMENT_INDEX, paymentIndex)
                .putBoolean(KEY_HAS_DRAFT, hasAnyData)
                .apply(); // apply() пишет на диск в фоне и не блокирует UI
    }

    public DraftData loadDraft() {
        boolean exists = prefs.getBoolean(KEY_HAS_DRAFT, false);
        return new DraftData(
                prefs.getString(KEY_NAME, ""),
                prefs.getString(KEY_PHONE, ""),
                prefs.getString(KEY_ADDRESS, ""),
                prefs.getString(KEY_COMMENT, ""),
                prefs.getString(KEY_PROMO, ""),
                prefs.getLong(KEY_DATETIME_MS, 0L),
                prefs.getInt(KEY_PAYMENT_INDEX, 0),
                exists
        );
    }

    public void clearDraft() {
        prefs.edit().clear().apply();
    }
}

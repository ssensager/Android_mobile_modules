package com.example.lab2;

/**
 * Простой класс проверки полей формы доставки и расчёта скидки по промокоду.
 * Вынесли проверки в отдельные чистые методы (без привязки к Android UI),
 * чтобы код был максимально понятным и его можно было легко проверить модульными тестами.
 */
public final class DeliveryValidator {

    public static final int BASE_ORDER_PRICE = 1290;

    // Результат проверки промокода (размер скидки и описание)
    public static final class PromoResult {
        public final boolean isKnown;
        public final int discountRub;
        public final String description;

        public PromoResult(boolean isKnown, int discountRub, String description) {
            this.isKnown = isKnown;
            this.discountRub = discountRub;
            this.description = description;
        }
    }

    public static boolean isValidName(String rawName) {
        if (rawName == null) return false;
        String trimmed = rawName.trim();
        return trimmed.length() >= 2;
    }

    // Проверяем номер телефона: разрешаем пробелы, скобки, дефисы и плюс,
    // но требуем от 10 до 15 реальных цифр (например, +7 900 123-45-67)
    public static boolean isValidPhone(String rawPhone) {
        if (rawPhone == null) return false;
        String trimmed = rawPhone.trim();
        if (trimmed.isEmpty()) return false;

        int digitCount = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                digitCount++;
            } else if (c != '+' && c != '-' && c != ' ' && c != '(' && c != ')') {
                return false;
            }
        }
        return digitCount >= 10 && digitCount <= 15;
    }

    public static boolean isValidAddress(String rawAddress) {
        if (rawAddress == null) return false;
        return rawAddress.trim().length() >= 5;
    }

    // Дата доставки должна быть выбрана и находиться в будущем (с запасом 1 минута)
    public static boolean isValidDeliveryTime(long deliveryTimeMs, long nowMs) {
        return deliveryTimeMs > 0 && deliveryTimeMs >= (nowMs - 60_000L);
    }

    /**
     * Бонус +1: проверка промокода с пересчётом итоговой суммы.
     * Пустое поле считается валидным (промокод необязателен).
     */
    public static PromoResult evaluatePromo(String rawPromo) {
        if (rawPromo == null || rawPromo.trim().isEmpty()) {
            return new PromoResult(true, 0, "");
        }
        String code = rawPromo.trim().toUpperCase();
        if ("FOOD10".equals(code)) {
            return new PromoResult(true, Math.round(BASE_ORDER_PRICE * 0.10f), "−10%");
        }
        if ("NOVSU20".equals(code)) {
            return new PromoResult(true, Math.round(BASE_ORDER_PRICE * 0.20f), "−20%");
        }
        if ("FREE".equals(code)) {
            return new PromoResult(true, 250, "скидка 250 ₽");
        }
        return new PromoResult(false, 0, "");
    }

    public static int calculateFinalPrice(String rawPromo) {
        PromoResult promo = evaluatePromo(rawPromo);
        return Math.max(0, BASE_ORDER_PRICE - promo.discountRub);
    }

    private DeliveryValidator() {
    }
}

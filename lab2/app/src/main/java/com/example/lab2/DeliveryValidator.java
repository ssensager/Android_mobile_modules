package com.example.lab2;

/**
 * Здесь собрал все проверки полей формы и расчёт скидки по промокоду.
 * Вынес их отдельно от Activity, чтобы код был чище и легко проверялся тестами.
 */
public final class DeliveryValidator {

    public static final int BASE_ORDER_PRICE = 1290;

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
        return rawName.trim().length() >= 2;
    }

    // Проверяю телефон по количеству реальных цифр (формат +7 (900) 111-11-11 даёт ровно 11 цифр)
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
        return digitCount == 11;
    }

    public static boolean isValidAddress(String rawAddress) {
        if (rawAddress == null) return false;
        return rawAddress.trim().length() >= 5;
    }

    // Не даю оформить доставку на время в прошлом (даю небольшой запас в 1 минуту)
    public static boolean isValidDeliveryTime(long deliveryTimeMs, long nowMs) {
        return deliveryTimeMs > 0 && deliveryTimeMs >= (nowMs - 60_000L);
    }

    // Проверяю промокод и сразу считаю скидку; пустое поле пропускаю, так как промокод необязателен
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

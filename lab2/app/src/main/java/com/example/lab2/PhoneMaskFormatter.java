package com.example.lab2;

/**
 * Вынес логику автоформатирования телефона в отдельный класс, чтобы было проще тестировать.
 * Пользователь просто вводит цифры (хоть с 9, хоть с 8 или +7), а я сам собираю строку
 * вида +7 (900) 111-11-11.
 */
public final class PhoneMaskFormatter {

    public static String formatRussianPhone(String rawInput) {
        if (rawInput == null) return "";

        // Вытаскиваю только цифры из всего, что ввёл или вставил пользователь
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < rawInput.length(); i++) {
            char c = rawInput.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }

        if (digits.length() == 0) {
            return "";
        }

        // Если пользователь начал с 7 или 8, убираю первую цифру кода страны — +7 подставлю сам.
        // Если нажали Backspace и осталась одна "7" от префикса "+7", очищаю поле полностью.
        if (digits.length() == 1 && (digits.charAt(0) == '7' || digits.charAt(0) == '8')) {
            if (rawInput.trim().equals("+7") || rawInput.trim().equals("+7 (") || rawInput.trim().equals("7")) {
                return "";
            }
            return "+7 (";
        }

        String national;
        if (digits.charAt(0) == '7' || digits.charAt(0) == '8') {
            national = digits.substring(1);
        } else {
            national = digits.toString();
        }

        // Ограничиваю номер 10 цифрами после +7
        if (national.length() > 10) {
            national = national.substring(0, 10);
        }

        StringBuilder out = new StringBuilder("+7");
        if (national.isEmpty()) {
            return "";
        }
        out.append(" (");
        int part1End = Math.min(3, national.length());
        out.append(national, 0, part1End);

        if (national.length() > 3) {
            out.append(") ");
            int part2End = Math.min(6, national.length());
            out.append(national, 3, part2End);
        }
        if (national.length() > 6) {
            out.append("-");
            int part3End = Math.min(8, national.length());
            out.append(national, 6, part3End);
        }
        if (national.length() > 8) {
            out.append("-");
            out.append(national, 8, national.length());
        }
        return out.toString();
    }

    private PhoneMaskFormatter() {
    }
}

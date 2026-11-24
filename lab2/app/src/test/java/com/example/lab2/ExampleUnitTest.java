package com.example.lab2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Мои модульные тесты для проверки валидации, маски телефона и расчёта промокодов.
 */
public class ExampleUnitTest {

    @Test
    public void phoneMaskFormatsDigitsAutomatically() {
        assertEquals("+7 (900) 111-11-11", PhoneMaskFormatter.formatRussianPhone("9001111111"));
        assertEquals("+7 (900) 111-11-11", PhoneMaskFormatter.formatRussianPhone("89001111111"));
        assertEquals("+7 (900) 111-11-11", PhoneMaskFormatter.formatRussianPhone("+79001111111"));
        assertEquals("+7 (900", PhoneMaskFormatter.formatRussianPhone("900"));
        assertEquals("", PhoneMaskFormatter.formatRussianPhone(""));
    }

    @Test
    public void nameAndAddressValidationRejectSpacesOnly() {
        assertFalse(DeliveryValidator.isValidName("   "));
        assertFalse(DeliveryValidator.isValidName("А"));
        assertTrue(DeliveryValidator.isValidName("Алексей"));

        assertFalse(DeliveryValidator.isValidAddress("   "));
        assertFalse(DeliveryValidator.isValidAddress("дом"));
        assertTrue(DeliveryValidator.isValidAddress("ул. Газон, д. 5, кв. 12"));
    }

    @Test
    public void phoneValidationRequiresFullElevenDigits() {
        assertTrue(DeliveryValidator.isValidPhone("+7 (900) 123-45-67"));
        assertTrue(DeliveryValidator.isValidPhone("89001234567"));
        assertFalse(DeliveryValidator.isValidPhone("+7 (900) 123"));
        assertFalse(DeliveryValidator.isValidPhone("abc-def-ghij"));
    }

    @Test
    public void promoCodeRecalculatesFinalPrice() {
        assertEquals(1290, DeliveryValidator.calculateFinalPrice(""));
        assertEquals(1161, DeliveryValidator.calculateFinalPrice("food10"));
        assertEquals(1040, DeliveryValidator.calculateFinalPrice("FREE"));
        assertFalse(DeliveryValidator.evaluatePromo("UNKNOWN").isKnown);
    }
}

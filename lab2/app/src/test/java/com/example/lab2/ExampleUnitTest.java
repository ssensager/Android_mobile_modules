package com.example.lab2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Модульные тесты валидации формы доставки еды и расчёта промокодов.
 */
public class ExampleUnitTest {

    @Test
    public void nameAndAddressValidationRejectSpacesOnly() {
        assertFalse(DeliveryValidator.isValidName("   "));
        assertFalse(DeliveryValidator.isValidName("А"));
        assertTrue(DeliveryValidator.isValidName("Алексей"));

        assertFalse(DeliveryValidator.isValidAddress("   "));
        assertFalse(DeliveryValidator.isValidAddress("дом"));
        assertTrue(DeliveryValidator.isValidAddress("ул. Ленина, д. 5, кв. 12"));
    }

    @Test
    public void phoneValidationAcceptsFormattedNumbersAndRejectsGarbage() {
        assertTrue(DeliveryValidator.isValidPhone("+7 900 123-45-67"));
        assertTrue(DeliveryValidator.isValidPhone("89001234567"));
        assertFalse(DeliveryValidator.isValidPhone("12345"));
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

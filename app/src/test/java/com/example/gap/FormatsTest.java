package com.example.gap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FormatsTest {

    @Test
    public void money_wholeAmountsHaveNoDecimals() {
        assertEquals("₹40", Formats.money(40));
        assertEquals("₹0", Formats.money(0));
    }

    @Test
    public void money_fractionalAmountsHaveTwoDecimals() {
        assertEquals("₹40.50", Formats.money(40.5));
        assertEquals("₹0.10", Formats.money(0.1 + 0.0000001));
    }
}

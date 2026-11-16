package com.example.gap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AddressTest {

    @Test
    public void withoutPin_hasNoMapLink() {
        Address a = new Address("Home", "12", "MG Road", "Bandra", "Mumbai", "MH", "400050", "", true);
        assertFalse(a.hasLocation());
        assertNull(a.getMapsUrl());
        assertEquals(a.toString(), a.toOrderText());
    }

    @Test
    public void withPin_orderTextIncludesMapLink() {
        Address a = new Address(0, "Home", "12", "", "Bandra", "Mumbai", "MH", "400050", "", true, 19.0596, 72.8295);
        assertTrue(a.hasLocation());
        assertEquals("https://maps.google.com/?q=19.059600,72.829500", a.getMapsUrl());
        assertTrue(a.toOrderText().endsWith("https://maps.google.com/?q=19.059600,72.829500"));
        assertTrue(a.getFullAddress().contains("Pinned on map"));
    }

    @Test
    public void halfAPin_isTreatedAsNoPin() {
        Address a = new Address(0, "Home", "12", "", "Bandra", "Mumbai", "MH", "400050", "", true, 19.0, null);
        assertFalse(a.hasLocation());
        assertNull(a.getLatitude());
    }
}

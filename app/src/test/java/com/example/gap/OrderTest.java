package com.example.gap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class OrderTest {

    @Test
    public void newOrder_isPlacedAndTotalsItsLines() {
        Order order = new Order(Arrays.asList(
                new GroceryItem("Potato", 25, "kg", 0, 2),
                new GroceryItem("Lemon", 5, "piece", 0, 3)),
                "Home", Order.PAYMENT_COD, null);

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(65.0, order.getTotal(), 0.0001);
        assertFalse(order.isPaidOnline());
    }

    @Test
    public void upiOrder_isPaidOnline() {
        Order order = new Order(Arrays.asList(new GroceryItem("Potato", 25, "kg", 0, 1)),
                "Home", Order.PAYMENT_UPI, "123456789012");
        assertTrue(order.isPaidOnline());
        assertEquals("123456789012", order.getPaymentRef());
    }

    @Test
    public void status_transitionRules() {
        assertTrue(OrderStatus.PLACED.canCancel());
        assertFalse(OrderStatus.OUT_FOR_DELIVERY.canCancel());
        assertFalse(OrderStatus.DELIVERED.canCancel());
        assertFalse(OrderStatus.CANCELLED.canCancel());

        assertTrue(OrderStatus.PLACED.canMarkDelivered());
        assertTrue(OrderStatus.OUT_FOR_DELIVERY.canMarkDelivered());
        assertFalse(OrderStatus.DELIVERED.canMarkDelivered());
        assertFalse(OrderStatus.CANCELLED.canMarkDelivered());
    }

    @Test
    public void status_fromDbFallsBackToPlaced() {
        assertEquals(OrderStatus.CANCELLED, OrderStatus.fromDb("CANCELLED"));
        assertEquals(OrderStatus.PLACED, OrderStatus.fromDb(null));
        assertEquals(OrderStatus.PLACED, OrderStatus.fromDb("garbage"));
    }
}

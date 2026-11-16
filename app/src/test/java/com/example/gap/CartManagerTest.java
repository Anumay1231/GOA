package com.example.gap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class CartManagerTest {

    private CartManager cart;
    private final GroceryItem potato = new GroceryItem("Potato", 25, "kg", 0);
    private final GroceryItem lemon = new GroceryItem("Lemon", 5, "piece", 0);

    @Before
    public void setUp() {
        cart = new CartManager();
    }

    @Test
    public void setQuantity_addsAndUpdatesWithoutMutatingCatalogItem() {
        cart.setQuantity(potato, 2);
        cart.setQuantity(potato, 3);

        assertEquals(3, cart.getQuantity("Potato"));
        assertEquals(1, cart.getItems().size());
        assertEquals(0, potato.getQuantity());
    }

    @Test
    public void setQuantityZero_removesItem() {
        cart.setQuantity(potato, 2);
        cart.setQuantity(potato, 0);

        assertFalse(cart.contains("Potato"));
        assertTrue(cart.isEmpty());
    }

    @Test
    public void total_sumsLineTotals() {
        cart.setQuantity(potato, 2); // 50
        cart.setQuantity(lemon, 3);  // 15
        assertEquals(65.0, cart.getTotal(), 0.0001);

        cart.remove("Potato");
        assertEquals(15.0, cart.getTotal(), 0.0001);

        cart.clear();
        assertTrue(cart.isEmpty());
    }
}

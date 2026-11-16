package com.example.gap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory shopping cart shared by the shopping and cart screens.
 * Items are stored as copies keyed by name, so catalog items are never mutated.
 */
public final class CartManager {

    private static final CartManager INSTANCE = new CartManager();

    private final Map<String, GroceryItem> items = new LinkedHashMap<>();

    CartManager() {}

    public static CartManager get() {
        return INSTANCE;
    }

    public synchronized int getQuantity(String name) {
        GroceryItem item = items.get(name);
        return item == null ? 0 : item.getQuantity();
    }

    public synchronized boolean contains(String name) {
        return items.containsKey(name);
    }

    /** Sets the quantity for an item; a quantity of 0 or less removes it from the cart. */
    public synchronized void setQuantity(GroceryItem item, int quantity) {
        if (quantity <= 0) {
            items.remove(item.getName());
        } else {
            items.put(item.getName(), item.withQuantity(quantity));
        }
    }

    public synchronized void remove(String name) {
        items.remove(name);
    }

    public synchronized List<GroceryItem> getItems() {
        return new ArrayList<>(items.values());
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized double getTotal() {
        double total = 0;
        for (GroceryItem item : items.values()) {
            total += item.getLineTotal();
        }
        return total;
    }

    public synchronized void clear() {
        items.clear();
    }
}

package com.example.gap;

import androidx.annotation.DrawableRes;

/** A product (catalog entry) or a line in the cart / an order, depending on {@link #getQuantity()}. Immutable. */
public class GroceryItem {
    private final String name;
    private final double price;
    /** Selling unit shown next to the price, e.g. "kg", "bunch", "piece". */
    private final String unit;
    @DrawableRes
    private final int imageResId;
    private final int quantity;

    public GroceryItem(String name, double price, String unit, @DrawableRes int imageResId) {
        this(name, price, unit, imageResId, 0);
    }

    public GroceryItem(String name, double price, String unit, @DrawableRes int imageResId, int quantity) {
        this.name = name;
        this.price = price;
        this.unit = unit;
        this.imageResId = imageResId;
        this.quantity = quantity;
    }

    /** Returns a copy of this item with the given quantity; the original is not modified. */
    public GroceryItem withQuantity(int newQuantity) {
        return new GroceryItem(name, price, unit, imageResId, newQuantity);
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getUnit() { return unit; }
    public int getImageResId() { return imageResId; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return price * quantity; }
}

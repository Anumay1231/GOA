package com.example.gap;

import androidx.annotation.ColorInt;

/** Lifecycle of an order. Stored in the database by {@link #name()}. */
public enum OrderStatus {
    PLACED("Placed", 0xFF1565C0),
    OUT_FOR_DELIVERY("Out for delivery", 0xFFEF6C00),
    DELIVERED("Delivered", 0xFF2E7D32),
    CANCELLED("Cancelled", 0xFFC62828);

    private final String label;
    @ColorInt private final int color;

    OrderStatus(String label, @ColorInt int color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    @ColorInt public int getColor() { return color; }

    /** Only orders the store hasn't dispatched yet can be cancelled. */
    public boolean canCancel() {
        return this == PLACED;
    }

    /** The customer can confirm receipt of anything still on its way. */
    public boolean canMarkDelivered() {
        return this == PLACED || this == OUT_FOR_DELIVERY;
    }

    /** Parses a stored value, falling back to PLACED for unknown/legacy values. */
    public static OrderStatus fromDb(String value) {
        if (value != null) {
            for (OrderStatus s : values()) {
                if (s.name().equals(value)) return s;
            }
        }
        return PLACED;
    }
}

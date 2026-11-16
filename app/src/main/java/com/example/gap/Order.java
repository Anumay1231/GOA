package com.example.gap;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A placed (or about to be placed) order. Screens pass the order id around and reload from the database. */
public class Order {
    public static final String PAYMENT_UPI = "UPI";
    public static final String PAYMENT_COD = "Cash on Delivery";

    private final long orderId;
    private final List<GroceryItem> items;
    private final double total;
    private final String deliveryAddress;
    private final long createdAt;
    /** {@link #PAYMENT_UPI} or {@link #PAYMENT_COD}; empty for orders placed before payments existed. */
    private final String paymentMethod;
    /** UPI transaction id / UTR if known, otherwise null. */
    @Nullable
    private final String paymentRef;
    private final OrderStatus status;

    /** Creates a new, not yet saved order. */
    public Order(List<GroceryItem> items, String deliveryAddress, String paymentMethod, @Nullable String paymentRef) {
        this(0, items, deliveryAddress, System.currentTimeMillis(), paymentMethod, paymentRef, OrderStatus.PLACED);
    }

    public Order(long orderId, List<GroceryItem> items, String deliveryAddress, long createdAt,
                 String paymentMethod, @Nullable String paymentRef, OrderStatus status) {
        this.orderId = orderId;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.deliveryAddress = deliveryAddress;
        this.createdAt = createdAt;
        this.paymentMethod = paymentMethod == null ? "" : paymentMethod;
        this.paymentRef = paymentRef;
        this.status = status;
        double sum = 0;
        for (GroceryItem item : items) {
            sum += item.getLineTotal();
        }
        this.total = sum;
    }

    public long getOrderId() { return orderId; }
    public List<GroceryItem> getItems() { return items; }
    public double getTotal() { return total; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public long getCreatedAt() { return createdAt; }
    public String getPaymentMethod() { return paymentMethod; }
    @Nullable public String getPaymentRef() { return paymentRef; }
    public OrderStatus getStatus() { return status; }
    public boolean isPaidOnline() { return PAYMENT_UPI.equals(paymentMethod); }
}

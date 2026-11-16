package com.example.gap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

/** Shows one order; lets the customer cancel it, confirm delivery, or reorder the same items. */
public class OrderDetailsActivity extends AppCompatActivity {

    static final String EXTRA_ORDER_ID = "order_id";

    private DatabaseHelper dbHelper;
    private Customer customer;
    private long orderId;
    private Order order;

    private TextView tvStatus, tvMeta, tvTotal, tvTitle;
    private Button btnCancel, btnMarkDelivered;
    private OrderItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        dbHelper = DatabaseHelper.getInstance(this);
        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }
        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);

        Toolbar toolbar = findViewById(R.id.toolbarOrderDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        tvTitle = findViewById(R.id.orderDetailsTitle);
        tvStatus = findViewById(R.id.tvOrderStatus);
        tvMeta = findViewById(R.id.tvOrderMeta);
        tvTotal = findViewById(R.id.totalOrderPriceTextView);
        btnCancel = findViewById(R.id.btnCancelOrder);
        btnMarkDelivered = findViewById(R.id.btnMarkDelivered);

        adapter = new OrderItemsAdapter();
        ((ListView) findViewById(R.id.orderItemsListView)).setAdapter(adapter);

        btnCancel.setOnClickListener(v -> confirmCancel());
        btnMarkDelivered.setOnClickListener(v -> confirmDelivered());
        findViewById(R.id.btnReorder).setOnClickListener(v -> reorder());

        if (!loadOrder()) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** (Re)reads the order from the database so status changes are reflected. */
    private boolean loadOrder() {
        order = dbHelper.getOrder(customer.getCustomerId(), orderId);
        if (order == null) {
            return false;
        }
        OrderStatus status = order.getStatus();
        tvTitle.setText("Order #" + order.getOrderId());
        tvStatus.setText("Status: " + status.getLabel());
        tvStatus.setTextColor(status.getColor());

        StringBuilder meta = new StringBuilder("Placed on ").append(Formats.dateTime(order.getCreatedAt()));
        if (!order.getPaymentMethod().isEmpty()) {
            meta.append("\nPayment: ").append(order.getPaymentMethod());
            if (order.getPaymentRef() != null) {
                meta.append(" (ref ").append(order.getPaymentRef()).append(")");
            }
        }
        meta.append("\n\nDelivery address:\n").append(order.getDeliveryAddress());
        tvMeta.setText(meta);
        tvTotal.setText("Total: " + Formats.money(order.getTotal()));

        btnCancel.setVisibility(status.canCancel() ? View.VISIBLE : View.GONE);
        btnMarkDelivered.setVisibility(status.canMarkDelivered() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
        return true;
    }

    private void confirmCancel() {
        String message = "Cancel order #" + order.getOrderId() + "?";
        if (order.isPaidOnline()) {
            message += "\n\nYou paid " + Formats.money(order.getTotal())
                    + " by UPI. Please contact the store with your UPI reference to get a refund.";
        }
        new AlertDialog.Builder(this)
                .setTitle("Cancel order")
                .setMessage(message)
                .setPositiveButton("Cancel order", (d, w) -> {
                    boolean ok = dbHelper.updateOrderStatus(customer.getCustomerId(), orderId,
                            OrderStatus.CANCELLED, OrderStatus.PLACED);
                    Toast.makeText(this, ok ? "Order cancelled" : "This order can no longer be cancelled",
                            Toast.LENGTH_SHORT).show();
                    loadOrder();
                })
                .setNegativeButton("Keep order", null)
                .show();
    }

    private void confirmDelivered() {
        new AlertDialog.Builder(this)
                .setTitle("Order received?")
                .setMessage("Confirm that you've received order #" + order.getOrderId() + ".")
                .setPositiveButton("Yes, received", (d, w) -> {
                    dbHelper.updateOrderStatus(customer.getCustomerId(), orderId,
                            OrderStatus.DELIVERED, OrderStatus.PLACED, OrderStatus.OUT_FOR_DELIVERY);
                    loadOrder();
                })
                .setNegativeButton("Not yet", null)
                .show();
    }

    /** Adds this order's items to the cart at today's prices and opens the cart. */
    private void reorder() {
        int added = 0;
        List<String> unavailable = new ArrayList<>();
        CartManager cart = CartManager.get();
        for (GroceryItem past : order.getItems()) {
            GroceryItem current = Catalog.find(past.getName());
            if (current == null) {
                unavailable.add(past.getName());
                continue;
            }
            int qty = Math.min(99, cart.getQuantity(current.getName()) + past.getQuantity());
            cart.setQuantity(current, qty);
            added++;
        }
        if (added == 0) {
            Toast.makeText(this, "None of these items are available any more", Toast.LENGTH_LONG).show();
            return;
        }
        if (!unavailable.isEmpty()) {
            Toast.makeText(this, "Not available any more: " + String.join(", ", unavailable),
                    Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, CartActivity.class);
        intent.putExtra(Customer.EXTRA, customer);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    class OrderItemsAdapter extends BaseAdapter {
        private List<GroceryItem> items() {
            return order != null ? order.getItems() : new ArrayList<>();
        }

        @Override
        public int getCount() {
            return items().size();
        }

        @Override
        public Object getItem(int position) {
            return items().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            GroceryItem item = items().get(position);
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            text1.setText(item.getName() + "  •  " + Formats.money(item.getLineTotal()));
            text2.setText(item.getQuantity() + " " + item.getUnit() + " × " + Formats.money(item.getPrice()));
            return convertView;
        }
    }
}

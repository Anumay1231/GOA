package com.example.gap;

import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class PastOrdersActivity extends AppCompatActivity {

    private Customer customer;
    private final List<Order> orders = new ArrayList<>();
    private PastOrdersAdapter adapter;
    private TextView tvNoOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_orders);

        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbarPastOrders);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        tvNoOrders = findViewById(R.id.tvNoOrders);
        ListView listView = findViewById(R.id.pastOrdersListView);
        adapter = new PastOrdersAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, OrderDetailsActivity.class);
            intent.putExtra(Customer.EXTRA, customer);
            intent.putExtra(OrderDetailsActivity.EXTRA_ORDER_ID, orders.get(position).getOrderId());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter == null) return;
        orders.clear();
        orders.addAll(DatabaseHelper.getInstance(this).getOrders(customer.getCustomerId()));
        adapter.notifyDataSetChanged();
        tvNoOrders.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    class PastOrdersAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return orders.size();
        }

        @Override
        public Object getItem(int position) {
            return orders.get(position);
        }

        @Override
        public long getItemId(int position) {
            return orders.get(position).getOrderId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            Order order = orders.get(position);
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            int count = order.getItems().size();
            OrderStatus status = order.getStatus();
            text1.setText("Order #" + order.getOrderId() + "  •  " + Formats.money(order.getTotal()));

            // "Delivered  •  16 Nov 2026, 01:30 PM  •  3 items", with the status in its colour.
            SpannableString line = new SpannableString(status.getLabel() + "  •  "
                    + Formats.dateTime(order.getCreatedAt()) + "  •  " + count + (count == 1 ? " item" : " items"));
            int end = status.getLabel().length();
            line.setSpan(new ForegroundColorSpan(status.getColor()), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            line.setSpan(new StyleSpan(Typeface.BOLD), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text2.setText(line);
            return convertView;
        }
    }
}

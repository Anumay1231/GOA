package com.example.gap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_QTY = 99;
    private static final String KEY_PENDING = "pending_qty";

    private final List<GroceryItem> groceryList = Catalog.items();
    /** Quantity chosen with +/- on each row, not yet committed to the cart. Survives scrolling and rotation. */
    private int[] pendingQty;
    private Customer customer;
    private GroceryAdapter adapter;
    private ExtendedFloatingActionButton cartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        pendingQty = savedInstanceState != null ? savedInstanceState.getIntArray(KEY_PENDING) : null;
        if (pendingQty == null || pendingQty.length != groceryList.size()) {
            pendingQty = new int[groceryList.size()];
            syncPendingWithCart();
        }

        ListView listView = findViewById(R.id.listView);
        adapter = new GroceryAdapter();
        listView.setAdapter(adapter);

        cartButton = findViewById(R.id.btnViewCart);
        cartButton.setOnClickListener(v -> {
            if (CartManager.get().isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CartActivity.class);
            intent.putExtra(Customer.EXTRA, customer);
            startActivity(intent);
        });
        updateCartButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            // The cart may have changed (item removed, order placed, reorder) while we were away.
            syncPendingWithCart();
            adapter.notifyDataSetChanged();
            updateCartButton();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingQty != null) {
            outState.putIntArray(KEY_PENDING, pendingQty);
        }
    }

    private void syncPendingWithCart() {
        for (int i = 0; i < groceryList.size(); i++) {
            pendingQty[i] = CartManager.get().getQuantity(groceryList.get(i).getName());
        }
    }

    /** Cart "badge": shows how many products are in the cart and the running total. */
    private void updateCartButton() {
        CartManager cart = CartManager.get();
        int count = cart.getItems().size();
        if (count == 0) {
            cartButton.setText("Cart");
            cartButton.setContentDescription("Cart, empty");
        } else {
            String text = count + (count == 1 ? " item" : " items") + " · " + Formats.money(cart.getTotal());
            cartButton.setText(text);
            cartButton.setContentDescription("View cart, " + text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_past_orders) {
            Intent intent = new Intent(this, PastOrdersActivity.class);
            intent.putExtra(Customer.EXTRA, customer);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_manage_addresses) {
            Intent intent = new Intent(this, AddressManagementActivity.class);
            intent.putExtra(Customer.EXTRA, customer);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        if (CartManager.get().isEmpty()) {
            LoginActivity.logout(this);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("Items in your cart will be removed.")
                .setPositiveButton("Log out", (d, w) -> LoginActivity.logout(this))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class RowHolder {
        final ImageView img;
        final TextView name, price, qty, plus, minus;
        final Button add;

        RowHolder(View row) {
            img = row.findViewById(R.id.imgItem);
            name = row.findViewById(R.id.txtItemName);
            price = row.findViewById(R.id.txtPrice);
            qty = row.findViewById(R.id.txtQty);
            plus = row.findViewById(R.id.btnPlus);
            minus = row.findViewById(R.id.btnMinus);
            add = row.findViewById(R.id.btnAdd);
        }
    }

    class GroceryAdapter extends BaseAdapter {
        @Override
        public int getCount() { return groceryList.size(); }

        @Override
        public Object getItem(int position) { return groceryList.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            RowHolder h;
            if (row == null) {
                row = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item, parent, false);
                h = new RowHolder(row);
                row.setTag(h);
            } else {
                h = (RowHolder) row.getTag();
            }

            GroceryItem item = groceryList.get(position);
            CartManager cart = CartManager.get();
            boolean inCart = cart.contains(item.getName());

            h.img.setImageResource(item.getImageResId());
            h.name.setText(item.getName());
            h.price.setText(getString(R.string.price_format, Formats.money(item.getPrice()), item.getUnit()));
            h.qty.setText(String.valueOf(pendingQty[position]));
            h.add.setText(inCart ? "Update" : "Add");

            h.plus.setOnClickListener(v -> {
                if (pendingQty[position] < MAX_QTY) {
                    pendingQty[position]++;
                    h.qty.setText(String.valueOf(pendingQty[position]));
                }
            });

            h.minus.setOnClickListener(v -> {
                if (pendingQty[position] > 0) {
                    pendingQty[position]--;
                    h.qty.setText(String.valueOf(pendingQty[position]));
                }
            });

            h.add.setOnClickListener(v -> {
                int q = pendingQty[position];
                boolean wasInCart = cart.contains(item.getName());
                if (q <= 0 && !wasInCart) {
                    Toast.makeText(MainActivity.this, "Use + to choose a quantity", Toast.LENGTH_SHORT).show();
                    return;
                }
                cart.setQuantity(item, q);
                String msg = q <= 0 ? item.getName() + " removed from cart"
                        : wasInCart ? item.getName() + " updated to " + q
                        : item.getName() + " added to cart";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
                updateCartButton();
            });
            return row;
        }
    }
}

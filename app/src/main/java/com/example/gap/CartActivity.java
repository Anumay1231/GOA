package com.example.gap;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.IntentCompat;

import java.util.List;

public class CartActivity extends AppCompatActivity {

    private static final String KEY_ADDRESS_ID = "selected_address_id";

    private Customer customer;
    private DatabaseHelper dbHelper;
    private Address selectedAddress;
    private TextView tvDeliveryAddress;
    private TextView totalText;
    private ArrayAdapter<GroceryItem> adapter;

    private final ActivityResultLauncher<Intent> pickAddress = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null) {
                    Address picked = IntentCompat.getParcelableExtra(data,
                            AddressManagementActivity.EXTRA_SELECTED_ADDRESS, Address.class);
                    if (picked != null) {
                        selectedAddress = picked;
                        Toast.makeText(this, "Address selected", Toast.LENGTH_SHORT).show();
                    }
                }
                // The address may have been deleted/edited on the other screen; re-read it either way.
                refreshSelectedAddress(selectedAddress != null ? selectedAddress.getAddressId() : -1);
            });

    private final ActivityResultLauncher<Intent> payForOrder = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    finish(); // order placed and cart cleared by PaymentActivity
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        dbHelper = DatabaseHelper.getInstance(this);
        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }
        if (CartManager.get().isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbarCart);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        ListView listView = findViewById(R.id.listViewCart);
        totalText = findViewById(R.id.txtTotal);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        Button placeOrderBtn = findViewById(R.id.btnPlaceOrder);
        Button btnChangeAddress = findViewById(R.id.btnChangeAddress);

        adapter = new ArrayAdapter<GroceryItem>(this, android.R.layout.simple_list_item_2, android.R.id.text1) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                GroceryItem g = getItem(position);
                if (g != null) {
                    ((TextView) row.findViewById(android.R.id.text1)).setText(
                            g.getName() + " × " + g.getQuantity() + " " + g.getUnit());
                    ((TextView) row.findViewById(android.R.id.text2)).setText(
                            Formats.money(g.getPrice()) + " per " + g.getUnit() + "  •  "
                                    + Formats.money(g.getLineTotal()) + "   (tap to remove)");
                }
                return row;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> confirmRemove(adapter.getItem(position)));
        refreshCart();

        int restoredId = savedInstanceState != null ? savedInstanceState.getInt(KEY_ADDRESS_ID, -1) : -1;
        refreshSelectedAddress(restoredId);

        btnChangeAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressManagementActivity.class);
            intent.putExtra(Customer.EXTRA, customer);
            intent.putExtra(AddressManagementActivity.EXTRA_FROM_CART, true);
            if (selectedAddress != null) {
                intent.putExtra(AddressManagementActivity.EXTRA_SELECTED_ADDRESS_ID, selectedAddress.getAddressId());
            }
            pickAddress.launch(intent);
        });

        placeOrderBtn.setText("Proceed to Pay");
        placeOrderBtn.setOnClickListener(v -> proceedToPayment());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedAddress != null) {
            outState.putInt(KEY_ADDRESS_ID, selectedAddress.getAddressId());
        }
    }

    /** Uses the given address if it still exists, otherwise falls back to the default address. */
    private void refreshSelectedAddress(int addressId) {
        Address address = addressId > 0 ? dbHelper.getAddress(customer.getCustomerId(), addressId) : null;
        selectedAddress = address != null ? address : dbHelper.getDefaultAddress(customer.getCustomerId());
        if (selectedAddress != null) {
            tvDeliveryAddress.setText("Deliver to:\n" + selectedAddress);
        } else {
            tvDeliveryAddress.setText("No address selected\nTap \"Change Address\" to add one");
        }
    }

    private void refreshCart() {
        List<GroceryItem> items = CartManager.get().getItems();
        adapter.clear();
        adapter.addAll(items);
        totalText.setText("Total: " + Formats.money(CartManager.get().getTotal()));
    }

    private void confirmRemove(GroceryItem item) {
        if (item == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Remove item")
                .setMessage("Remove " + item.getName() + " from your cart?")
                .setPositiveButton("Remove", (d, w) -> {
                    CartManager.get().remove(item.getName());
                    if (CartManager.get().isEmpty()) {
                        Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        refreshCart();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Goes to the UPI payment screen; the order is saved there once payment is confirmed. */
    private void proceedToPayment() {
        if (selectedAddress == null) {
            Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(Customer.EXTRA, customer);
        intent.putExtra(PaymentActivity.EXTRA_ADDRESS_ID, selectedAddress.getAddressId());
        payForOrder.launch(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cart_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_past_orders_cart) {
            Intent intent = new Intent(this, PastOrdersActivity.class);
            intent.putExtra(Customer.EXTRA, customer);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

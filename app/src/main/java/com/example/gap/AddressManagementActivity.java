package com.example.gap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class AddressManagementActivity extends AppCompatActivity {

    /** Input: open in "pick a delivery address" mode (radio buttons + Select button). */
    static final String EXTRA_FROM_CART = "fromCart";
    /** Input: id of the address to pre-select. */
    static final String EXTRA_SELECTED_ADDRESS_ID = "selectedAddressId";
    /** Result: the chosen {@link Address}. */
    static final String EXTRA_SELECTED_ADDRESS = "selectedAddress";

    private static final String KEY_SELECTED = "selected_address_id";

    private DatabaseHelper dbHelper;
    private Customer customer;
    private boolean isFromCart;
    private List<Address> addresses = new ArrayList<>();
    private AddressAdapter adapter;
    /** Id of the address the user picked (only meaningful when opened from the cart). */
    private int selectedAddressId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_management);

        dbHelper = DatabaseHelper.getInstance(this);
        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }
        isFromCart = getIntent().getBooleanExtra(EXTRA_FROM_CART, false);
        selectedAddressId = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_SELECTED, -1)
                : getIntent().getIntExtra(EXTRA_SELECTED_ADDRESS_ID, -1);

        Toolbar toolbar = findViewById(R.id.toolbarAddress);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        ListView listView = findViewById(R.id.listViewAddresses);
        Button btnAddNew = findViewById(R.id.btnAddNewAddress);
        Button btnSelectAddress = findViewById(R.id.btnSelectAddress);
        btnSelectAddress.setVisibility(isFromCart ? View.VISIBLE : View.GONE);

        adapter = new AddressAdapter();
        listView.setAdapter(adapter);
        if (isFromCart) {
            listView.setOnItemClickListener((parent, view, position, id) -> select(addresses.get(position)));
        }

        btnAddNew.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddAddressActivity.class);
            intent.putExtra(Customer.EXTRA, customer);
            startActivity(intent);
        });

        btnSelectAddress.setOnClickListener(v -> {
            Address selected = findSelected();
            if (selected == null) {
                Toast.makeText(this, "Please select an address", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SELECTED_ADDRESS, selected);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            loadAddresses(); // also picks up addresses added on AddAddressActivity
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED, selectedAddressId);
    }

    private void loadAddresses() {
        addresses = dbHelper.getCustomerAddresses(customer.getCustomerId());
        // Keep the current choice if it still exists; otherwise fall back to the default address.
        if (findSelected() == null) {
            selectedAddressId = -1;
            for (Address a : addresses) {
                if (a.isDefault()) {
                    selectedAddressId = a.getAddressId();
                    break;
                }
            }
        }
        if (addresses.isEmpty()) {
            Toast.makeText(this, "No addresses found. Please add one.", Toast.LENGTH_LONG).show();
        }
        adapter.notifyDataSetChanged();
    }

    private void select(Address address) {
        selectedAddressId = address.getAddressId();
        adapter.notifyDataSetChanged();
    }

    @Nullable
    private Address findSelected() {
        for (Address a : addresses) {
            if (a.getAddressId() == selectedAddressId) {
                return a;
            }
        }
        return null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    class AddressAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return addresses.size();
        }

        @Override
        public Object getItem(int position) {
            return addresses.get(position);
        }

        @Override
        public long getItemId(int position) {
            return addresses.get(position).getAddressId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_address, parent, false);
            }

            Address address = addresses.get(position);

            RadioButton rbSelect = convertView.findViewById(R.id.rbSelectAddress);
            TextView tvLabel = convertView.findViewById(R.id.tvAddressLabel);
            TextView tvAddress = convertView.findViewById(R.id.tvAddressDetails);
            Button btnSetDefault = convertView.findViewById(R.id.btnSetDefault);
            Button btnEdit = convertView.findViewById(R.id.btnEditAddress);
            Button btnDelete = convertView.findViewById(R.id.btnDeleteAddress);

            tvLabel.setText(address.getLabel() + (address.isDefault() ? " (Default)" : ""));
            tvAddress.setText(address.getFullAddress());

            // Radio buttons only make sense when picking a delivery address for the cart.
            rbSelect.setVisibility(isFromCart ? View.VISIBLE : View.GONE);
            rbSelect.setChecked(address.getAddressId() == selectedAddressId);
            rbSelect.setOnClickListener(v -> select(address));

            btnSetDefault.setEnabled(!address.isDefault());
            btnSetDefault.setText(address.isDefault() ? "Default" : "Set as Default");
            btnSetDefault.setOnClickListener(v -> {
                dbHelper.setDefaultAddress(customer.getCustomerId(), address.getAddressId());
                Toast.makeText(AddressManagementActivity.this, "Default address updated", Toast.LENGTH_SHORT).show();
                loadAddresses();
            });

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(AddressManagementActivity.this, AddAddressActivity.class);
                intent.putExtra(Customer.EXTRA, customer);
                intent.putExtra(AddAddressActivity.EXTRA_EDIT_ADDRESS_ID, address.getAddressId());
                startActivity(intent); // list reloads in onResume
            });

            btnDelete.setOnClickListener(v -> {
                if (addresses.size() == 1) {
                    Toast.makeText(AddressManagementActivity.this, "You must have at least one address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (address.isDefault()) {
                    Toast.makeText(AddressManagementActivity.this, "Cannot delete default address. Set another as default first.", Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(AddressManagementActivity.this)
                        .setTitle("Delete Address")
                        .setMessage("Are you sure you want to delete this address?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dbHelper.deleteAddress(customer.getCustomerId(), address.getAddressId());
                            Toast.makeText(AddressManagementActivity.this, "Address deleted", Toast.LENGTH_SHORT).show();
                            loadAddresses();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            return convertView;
        }
    }
}

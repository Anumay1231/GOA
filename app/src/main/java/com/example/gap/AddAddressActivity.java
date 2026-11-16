package com.example.gap;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/** Adds a new delivery address, or edits one when started with {@link #EXTRA_EDIT_ADDRESS_ID}. */
public class AddAddressActivity extends AppCompatActivity {

    static final String EXTRA_EDIT_ADDRESS_ID = "edit_address_id";

    private DatabaseHelper dbHelper;
    private Customer customer;
    private AddressForm form;
    /** Address being edited, or null when adding a new one. */
    private Address editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_address);

        dbHelper = DatabaseHelper.getInstance(this);
        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbarAddAddress);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        form = new AddressForm(this);
        form.restoreState(savedInstanceState);
        Button btnSave = findViewById(R.id.btnSaveAddress);
        btnSave.setOnClickListener(v -> saveAddress());

        int editId = getIntent().getIntExtra(EXTRA_EDIT_ADDRESS_ID, -1);
        if (editId > 0) {
            editing = dbHelper.getAddress(customer.getCustomerId(), editId);
            if (editing == null) {
                Toast.makeText(this, "Address no longer exists", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            ((TextView) findViewById(R.id.tvAddAddressTitle)).setText("Edit Address");
            btnSave.setText("Save Changes");
            // Only pre-fill on first creation; after rotation the EditTexts restore what was typed.
            if (savedInstanceState == null) {
                form.fill(editing);
            }
            // The default address can't be "un-defaulted" here (every customer needs one);
            // pick another address as default on the Manage Addresses screen instead.
            if (editing.isDefault()) {
                form.cbDefault.setChecked(true);
                form.cbDefault.setEnabled(false);
            }
        }
    }

    private void saveAddress() {
        Address address = form.readValidated(editing != null ? editing.getAddressId() : 0);
        if (address == null) {
            return;
        }
        boolean ok = editing != null
                ? dbHelper.updateAddress(customer.getCustomerId(), address)
                : dbHelper.addAddress(customer.getCustomerId(), address) > 0;
        if (ok) {
            Toast.makeText(this, editing != null ? "Address updated" : "Address saved successfully!",
                    Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save address", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (form != null) form.saveState(outState);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

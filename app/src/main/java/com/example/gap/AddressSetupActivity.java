package com.example.gap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/** First-run screen after registration: the customer must add at least one delivery address. */
public class AddressSetupActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Customer customer;
    private AddressForm form;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_setup);
        SystemBars.apply(this, null, false);

        dbHelper = DatabaseHelper.getInstance(this);
        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }

        form = new AddressForm(this);
        form.restoreState(savedInstanceState);
        Button btnSaveAddress = findViewById(R.id.btnSaveAddress);
        btnContinue = findViewById(R.id.btnSkip);

        btnSaveAddress.setOnClickListener(v -> saveAddress());
        btnContinue.setOnClickListener(v -> tryContinue());

        // onBackPressed() is not called for apps targeting Android 16+, so use the dispatcher.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                tryContinue();
            }
        });

        updateState();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (form != null) form.saveState(outState);
    }

    /** Count comes from the database so it is correct after rotation or process death. */
    private int addressCount() {
        return dbHelper.getAddressCount(customer.getCustomerId());
    }

    private void updateState() {
        boolean first = addressCount() == 0;
        // The first address always becomes the default (enforced in DatabaseHelper.addAddress).
        form.cbDefault.setChecked(first);
        form.cbDefault.setEnabled(!first);
        btnContinue.setText(first ? "Continue" : "Continue to App");
    }

    private void saveAddress() {
        Address address = form.readValidated();
        if (address == null) {
            return;
        }
        long result = dbHelper.addAddress(customer.getCustomerId(), address);
        if (result > 0) {
            Toast.makeText(this, "Address saved! Add another or tap \"Continue to App\".", Toast.LENGTH_LONG).show();
            form.clear();
            updateState();
        } else {
            Toast.makeText(this, "Failed to save address", Toast.LENGTH_SHORT).show();
        }
    }

    private void tryContinue() {
        if (addressCount() < 1) {
            Toast.makeText(this, "Please add at least one address", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Customer.EXTRA, customer);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

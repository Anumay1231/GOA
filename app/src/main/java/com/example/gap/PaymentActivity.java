package com.example.gap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

/**
 * Collects payment for the current cart via UPI, either by scanning the shop owner's QR code or by
 * handing off to a UPI app installed on this phone (amount pre-filled), then places the order.
 * Cash on Delivery is offered as an alternative.
 *
 * Note: without a payment gateway/backend the app cannot verify that money actually arrived;
 * the UPI reference stored with the order lets the shop owner match it against their bank statement.
 */
public class PaymentActivity extends AppCompatActivity {

    static final String EXTRA_ADDRESS_ID = "address_id";

    private DatabaseHelper dbHelper;
    private Customer customer;
    private Address address;
    private double amount;
    private EditText etUtr;

    private final ActivityResultLauncher<Intent> upiLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                String response = data != null ? data.getStringExtra("response") : null;
                onUpiResult(UpiResponse.parse(response));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        dbHelper = DatabaseHelper.getInstance(this);
        customer = Customer.fromIntent(getIntent());
        if (customer == null) {
            LoginActivity.restart(this);
            return;
        }
        address = dbHelper.getAddress(customer.getCustomerId(), getIntent().getIntExtra(EXTRA_ADDRESS_ID, -1));
        if (address == null || CartManager.get().isEmpty()) {
            Toast.makeText(this, "Nothing to pay for", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        amount = CartManager.get().getTotal();

        Toolbar toolbar = findViewById(R.id.toolbarPayment);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        String vpa = getString(R.string.upi_payee_vpa);
        ((TextView) findViewById(R.id.tvAmount)).setText(Formats.money(amount));
        ((TextView) findViewById(R.id.tvAmountHint)).setText(
                "Pay exactly " + Formats.money(amount) + " to " + getString(R.string.upi_payee_name));
        ((TextView) findViewById(R.id.tvUpiId)).setText("UPI ID: " + vpa);
        etUtr = findViewById(R.id.etUtr);

        findViewById(R.id.btnCopyUpiId).setOnClickListener(v -> copyUpiId(vpa));

        Button btnPayWithApp = findViewById(R.id.btnPayWithApp);
        btnPayWithApp.setText("Pay " + Formats.money(amount) + " with a UPI app");
        btnPayWithApp.setOnClickListener(v -> launchUpiApp());

        findViewById(R.id.btnConfirmPaid).setOnClickListener(v -> confirmManualPayment());

        Button btnCod = findViewById(R.id.btnCashOnDelivery);
        btnCod.setText("Cash on Delivery (" + Formats.money(amount) + ")");
        btnCod.setOnClickListener(v -> confirmCashOnDelivery());
    }

    private void confirmCashOnDelivery() {
        new AlertDialog.Builder(this)
                .setTitle("Cash on Delivery")
                .setMessage("Please keep " + Formats.money(amount) + " ready when your order arrives.")
                .setPositiveButton("Place order", (d, w) -> placeOrder(Order.PAYMENT_COD, null))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Uri buildUpiUri() {
        return new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", getString(R.string.upi_payee_vpa))
                .appendQueryParameter("pn", getString(R.string.upi_payee_name))
                .appendQueryParameter("am", String.format(Locale.ENGLISH, "%.2f", amount))
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tn", "Grocery order")
                .build();
    }

    private void launchUpiApp() {
        Intent upi = new Intent(Intent.ACTION_VIEW, buildUpiUri());
        // Only offer apps that can actually handle UPI (needs the <queries> entry in the manifest).
        if (upi.resolveActivity(getPackageManager()) == null) {
            new AlertDialog.Builder(this)
                    .setTitle("No UPI app found")
                    .setMessage("Install a UPI app (Google Pay, PhonePe, Paytm, BHIM…) or scan the QR code with another phone, then enter the UPI reference below.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        upiLauncher.launch(Intent.createChooser(upi, "Pay with"));
    }

    private void onUpiResult(UpiResponse response) {
        switch (response.getStatus()) {
            case SUCCESS:
                placeOrder(Order.PAYMENT_UPI, response.getTransactionId());
                break;
            case FAILURE:
                Toast.makeText(this, "Payment failed or was cancelled. You have not been charged for this order.",
                        Toast.LENGTH_LONG).show();
                break;
            case SUBMITTED:
                Toast.makeText(this, "Payment is pending. Once your UPI app shows it as successful, tap \"I've paid\".",
                        Toast.LENGTH_LONG).show();
                if (response.getTransactionId() != null) {
                    etUtr.setText(response.getTransactionId());
                }
                break;
            default:
                // Many UPI apps don't report a result back; let the user confirm themselves.
                Toast.makeText(this, "If the payment went through, tap \"I've paid\" to place your order.",
                        Toast.LENGTH_LONG).show();
        }
    }

    private void confirmManualPayment() {
        String utr = etUtr.getText().toString().trim();
        if (!utr.isEmpty() && !utr.matches("[0-9]{12}")) {
            etUtr.setError("UPI reference numbers are 12 digits");
            etUtr.requestFocus();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Confirm payment")
                .setMessage("Have you paid " + Formats.money(amount) + " to " + getString(R.string.upi_payee_name) + "?")
                .setPositiveButton("Yes, place order", (d, w) -> placeOrder(Order.PAYMENT_UPI, utr.isEmpty() ? null : utr))
                .setNegativeButton("Not yet", null)
                .show();
    }

    private void placeOrder(String paymentMethod, @Nullable String paymentRef) {
        // Guard against the cart changing (or being emptied) while we were in the UPI app.
        if (CartManager.get().isEmpty()) {
            finish();
            return;
        }
        Order order = new Order(CartManager.get().getItems(), address.toOrderText(), paymentMethod, paymentRef);
        long orderId = dbHelper.placeOrder(customer.getCustomerId(), order);
        if (orderId <= 0) {
            Toast.makeText(this, "Could not place order, please try again", Toast.LENGTH_SHORT).show();
            return;
        }
        CartManager.get().clear();
        Toast.makeText(this, "Order #" + orderId + " placed! Delivering to " + address.getLabel(),
                Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }

    private void copyUpiId(String vpa) {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        cm.setPrimaryClip(ClipData.newPlainText("UPI ID", vpa));
        // Android 13+ shows its own clipboard confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "UPI ID copied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

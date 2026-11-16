package com.example.gap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class LoginActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText username, password;

    /**
     * Clears the back stack and returns to the launch screen. If a saved session exists the user is
     * taken straight back into the app (used when a screen is opened without a customer, e.g. after
     * process death).
     */
    static void restart(Activity from) {
        Intent intent = new Intent(from, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        from.finish();
    }

    /** Ends the session, empties the cart and shows the login form. */
    static void logout(Activity from) {
        Session.clear(from);
        CartManager.get().clear();
        restart(from);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // System splash screen (Android 12+ style, backported) replaces the old SplashActivity.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        dbHelper = DatabaseHelper.getInstance(this);

        // Stay logged in: skip the form if a session from a previous launch is still valid.
        int savedId = Session.getCustomerId(this);
        if (savedId > 0) {
            Customer saved = dbHelper.getCustomer(savedId);
            if (saved != null) {
                openApp(saved);
                return;
            }
            Session.clear(this); // account no longer exists
        }

        setContentView(R.layout.activity_login);
        SystemBars.apply(this, null, true);

        username = findViewById(R.id.editUsername);
        password = findViewById(R.id.editPassword);
        Button loginBtn = findViewById(R.id.btnLogin);
        TextView tvRegisterLink = findViewById(R.id.tvRegisterLink);

        loginBtn.setOnClickListener(v -> attemptLogin());
        password.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String user = username.getText().toString().trim();
        String pass = password.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        Customer customer = dbHelper.loginCustomer(user, pass);
        if (customer == null) {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        CartManager.get().clear();
        Session.save(this, customer);
        Toast.makeText(this, "Welcome, " + customer.getFullName() + "!", Toast.LENGTH_SHORT).show();
        openApp(customer);
    }

    private void openApp(Customer customer) {
        // Customers who never finished address setup are sent back there first.
        boolean hasAddress = dbHelper.getAddressCount(customer.getCustomerId()) > 0;
        Intent intent = new Intent(this, hasAddress ? MainActivity.class : AddressSetupActivity.class);
        intent.putExtra(Customer.EXTRA, customer);
        startActivity(intent);
        finish();
    }
}

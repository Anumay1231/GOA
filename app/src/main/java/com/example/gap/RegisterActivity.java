package com.example.gap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etUsername, etPassword, etConfirmPassword, etFullName, etPhone, etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        SystemBars.apply(this, null, false);

        dbHelper = DatabaseHelper.getInstance(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> registerUser());

        // Login is still underneath in the back stack, so just close this screen.
        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.contains(" ")) {
            etUsername.setError("Username cannot contain spaces");
            etUsername.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }
        if (!phone.matches("[0-9]{10}")) {
            etPhone.setError("Enter a valid 10-digit phone number");
            etPhone.requestFocus();
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (dbHelper.isUsernameExists(username)) {
            etUsername.setError("Username already exists");
            etUsername.requestFocus();
            return;
        }

        Customer customer = dbHelper.registerCustomer(username, password, fullName, phone,
                email.isEmpty() ? null : email);
        if (customer == null) {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Registration successful! Please add your address.", Toast.LENGTH_LONG).show();
        CartManager.get().clear();
        Session.save(this, customer);
        Intent intent = new Intent(this, AddressSetupActivity.class);
        intent.putExtra(Customer.EXTRA, customer);
        // Logged in now: drop Login/Register from the back stack.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

package com.example.gap;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;

/** Logged-in customer. Deliberately does not carry the password (hash) around the app. */
public class Customer implements Parcelable {
    private final int customerId;
    private final String username;
    private final String fullName;
    private final String phone;
    private final String email;

    public Customer(int customerId, String username, String fullName, String phone, String email) {
        this.customerId = customerId;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    protected Customer(Parcel in) {
        customerId = in.readInt();
        username = in.readString();
        fullName = in.readString();
        phone = in.readString();
        email = in.readString();
    }

    public static final Creator<Customer> CREATOR = new Creator<Customer>() {
        @Override
        public Customer createFromParcel(Parcel in) {
            return new Customer(in);
        }

        @Override
        public Customer[] newArray(int size) {
            return new Customer[size];
        }
    };

    /** Intent extra key used to pass the logged-in customer between screens. */
    public static final String EXTRA = "customer";

    @Nullable
    public static Customer fromIntent(Intent intent) {
        return IntentCompat.getParcelableExtra(intent, EXTRA, Customer.class);
    }

    public int getCustomerId() { return customerId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(customerId);
        dest.writeString(username);
        dest.writeString(fullName);
        dest.writeString(phone);
        dest.writeString(email);
    }
}

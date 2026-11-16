package com.example.gap;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers who is logged in so the app opens straight to the shop on the next launch.
 * Only the customer id is stored; it's excluded from backups (see data_extraction_rules.xml).
 */
final class Session {

    private static final String PREFS = "session";
    private static final String KEY_CUSTOMER_ID = "customer_id";

    private Session() {}

    static void save(Context context, Customer customer) {
        prefs(context).edit().putInt(KEY_CUSTOMER_ID, customer.getCustomerId()).apply();
    }

    /** Returns -1 if nobody is logged in. */
    static int getCustomerId(Context context) {
        return prefs(context).getInt(KEY_CUSTOMER_ID, -1);
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY_CUSTOMER_ID).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

package com.example.gap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared display formatting for prices and dates. */
public final class Formats {

    private Formats() {}

    /** Formats a rupee amount: whole amounts without decimals (₹40), others with two (₹40.50). */
    public static String money(double amount) {
        double rounded = Math.round(amount * 100) / 100.0;
        if (rounded == Math.rint(rounded)) {
            return String.format(Locale.ENGLISH, "₹%d", (long) rounded);
        }
        return String.format(Locale.ENGLISH, "₹%.2f", rounded);
    }

    public static String dateTime(long epochMillis) {
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(new Date(epochMillis));
    }
}

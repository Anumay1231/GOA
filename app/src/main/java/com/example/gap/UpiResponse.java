package com.example.gap;

import androidx.annotation.Nullable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the "response" string UPI apps return after an upi://pay intent, e.g.
 * {@code txnId=ABC123&responseCode=00&Status=SUCCESS&txnRef=XYZ}. Keys are matched case-insensitively
 * because apps are inconsistent about casing.
 */
public final class UpiResponse {

    public enum Status { SUCCESS, SUBMITTED, FAILURE, UNKNOWN }

    private final Status status;
    @Nullable private final String transactionId;

    private UpiResponse(Status status, @Nullable String transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }

    public static UpiResponse parse(@Nullable String response) {
        if (response == null || response.trim().isEmpty()) {
            return new UpiResponse(Status.UNKNOWN, null);
        }
        Map<String, String> values = new HashMap<>();
        for (String pair : response.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = pair.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = decode(pair.substring(eq + 1).trim());
            values.put(key, value);
        }

        String rawStatus = values.get("status");
        Status status;
        if (rawStatus == null) {
            status = Status.UNKNOWN;
        } else {
            switch (rawStatus.toUpperCase(Locale.ROOT)) {
                case "SUCCESS":
                    status = Status.SUCCESS;
                    break;
                case "SUBMITTED":
                case "PENDING":
                    status = Status.SUBMITTED;
                    break;
                case "FAILURE":
                case "FAILED":
                    status = Status.FAILURE;
                    break;
                default:
                    status = Status.UNKNOWN;
            }
        }

        String txn = firstNonEmpty(values.get("approvalrefno"), values.get("txnid"), values.get("txnref"));
        return new UpiResponse(status, txn);
    }

    public Status getStatus() { return status; }
    @Nullable public String getTransactionId() { return transactionId; }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    @Nullable
    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty() && !"null".equalsIgnoreCase(v)) return v;
        }
        return null;
    }
}

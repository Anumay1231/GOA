package com.example.gap;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.Locale;

/** A delivery address. Immutable; optionally pinned to map coordinates. */
public class Address implements Parcelable {
    private final int addressId;
    private final String label; // Home, Office, Other
    private final String houseNo;
    private final String street;
    private final String area;
    private final String city;
    private final String state;
    private final String pincode;
    private final String landmark;
    private final boolean isDefault;
    /** Map pin, or null if the customer didn't pick one. */
    @Nullable private final Double latitude;
    @Nullable private final Double longitude;

    public Address(int addressId, String label, String houseNo, String street, String area,
                   String city, String state, String pincode, String landmark, boolean isDefault,
                   @Nullable Double latitude, @Nullable Double longitude) {
        this.addressId = addressId;
        this.label = label;
        this.houseNo = houseNo;
        this.street = street;
        this.area = area;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.landmark = landmark;
        this.isDefault = isDefault;
        // Store both or neither.
        boolean hasPin = latitude != null && longitude != null;
        this.latitude = hasPin ? latitude : null;
        this.longitude = hasPin ? longitude : null;
    }

    public Address(int addressId, String label, String houseNo, String street, String area,
                   String city, String state, String pincode, String landmark, boolean isDefault) {
        this(addressId, label, houseNo, street, area, city, state, pincode, landmark, isDefault, null, null);
    }

    /** Constructor for a new address (without ID). */
    public Address(String label, String houseNo, String street, String area,
                   String city, String state, String pincode, String landmark, boolean isDefault) {
        this(0, label, houseNo, street, area, city, state, pincode, landmark, isDefault, null, null);
    }

    protected Address(Parcel in) {
        addressId = in.readInt();
        label = in.readString();
        houseNo = in.readString();
        street = in.readString();
        area = in.readString();
        city = in.readString();
        state = in.readString();
        pincode = in.readString();
        landmark = in.readString();
        isDefault = in.readByte() != 0;
        if (in.readByte() != 0) {
            latitude = in.readDouble();
            longitude = in.readDouble();
        } else {
            latitude = null;
            longitude = null;
        }
    }

    public static final Creator<Address> CREATOR = new Creator<Address>() {
        @Override
        public Address createFromParcel(Parcel in) {
            return new Address(in);
        }

        @Override
        public Address[] newArray(int size) {
            return new Address[size];
        }
    };

    public int getAddressId() { return addressId; }
    public String getLabel() { return label; }
    public String getHouseNo() { return houseNo; }
    public String getStreet() { return street; }
    public String getArea() { return area; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }
    public String getLandmark() { return landmark; }
    public boolean isDefault() { return isDefault; }
    @Nullable public Double getLatitude() { return latitude; }
    @Nullable public Double getLongitude() { return longitude; }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    /** Google Maps link to the pinned location (opens the Maps app or browser), or null. */
    @Nullable
    public String getMapsUrl() {
        if (!hasLocation()) return null;
        return String.format(Locale.ROOT, "https://maps.google.com/?q=%.6f,%.6f", latitude, longitude);
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(houseNo);
        if (street != null && !street.isEmpty()) {
            sb.append(", ").append(street);
        }
        sb.append(", ").append(area);
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" - ").append(pincode);
        if (landmark != null && !landmark.isEmpty()) {
            sb.append("\nLandmark: ").append(landmark);
        }
        if (hasLocation()) {
            sb.append("\n📍 Pinned on map");
        }
        return sb.toString();
    }

    /** Text saved with an order: the address plus a map link the delivery person can open. */
    public String toOrderText() {
        String text = toString();
        String url = getMapsUrl();
        return url != null ? text + "\n" + url : text;
    }

    @Override
    public String toString() {
        return label + (isDefault ? " (Default)" : "") + "\n" + getFullAddress();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(addressId);
        dest.writeString(label);
        dest.writeString(houseNo);
        dest.writeString(street);
        dest.writeString(area);
        dest.writeString(city);
        dest.writeString(state);
        dest.writeString(pincode);
        dest.writeString(landmark);
        dest.writeByte((byte) (isDefault ? 1 : 0));
        dest.writeByte((byte) (hasLocation() ? 1 : 0));
        if (hasLocation()) {
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
        }
    }
}

package com.example.gap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Pattern;

/**
 * Binds and validates the address form shared by AddressSetupActivity and AddAddressActivity
 * (both layouts use the same view ids), including the optional "Pick on map" location pin.
 * Must be created in the activity's onCreate (it registers an activity-result launcher).
 */
final class AddressForm {

    private static final String DEFAULT_CITY = "Mumbai";
    private static final String DEFAULT_STATE = "Maharashtra";
    /** Indian PIN codes are 6 digits and never start with 0. */
    private static final Pattern PINCODE = Pattern.compile("[1-9][0-9]{5}");
    private static final String KEY_LAT = "address_form_lat", KEY_LNG = "address_form_lng";

    private final AppCompatActivity activity;
    private final EditText etHouseNo, etStreet, etArea, etCity, etState, etPincode, etLandmark;
    private final Spinner spinnerLabel;
    private final Button btnPickOnMap;
    private final TextView tvMapStatus;
    final CheckBox cbDefault;

    /** Map pin for the address being entered, or null. */
    @Nullable private Double latitude, longitude;

    private final ActivityResultLauncher<Intent> pickOnMap;

    AddressForm(AppCompatActivity activity) {
        this.activity = activity;
        etHouseNo = activity.findViewById(R.id.etHouseNo);
        etStreet = activity.findViewById(R.id.etStreet);
        etArea = activity.findViewById(R.id.etArea);
        etCity = activity.findViewById(R.id.etCity);
        etState = activity.findViewById(R.id.etState);
        etPincode = activity.findViewById(R.id.etPincode);
        etLandmark = activity.findViewById(R.id.etLandmark);
        spinnerLabel = activity.findViewById(R.id.spinnerLabel);
        cbDefault = activity.findViewById(R.id.cbDefault);
        btnPickOnMap = activity.findViewById(R.id.btnPickOnMap);
        tvMapStatus = activity.findViewById(R.id.tvMapStatus);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity,
                R.array.address_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLabel.setAdapter(adapter);

        etCity.setText(DEFAULT_CITY);
        etState.setText(DEFAULT_STATE);

        pickOnMap = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        applyMapResult(result.getData());
                    }
                });
        if (BuildConfig.HAS_MAPS_KEY) {
            btnPickOnMap.setOnClickListener(v -> openMap());
        } else {
            // No Maps key in local.properties: hide the feature rather than show a blank map.
            btnPickOnMap.setVisibility(View.GONE);
        }
        updateMapStatus();
    }

    private void openMap() {
        Intent intent = new Intent(activity, MapLocationActivity.class);
        if (latitude != null && longitude != null) {
            intent.putExtra(MapLocationActivity.EXTRA_LAT, latitude);
            intent.putExtra(MapLocationActivity.EXTRA_LNG, longitude);
        }
        pickOnMap.launch(intent);
    }

    /** Stores the pin and fills in whatever address parts the map could work out. */
    private void applyMapResult(Intent data) {
        latitude = data.getDoubleExtra(MapLocationActivity.EXTRA_LAT, 0);
        longitude = data.getDoubleExtra(MapLocationActivity.EXTRA_LNG, 0);
        fillFrom(data, MapLocationActivity.EXTRA_AREA, etArea, true);
        fillFrom(data, MapLocationActivity.EXTRA_CITY, etCity, true);
        fillFrom(data, MapLocationActivity.EXTRA_STATE, etState, true);
        fillFrom(data, MapLocationActivity.EXTRA_PINCODE, etPincode, true);
        fillFrom(data, MapLocationActivity.EXTRA_STREET, etStreet, false); // don't clobber a typed street
        updateMapStatus();
        Toast.makeText(activity, "Location pinned. Check the details and add your house/flat no.",
                Toast.LENGTH_LONG).show();
    }

    private static void fillFrom(Intent data, String key, EditText field, boolean overwrite) {
        String value = data.getStringExtra(key);
        if (value == null) return;
        if (overwrite || field.getText().toString().trim().isEmpty()) {
            field.setText(value);
            field.setError(null);
        }
    }

    private void updateMapStatus() {
        boolean pinned = latitude != null && longitude != null;
        tvMapStatus.setVisibility(pinned ? View.VISIBLE : View.GONE);
        btnPickOnMap.setText(pinned ? "Change map location" : "📍 Pick location on map");
    }

    /** Returns the entered (new) address, or null (after showing a message) if the input is invalid. */
    @Nullable
    Address readValidated() {
        return readValidated(0);
    }

    /** Same as {@link #readValidated()} but keeps the id of the address being edited. */
    @Nullable
    Address readValidated(int addressId) {
        String houseNo = text(etHouseNo);
        String street = text(etStreet);
        String area = text(etArea);
        String city = text(etCity);
        String state = text(etState);
        String pincode = text(etPincode);
        String landmark = text(etLandmark);
        String label = spinnerLabel.getSelectedItem().toString();

        if (houseNo.isEmpty() || area.isEmpty() || city.isEmpty() || state.isEmpty() || pincode.isEmpty()) {
            Toast.makeText(activity, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!PINCODE.matcher(pincode).matches()) {
            etPincode.setError("Enter a valid 6-digit pincode");
            etPincode.requestFocus();
            return null;
        }
        return new Address(addressId, label, houseNo, street, area, city, state, pincode, landmark,
                cbDefault.isChecked(), latitude, longitude);
    }

    /** Pre-fills the form with an existing address (edit mode). */
    void fill(Address address) {
        etHouseNo.setText(address.getHouseNo());
        etStreet.setText(address.getStreet());
        etArea.setText(address.getArea());
        etCity.setText(address.getCity());
        etState.setText(address.getState());
        etPincode.setText(address.getPincode());
        etLandmark.setText(address.getLandmark());
        cbDefault.setChecked(address.isDefault());
        latitude = address.getLatitude();
        longitude = address.getLongitude();
        updateMapStatus();
        for (int i = 0; i < spinnerLabel.getCount(); i++) {
            if (spinnerLabel.getItemAtPosition(i).toString().equals(address.getLabel())) {
                spinnerLabel.setSelection(i);
                break;
            }
        }
    }

    void clear() {
        etHouseNo.setText("");
        etStreet.setText("");
        etArea.setText("");
        etPincode.setText("");
        etLandmark.setText("");
        cbDefault.setChecked(false);
        spinnerLabel.setSelection(0);
        etCity.setText(DEFAULT_CITY);
        etState.setText(DEFAULT_STATE);
        latitude = null;
        longitude = null;
        updateMapStatus();
        etHouseNo.requestFocus();
    }

    /** The text fields restore themselves after rotation; the map pin needs saving explicitly. */
    void saveState(Bundle outState) {
        if (latitude != null && longitude != null) {
            outState.putDouble(KEY_LAT, latitude);
            outState.putDouble(KEY_LNG, longitude);
        }
    }

    void restoreState(@Nullable Bundle savedState) {
        if (savedState != null && savedState.containsKey(KEY_LAT)) {
            latitude = savedState.getDouble(KEY_LAT);
            longitude = savedState.getDouble(KEY_LNG);
            updateMapStatus();
        }
    }

    private static String text(EditText et) {
        return et.getText().toString().trim();
    }
}

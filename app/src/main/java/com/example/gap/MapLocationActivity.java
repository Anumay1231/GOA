package com.example.gap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lets the customer pin their delivery location on a Google Map. Returns the coordinates plus
 * reverse-geocoded address parts so the address form can be filled in automatically.
 */
public class MapLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Input (optional) and output extras.
    static final String EXTRA_LAT = "latitude";
    static final String EXTRA_LNG = "longitude";
    // Output only (may be missing if geocoding failed).
    static final String EXTRA_STREET = "street";
    static final String EXTRA_AREA = "area";
    static final String EXTRA_CITY = "city";
    static final String EXTRA_STATE = "state";
    static final String EXTRA_PINCODE = "pincode";

    private static final LatLng MUMBAI = new LatLng(19.0760, 72.8777);
    private static final String KEY_LAT = "lat", KEY_LNG = "lng";

    private GoogleMap map;
    private Marker marker;
    private LatLng selected;
    @Nullable private android.location.Address geocoded;
    private int geocodeToken;
    private TextView tvPicked;
    private FusedLocationProviderClient fused;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.HAS_MAPS_KEY) {
            Toast.makeText(this, "Maps are not configured in this build", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_map_location);

        Toolbar toolbar = findViewById(R.id.toolbarMap);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        SystemBars.apply(this, toolbar, true);

        tvPicked = findViewById(R.id.tvPickedAddress);
        fused = LocationServices.getFusedLocationProviderClient(this);

        // Start from: restored pin > pin passed in (editing an address) > Mumbai.
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_LAT)) {
            selected = new LatLng(savedInstanceState.getDouble(KEY_LAT), savedInstanceState.getDouble(KEY_LNG));
        } else if (getIntent().hasExtra(EXTRA_LAT) && getIntent().hasExtra(EXTRA_LNG)) {
            selected = new LatLng(getIntent().getDoubleExtra(EXTRA_LAT, MUMBAI.latitude),
                    getIntent().getDoubleExtra(EXTRA_LNG, MUMBAI.longitude));
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.btnMyLocation).setOnClickListener(v -> requestMyLocation());
        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> confirm());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false); // we have our own button
        enableMyLocationLayerIfPermitted();

        boolean hadPin = selected != null;
        LatLng start = hadPin ? selected : MUMBAI;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, hadPin ? 17f : 12f));
        marker = map.addMarker(new MarkerOptions().position(start).draggable(true).title("Deliver here"));
        setSelected(start);

        map.setOnMapClickListener(this::moveTo);
        map.setOnMapLongClickListener(this::moveTo);
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(@NonNull Marker m) { }
            @Override public void onMarkerDrag(@NonNull Marker m) { }
            @Override public void onMarkerDragEnd(@NonNull Marker m) { setSelected(m.getPosition()); }
        });

        // First time with no saved pin: jump to the user's location if they've already allowed it.
        if (!hadPin && hasLocationPermission()) {
            fetchCurrentLocation();
        }
    }

    private void moveTo(LatLng latLng) {
        if (marker != null) marker.setPosition(latLng);
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        setSelected(latLng);
    }

    private void setSelected(LatLng latLng) {
        selected = latLng;
        reverseGeocode(latLng);
    }

    // ------------------------------------------------------------------ my location

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMyLocation() {
        if (hasLocationPermission()) {
            fetchCurrentLocation();
        } else {
            locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        if (hasLocationPermission()) {
            enableMyLocationLayerIfPermitted();
            fetchCurrentLocation();
        } else {
            Toast.makeText(this, "Location permission denied. You can still tap the map to pick a spot.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission") // checked by hasLocationPermission()
    private void enableMyLocationLayerIfPermitted() {
        if (map != null && hasLocationPermission()) {
            map.setMyLocationEnabled(true);
        }
    }

    @SuppressLint("MissingPermission") // checked by hasLocationPermission()
    private void fetchCurrentLocation() {
        if (!hasLocationPermission() || map == null) return;
        tvPicked.setText("Finding your location…");
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location == null) {
                        Toast.makeText(this, "Couldn't get your location. Is location turned on?",
                                Toast.LENGTH_LONG).show();
                        if (selected != null) reverseGeocode(selected);
                        return;
                    }
                    LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                    if (marker != null) marker.setPosition(here);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 17f));
                    setSelected(here);
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Couldn't get your location", Toast.LENGTH_SHORT).show();
                    if (selected != null) reverseGeocode(selected);
                });
    }

    // ------------------------------------------------------------------ reverse geocoding

    private void reverseGeocode(LatLng latLng) {
        final int token = ++geocodeToken; // ignore results for pins the user has since moved
        geocoded = null;
        tvPicked.setText("Finding address…");
        if (!Geocoder.isPresent()) {
            showCoordinates(latLng);
            return;
        }
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<android.location.Address> addresses) {
                    runOnUiThread(() -> onGeocoded(token, latLng, addresses));
                }

                @Override
                public void onError(@Nullable String errorMessage) {
                    runOnUiThread(() -> onGeocoded(token, latLng, null));
                }
            });
        } else {
            geocodeExecutor.execute(() -> {
                List<android.location.Address> result;
                try {
                    result = legacyGeocode(geocoder, latLng);
                } catch (Exception e) {
                    result = null;
                }
                List<android.location.Address> finalResult = result;
                runOnUiThread(() -> onGeocoded(token, latLng, finalResult));
            });
        }
    }

    @SuppressWarnings("deprecation") // blocking API is the only option below Android 13; runs off the main thread
    private static List<android.location.Address> legacyGeocode(Geocoder geocoder, LatLng latLng) throws java.io.IOException {
        return geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
    }

    private void onGeocoded(int token, LatLng latLng, @Nullable List<android.location.Address> addresses) {
        if (token != geocodeToken || isFinishing()) return;
        if (addresses == null || addresses.isEmpty()) {
            showCoordinates(latLng);
            return;
        }
        geocoded = addresses.get(0);
        String line = geocoded.getMaxAddressLineIndex() >= 0 ? geocoded.getAddressLine(0) : null;
        tvPicked.setText(line != null ? line : formatCoords(latLng));
    }

    private void showCoordinates(LatLng latLng) {
        tvPicked.setText(formatCoords(latLng) + "\n(address lookup unavailable – fill in the details yourself)");
    }

    private static String formatCoords(LatLng latLng) {
        return String.format(Locale.ROOT, "%.5f, %.5f", latLng.latitude, latLng.longitude);
    }

    // ------------------------------------------------------------------ result

    private void confirm() {
        if (selected == null) {
            Toast.makeText(this, "Please pick a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_LAT, selected.latitude);
        data.putExtra(EXTRA_LNG, selected.longitude);
        if (geocoded != null) {
            putIfPresent(data, EXTRA_STREET, geocoded.getThoroughfare());
            putIfPresent(data, EXTRA_AREA, geocoded.getSubLocality());
            putIfPresent(data, EXTRA_CITY, geocoded.getLocality());
            putIfPresent(data, EXTRA_STATE, geocoded.getAdminArea());
            putIfPresent(data, EXTRA_PINCODE, geocoded.getPostalCode());
        }
        setResult(RESULT_OK, data);
        finish();
    }

    private static void putIfPresent(Intent data, String key, @Nullable String value) {
        if (value != null && !value.trim().isEmpty()) data.putExtra(key, value.trim());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selected != null) {
            outState.putDouble(KEY_LAT, selected.latitude);
            outState.putDouble(KEY_LNG, selected.longitude);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geocodeExecutor.shutdownNow();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

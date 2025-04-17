package app.gps.gps;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FieldMeasurementActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Polygon currentPolygon;
    private List<LatLng> polygonPoints = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private TextView areaTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private Button calculateButton;
    private EditText locationSearchEditText;
    private Button searchButton;

    // 🔄 Added for Refresh Button
    private ImageView refreshButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_measurement);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_field);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        areaTextView = findViewById(R.id.areaTextView);
        calculateButton = findViewById(R.id.calculateButton);
        locationSearchEditText = findViewById(R.id.locationSearchEditText);
        searchButton = findViewById(R.id.searchButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        calculateButton.setOnClickListener(v -> {
            if (polygonPoints.size() > 2) {
                double area = calculateAreaOfPolygon(polygonPoints);
                areaTextView.setText("Area: " + String.format("%.2f", area) + " acres");
            } else {
                Toast.makeText(FieldMeasurementActivity.this, "Please select at least 3 points.", Toast.LENGTH_SHORT).show();
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = locationSearchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchLocation(query);
            }
        });

        // 🔄 Initialize and set listener for Refresh Button
        refreshButton = findViewById(R.id.logoImageView);
        refreshButton.setOnClickListener(v -> getCurrentLocation());
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    Address address = addresses.get(0);
                                    String addressString = address.getAddressLine(0);

                                    mMap.addMarker(new MarkerOptions()
                                            .position(currentLocation)
                                            .title("You are here")
                                            .snippet(addressString));

                                    Toast.makeText(FieldMeasurementActivity.this, "Location: " + addressString, Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        mMap.setOnMapClickListener(latLng -> {
            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
            markers.add(marker);
            polygonPoints.add(latLng);
            updatePolygon();
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                int index = markers.indexOf(marker);
                if (index != -1) {
                    polygonPoints.set(index, marker.getPosition());
                    updatePolygon();
                }
            }
        });
    }

    private void updatePolygon() {
        if (currentPolygon != null) {
            currentPolygon.remove();
        }
        PolygonOptions polygonOptions = new PolygonOptions().addAll(polygonPoints);
        currentPolygon = mMap.addPolygon(polygonOptions);
    }

    private void searchLocation(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title("Searched Location")
                        .snippet(address.getAddressLine(0)));

                Toast.makeText(this, "Location found: " + address.getAddressLine(0), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location not found!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateAreaOfPolygon(List<LatLng> points) {
        if (points.size() < 3) return 0;

        double total = 0.0;
        final double radius = 6378137.0;

        for (int i = 0; i < points.size(); i++) {
            LatLng p1 = points.get(i);
            LatLng p2 = points.get((i + 1) % points.size());

            double lat1 = Math.toRadians(p1.latitude);
            double lon1 = Math.toRadians(p1.longitude);
            double lat2 = Math.toRadians(p2.latitude);
            double lon2 = Math.toRadians(p2.longitude);

            total += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
        }

        double areaInSquareMeters = Math.abs(total * radius * radius / 2.0);
        return areaInSquareMeters / 4046.86;
    }

    // 🔄 Added: Method to get current accurate location
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));

                        mMap.addMarker(new MarkerOptions()
                                .position(currentLatLng)
                                .title("Live Location"));

                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String address = addresses.get(0).getAddressLine(0);
                                Toast.makeText(this, "Updated Location: " + address, Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation(); // 🔄 Changed from onMapReady to getCurrentLocation
        } else {
            Toast.makeText(this, "Location permission is required to get the current location.", Toast.LENGTH_SHORT).show();
        }
    }
}

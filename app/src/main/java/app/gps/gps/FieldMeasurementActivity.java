package app.gps.gps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import java.util.ArrayList;
import java.util.List;
import com.google.maps.android.SphericalUtil;


public class FieldMeasurementActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Polygon currentPolygon; // To store the polygon the user draws
    private List<LatLng> polygonPoints = new ArrayList<>(); // Store the points of the polygon
    private List<Marker> markers = new ArrayList<>(); // Store draggable markers
    private TextView areaTextView; // Reference to the TextView that will display the area
    private FusedLocationProviderClient fusedLocationClient;
    private Button calculateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_measurement);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_field);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Initialize the TextView to show the area
        areaTextView = findViewById(R.id.areaTextView);

        // Initialize the calculate button
        calculateButton = findViewById(R.id.calculateButton);
        calculateButton.setOnClickListener(v -> {
            if (polygonPoints.size() > 2) {
                double area = calculateAreaOfPolygon(polygonPoints);
                areaTextView.setText("Area: " + String.format("%.2f", area) + " acres");
            } else {
                Toast.makeText(FieldMeasurementActivity.this, "Please select at least 3 points.", Toast.LENGTH_SHORT).show();
            }
        });
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set the map to satellite view
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // Get the user's current location and set the map's camera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        }
                    });
        } else {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Set up a click listener to add points to the polygon
        mMap.setOnMapClickListener(latLng -> {
            // Add a draggable marker at each clicked point
            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true)); // Draggable marker
            markers.add(marker);
            polygonPoints.add(latLng);

            // Update the polygon
            updatePolygon();
        });


        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // When dragging ends, update the polygon points
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

        // Add the polygon to the map with updated points
        PolygonOptions polygonOptions = new PolygonOptions().addAll(polygonPoints);
        currentPolygon = mMap.addPolygon(polygonOptions);
    }

    // Function to calculate area of a polygon (Shoelace formula)


    // Accurate area calculation using spherical geometry
    private double calculateAreaOfPolygon(List<LatLng> points) {
        if (points.size() < 3) return 0;

        double total = 0.0;
        final double radius = 6378137.0; // Earth’s radius in meters

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
        double areaInAcres = areaInSquareMeters / 4046.86;

        // Debug log
        Log.d("AreaDebug", "Square Meters: " + areaInSquareMeters);
        Log.d("AreaDebug", "Acres: " + areaInAcres);

        Toast.makeText(this, "Area: " + areaInAcres + " acres", Toast.LENGTH_SHORT).show();
        return areaInAcres;
    }



    // Handle the permission request result (required for new android versions)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now get the location
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(this, location -> {
                                if (location != null) {
                                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                                }
                            });
                }
            } else {
                Toast.makeText(this, "Location permission is required to get the current location.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

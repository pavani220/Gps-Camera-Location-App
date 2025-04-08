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

public class FieldMeasurementActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Polygon currentPolygon; // To store the polygon the user draws
    private List<LatLng> polygonPoints = new ArrayList<>(); // Store the points of the polygon
    private List<Marker> markers = new ArrayList<>(); // Store draggable markers
    private TextView areaTextView; // Reference to the TextView that will display the area
    private FusedLocationProviderClient fusedLocationClient;
    private Button calculateButton; // Reference to the button to calculate area

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
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Calculate area when button is clicked
                if (polygonPoints.size() > 2) {
                    double area = calculateAreaOfPolygon(polygonPoints);
                    areaTextView.setText("Area: " + area + " hectares");
                } else {
                    Toast.makeText(FieldMeasurementActivity.this, "Please select at least 3 points to form a polygon.", Toast.LENGTH_SHORT).show();
                }
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
            markers.add(marker); // Add to list of markers
            polygonPoints.add(latLng); // Add point to polygon

            // Update the polygon
            updatePolygon();
        });

        // Set up a listener for marker drag events
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
                    updatePolygon(); // Re-render the polygon with updated points
                }
            }
        });
    }

    private void updatePolygon() {
        if (currentPolygon != null) {
            currentPolygon.remove(); // Remove the previous polygon
        }

        // Add the polygon to the map with updated points
        PolygonOptions polygonOptions = new PolygonOptions().addAll(polygonPoints);
        currentPolygon = mMap.addPolygon(polygonOptions);
    }

    // Function to calculate area of a polygon (Shoelace formula)
    private double calculateAreaOfPolygon(List<LatLng> points) {
        double area = 0;
        int j = points.size() - 1;

        // Calculate the area using the Shoelace formula
        for (int i = 0; i < points.size(); i++) {
            area += (points.get(j).longitude + points.get(i).longitude) * (points.get(j).latitude - points.get(i).latitude);
            j = i;
        }

        // Convert the area to hectares (1 square meter = 1e-6 hectares)
        double areaInHectares = Math.abs(area * 0.5 * 1E-6);

        // Debugging: Log the calculated area in square meters for debugging
        Toast.makeText(this, "Calculated Area in Square Meters: " + area + " m²", Toast.LENGTH_SHORT).show();

        // Log the polygon points and area for debugging
        Log.d("PolygonPoints", "Points: " + points.toString());
        Log.d("AreaCalculation", "Calculated Area: " + areaInHectares + " hectares");

        return areaInHectares;
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

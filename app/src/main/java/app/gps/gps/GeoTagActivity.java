package app.gps.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GeoTagActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;

    private GoogleMap mMap;
    private Polygon currentPolygon;
    private List<LatLng> polygonPoints = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();

    private TextView areaTextView, areaTextView1, areaTextView2;
    private Button calculateButton, downloadReportButton;
    private EditText locationSearchEditText;
    private Button searchButton;
    private ImageView refreshButton;

    private FusedLocationProviderClient fusedLocationClient;
    private String pendingReportContent = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geotag);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_field);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        areaTextView = findViewById(R.id.areaTextView);
        areaTextView1 = findViewById(R.id.areaTextView1);
        areaTextView2 = findViewById(R.id.areaTextView2);
        calculateButton = findViewById(R.id.calculateButton);
        locationSearchEditText = findViewById(R.id.locationSearchEditText);
        searchButton = findViewById(R.id.searchButton);
        refreshButton = findViewById(R.id.logoImageView);
        downloadReportButton = findViewById(R.id.downloadReportButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        calculateButton.setOnClickListener(v -> {
            if (polygonPoints.size() > 2) {
                StringBuilder coordinatesBuilder = new StringBuilder();
                for (int i = 0; i < polygonPoints.size(); i++) {
                    LatLng point = polygonPoints.get(i);
                    coordinatesBuilder.append("Point ").append(i + 1).append(": ")
                            .append("Lat = ").append(point.latitude)
                            .append(", Lng = ").append(point.longitude).append("\n");
                }
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                areaTextView.setText("Polygon Edges:\n" + coordinatesBuilder);
                areaTextView1.setText("");
                areaTextView2.setText("Timestamp: " + timestamp);
            } else {
                Toast.makeText(this, "Please select at least 3 points to form a polygon.", Toast.LENGTH_SHORT).show();
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = locationSearchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchLocation(query);
            }
        });

        refreshButton.setOnClickListener(v -> getCurrentLocation());

        downloadReportButton.setOnClickListener(v -> {
            if (polygonPoints.size() > 2) {
                StringBuilder coordinatesBuilder = new StringBuilder();
                for (int i = 0; i < polygonPoints.size(); i++) {
                    LatLng point = polygonPoints.get(i);
                    coordinatesBuilder.append("Point ").append(i + 1).append(": ")
                            .append("Lat = ").append(point.latitude)
                            .append(", Lng = ").append(point.longitude).append("\n");
                }
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String reportContent = "Polygon Coordinates:\n" + coordinatesBuilder + "\nTimestamp: " + timestamp;

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pendingReportContent = reportContent;
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                } else {
                    saveReportToDownloads(reportContent);
                }
            } else {
                Toast.makeText(this, "Please select at least 3 points to form a polygon.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveReportToDownloads(String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "GeoTagReport.txt");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        outputStream.write(content.getBytes());
                        Toast.makeText(this, "Report saved to Downloads folder", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error saving the report", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to create file in Downloads", Toast.LENGTH_SHORT).show();
            }
        } else {
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File reportFile = new File(downloadsFolder, "GeoTagReport.txt");
            try {
                if (!downloadsFolder.exists()) downloadsFolder.mkdirs();
                FileOutputStream fos = new FileOutputStream(reportFile);
                fos.write(content.getBytes());
                fos.close();
                Toast.makeText(this, "Report saved to Downloads folder", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving the report", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        mMap.setOnMapClickListener(latLng -> {
            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
            markers.add(marker);
            polygonPoints.add(latLng);
            updatePolygon();
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            public void onMarkerDragStart(Marker marker) {}
            public void onMarkerDrag(Marker marker) {}
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
        if (currentPolygon != null) currentPolygon.remove();
        currentPolygon = mMap.addPolygon(new PolygonOptions().addAll(polygonPoints));
    }

    private void searchLocation(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                mMap.addMarker(new MarkerOptions().position(location).title("Searched Location"));
            } else {
                Toast.makeText(this, "Location not found!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Live Location"));
                        try {
                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (!addresses.isEmpty()) {
                                Toast.makeText(this, "Updated Location: " + addresses.get(0).getAddressLine(0), Toast.LENGTH_SHORT).show();
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
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingReportContent != null) {
                saveReportToDownloads(pendingReportContent);
                pendingReportContent = null;
            }
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
        }
    }
}

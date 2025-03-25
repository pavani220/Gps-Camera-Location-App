package com.example.gps;

import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.location.Address;
import android.location.Geocoder;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView capturedImageView, logoImageView;
    private TextView textLatitude, textLongitude, textAddress, textTimestamp;
    private GoogleMap googleMap;
    private double latitude, longitude;
    private String currentPhotoPath;
    private String timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Get the passed data from MainActivity
        currentPhotoPath = getIntent().getStringExtra("photoPath");
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);
        timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Initialize UI elements
        capturedImageView = findViewById(R.id.capturedImage);
        logoImageView = findViewById(R.id.logo);
        textLatitude = findViewById(R.id.text_latitude);
        textLongitude = findViewById(R.id.text_longitude);
        textAddress = findViewById(R.id.text_address);
        textTimestamp = findViewById(R.id.text_timestamp);

        // Display the image
        File imgFile = new  File(currentPhotoPath);
        if(imgFile.exists()){
            capturedImageView.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
        }

        // Set timestamp
        textTimestamp.setText("Timestamp: " + timestamp);

        // Set latitude and longitude
        textLatitude.setText("Latitude: " + latitude);
        textLongitude.setText("Longitude: " + longitude);

        // Get the address
        getLocationName(latitude, longitude);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getLocality();
                textAddress.setText("Address: " + (locationName != null ? locationName : "Unknown"));
            } else {
                textAddress.setText("Address: Unknown");
            }
        } catch (IOException e) {
            e.printStackTrace();
            textAddress.setText("Address: Unknown");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Enable location-related features
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        this.googleMap.setMyLocationEnabled(true);

        // Add a marker to the map for the current location
        LatLng currentLocation = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
    }
}

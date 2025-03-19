package com.example.gps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    private ImageView imageView;
    private TextView textViewLatitude, textViewLongitude, textViewLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textViewLatitude = findViewById(R.id.textView_latitude);
        textViewLongitude = findViewById(R.id.textView_longitude);
        textViewLocation = findViewById(R.id.textView_location); // TextView for location name
        Button takePhotoButton = findViewById(R.id.button_take_photo);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        // Check if the location permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());
    }

    // Dispatch the camera intent to take a photo
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            // Get the photo from the intent data
            imageView.setImageURI(data.getData());
            getLastLocation(); // Fetch and show the location
        }
    }

    // Fetch the last known location of the device
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            textViewLatitude.setText("Latitude: " + latitude);
                            textViewLongitude.setText("Longitude: " + longitude);

                            // Use Geocoder to get the location name
                            getLocationName(latitude, longitude);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Use Geocoder to get the location name from latitude and longitude
    private void getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getLocality();  // Get the locality (city, town, etc.)
                if (locationName != null) {
                    textViewLocation.setText("Location: " + locationName);
                } else {
                    textViewLocation.setText("Location: Unknown");
                }
            } else {
                textViewLocation.setText("Location: Unknown");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to get location name", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Camera permission granted
        } else if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission granted
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}

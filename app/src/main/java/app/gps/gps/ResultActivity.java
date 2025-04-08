package app.gps.gps;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.location.Address;
import android.location.Geocoder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
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
        File imgFile = new File(currentPhotoPath);
        if (imgFile.exists()) {
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

        // Set the download functionality for the image
        findViewById(R.id.button).setOnClickListener(v -> {
            saveImageToGallery();
        });
    }

    private void getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            // Fetching the address from the provided latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            // Check if we got a valid address
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Log the address components for debugging
                Log.d("Address", "Address found: " + address.toString());

                // Getting the components of the address
                String subLocality = address.getSubLocality();
                String locality = address.getLocality();
                String adminArea = address.getAdminArea();
                String countryName = address.getCountryName();
                String postalCode = address.getPostalCode();
                String featureName = address.getFeatureName();

                // Combine the components into a full address
                StringBuilder fullAddress = new StringBuilder();

                if (subLocality != null) fullAddress.append(subLocality).append(", ");
                if (locality != null) fullAddress.append(locality).append(", ");
                if (adminArea != null) fullAddress.append(adminArea).append(", ");
                if (postalCode != null) fullAddress.append(postalCode).append(", ");
                if (countryName != null) fullAddress.append(countryName);

                // If the address is still incomplete, add the feature name (if available)
                if (fullAddress.length() == 0 && featureName != null) {
                    fullAddress.append(featureName);
                }

                // Set the address to the TextView
                if (fullAddress.length() == 0) {
                    textAddress.setText("Address: Unknown");
                } else {
                    textAddress.setText("Address: " + fullAddress.toString());
                }
            } else {
                textAddress.setText("Address: Unknown");
            }
        } catch (IOException e) {
            e.printStackTrace();
            textAddress.setText("Address: Unknown");
        }
    }

    private void saveImageToGallery() {
        // Get the bitmap of the captured image
        File imgFile = new File(currentPhotoPath);
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            // Save the image to the gallery
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "Captured Image");
                values.put(MediaStore.Images.Media.DESCRIPTION, "Image from GPS capture");
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

                // Get the content resolver and insert the image into the MediaStore
                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                FileOutputStream out = new FileOutputStream(imgFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.close();

                // Show a toast message confirming the image has been saved
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Enable location-related features
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If permissions are not granted, you should handle it, but for this example, we will simply return
            return;
        }
        this.googleMap.setMyLocationEnabled(true);

        // Add a marker to the map for the current location
        LatLng currentLocation = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
    }
}
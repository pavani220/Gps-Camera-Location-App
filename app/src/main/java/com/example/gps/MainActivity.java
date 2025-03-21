package com.example.gps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    private ImageView imageView;
    private TextView textViewLatitude, textViewLongitude, textViewLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentPhotoPath;
    private GoogleMap googleMap;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textViewLatitude = findViewById(R.id.textView_latitude);
        textViewLongitude = findViewById(R.id.textView_longitude);
        textViewLocation = findViewById(R.id.textView_location);
        Button takePhotoButton = findViewById(R.id.button_take_photo);
        Button findButton = findViewById(R.id.button_find); // Find button

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set onClickListener for Take Photo Button
        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());

        // Set onClickListener for Find Button
        findButton.setOnClickListener(v -> {
            // Open the FieldMeasurementActivity when the "Find" button is clicked
            Intent intent = new Intent(MainActivity.this, FieldMeasurementActivity.class);
            startActivity(intent);
        });
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
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            // Display latitude and longitude
                            textViewLatitude.setText("Latitude: " + latitude);
                            textViewLongitude.setText("Longitude: " + longitude);

                            // Get location name from the coordinates
                            getLocationName(latitude, longitude);

                            // Make map fragment visible after the location is fetched
                            findViewById(R.id.map).setVisibility(View.VISIBLE);

                            // Move map camera to the current location
                            if (googleMap != null) {
                                LatLng currentLocation = new LatLng(latitude, longitude);
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                                googleMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here"));
                            }

                            // Make latitude, longitude, location text views visible
                            textViewLatitude.setVisibility(View.VISIBLE);
                            textViewLongitude.setVisibility(View.VISIBLE);
                            textViewLocation.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getLocality();
                textViewLocation.setText("Location: " + (locationName != null ? locationName : "Unknown"));
            } else {
                textViewLocation.setText("Location: Unknown");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to get location name", Toast.LENGTH_SHORT).show();
        }
    }

    // Dispatch the camera intent to take a photo
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.gps.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    // Handle activity result for photo capture
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
            setPic();
            getLastLocation(); // Get the location and update UI
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(View.VISIBLE); // Make image visible
    }

    // Create a file to save the photo
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }
}

package app.gps.gps;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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

    private ImageView capturedImageView;
    private TextView textLatitude, textLongitude, textAddress, textTimestamp;
    private RelativeLayout resultLayout;
    private GoogleMap googleMap;

    private double latitude, longitude;
    private String currentPhotoPath;
    private String timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Get data
        currentPhotoPath = getIntent().getStringExtra("photoPath");
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);
        timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Initialize views
        capturedImageView = findViewById(R.id.capturedImage);
        textLatitude = findViewById(R.id.text_latitude);
        textLongitude = findViewById(R.id.text_longitude);
        textAddress = findViewById(R.id.text_address);
        textTimestamp = findViewById(R.id.text_timestamp);
        resultLayout = findViewById(R.id.resultLayout);

        // Display captured image
        File imgFile = new File(currentPhotoPath);
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            capturedImageView.setImageBitmap(bitmap);
        }

        // Display location info
        textLatitude.setText("Latitude: " + latitude);
        textLongitude.setText("Longitude: " + longitude);
        textTimestamp.setText("Timestamp: " + timestamp);
        getLocationName(latitude, longitude);

        // Load Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Save button
        ImageView saveButton = findViewById(R.id.button);
        saveButton.setOnClickListener(v -> saveScreenshot());
    }

    private void getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder fullAddress = new StringBuilder();
                if (address.getSubLocality() != null) fullAddress.append(address.getSubLocality()).append(", ");
                if (address.getLocality() != null) fullAddress.append(address.getLocality()).append(", ");
                if (address.getAdminArea() != null) fullAddress.append(address.getAdminArea()).append(", ");
                if (address.getPostalCode() != null) fullAddress.append(address.getPostalCode()).append(", ");
                if (address.getCountryName() != null) fullAddress.append(address.getCountryName());
                textAddress.setText("Address: " + fullAddress.toString());
            } else {
                textAddress.setText("Address: Unknown");
            }
        } catch (IOException e) {
            e.printStackTrace();
            textAddress.setText("Address: Unknown");
        }
    }

    private void saveScreenshot() {
        if (googleMap == null) return;

        // Step 1: Capture the map
        googleMap.snapshot(mapBitmap -> {

            // Step 2: Ensure the full layout is measured and drawn
            resultLayout.setDrawingCacheEnabled(false);
            resultLayout.setDrawingCacheEnabled(true);
            resultLayout.buildDrawingCache();

            int width = resultLayout.getWidth();
            int height = resultLayout.getHeight();

            if (width == 0 || height == 0) {
                resultLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                width = resultLayout.getMeasuredWidth();
                height = resultLayout.getMeasuredHeight();
                resultLayout.layout(0, 0, width, height);
            }

            Bitmap layoutBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(layoutBitmap);
            resultLayout.draw(canvas); // draw the entire screen

            // Step 3: Draw the map bitmap on top
            View mapView = findViewById(R.id.map);
            int[] location = new int[2];
            mapView.getLocationInWindow(location);
            int mapX = mapView.getLeft();
            int mapY = mapView.getTop();

            canvas.drawBitmap(mapBitmap, mapX, mapY, null);

            // Step 4: Save to gallery
            saveBitmapToGallery(layoutBitmap);
        });
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Screenshot_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GPS_Camera");

        try {
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (FileOutputStream out = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }

                Toast.makeText(this, "Screenshot saved to gallery", Toast.LENGTH_SHORT).show();

                // Share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share Screenshot"));
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error saving screenshot", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        googleMap.setMyLocationEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        LatLng location = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(location).title("Captured Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        String address = textAddress.getText().toString().replace("Address: ", "");
        String mapsLink = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;

        String message = "📍 Location Details:\n"
                + "Latitude: " + latitude + "\n"
                + "Longitude: " + longitude + "\n"
                + "Address: " + address + "\n"
                + "Timestamp: " + timestamp + "\n"
                + "Map: " + mapsLink + "\n\n"
                + "Shared via GPS Camera App";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(intent, "Share Location Info"));
        return true;
    }
}

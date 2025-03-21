package com.example.gps;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.example.gps.MainActivity;
import com.example.gps.R;

public class WelcomeActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 5000; // 5 seconds delay for the splash screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome); // Make sure the correct layout is used

        // Initialize Views
        TextView welcomeText = findViewById(R.id.welcome_text);
        TextView appNameText = findViewById(R.id.app_name);
        Button startButton = findViewById(R.id.start_button);

        // Optionally, you can set up text or modify UI elements if needed
        welcomeText.setText("Welcome To");
        appNameText.setText("VURIMI APP");

        // Set an OnClickListener for the button (optional for manual navigation)
        startButton.setOnClickListener(v -> navigateToHomeScreen());

        // Handler for auto transitioning after 5 seconds
        new Handler().postDelayed(this::navigateToHomeScreen, SPLASH_TIME_OUT);
    }

    // This method navigates to MainActivity after 5 seconds or when button is clicked
    private void navigateToHomeScreen() {
        // Navigate to MainActivity
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close the WelcomeActivity
    }
}

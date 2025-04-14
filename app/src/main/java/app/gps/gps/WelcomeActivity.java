package app.gps.gps;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000; // 3 seconds delay for the splash screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome); // Correct layout used

        // Initialize SessionManager
        SessionManager sessionManager = new SessionManager(this);

        // Check if the user is logged in
        new Handler().postDelayed(() -> {
            if (sessionManager.isLoggedIn()) {
                // If logged in, directly go to MainActivity
                navigateToMainActivity();
            } else {
                // If not logged in, show SignupActivity
                navigateToSignupActivity();
            }
        }, SPLASH_TIME_OUT);
    }

    private void navigateToSignupActivity() {
        // Navigate to SignupActivity if user is not logged in
        Intent intent = new Intent(WelcomeActivity.this, SignupActivity.class);
        startActivity(intent);
        finish(); // Close WelcomeActivity
    }

    private void navigateToMainActivity() {
        // Navigate to MainActivity if user is logged in
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close WelcomeActivity
    }
}

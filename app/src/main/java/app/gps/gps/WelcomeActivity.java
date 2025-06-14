package app.gps.gps;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button startButton = findViewById(R.id.start_button);

        // Optional: allow user to skip splash manually
        startButton.setOnClickListener(v -> navigateToNextScreen());

        // Auto navigate after delay
        new Handler().postDelayed(this::navigateToNextScreen, SPLASH_TIME_OUT);
    }

    private void navigateToNextScreen() {
        SessionManager sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {

            startActivity(new Intent(this, MainActivity.class));
        } else {

            startActivity(new Intent(this, SignupActivity.class));
        }

        finish();
    }
}





package app.gps.gps;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    Button loginBtn;
    TextView toSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        toSignup = findViewById(R.id.toSignup);

        // Redirect to SignupActivity if user doesn't have an account
        toSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish(); // Close LoginActivity
        });

        // Handle login button click
        loginBtn.setOnClickListener(v -> {
            // Here you would typically validate login credentials

            // Mark the user as logged in after successful login
            SessionManager sessionManager = new SessionManager(LoginActivity.this);
            sessionManager.setLogin(true); // Set login status to true

            // Go to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close LoginActivity to prevent going back
        });
    }
}

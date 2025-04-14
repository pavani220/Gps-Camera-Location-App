package app.gps.gps;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {
    EditText username, email, password;
    Button signupBtn;
    TextView toLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        signupBtn = findViewById(R.id.signupBtn);
        toLogin = findViewById(R.id.toLogin);

        // Redirect to LoginActivity if the user already has an account
        toLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish(); // Close SignupActivity
        });

        // Handle signup button click
        signupBtn.setOnClickListener(v -> {
            // Here you would typically add logic to validate and save user data

            // Mark the user as logged in after successful signup
            SessionManager sessionManager = new SessionManager(SignupActivity.this);
            sessionManager.setLogin(true); // Set login status to true

            // Go to MainActivity
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close SignupActivity to prevent going back
        });
    }
}

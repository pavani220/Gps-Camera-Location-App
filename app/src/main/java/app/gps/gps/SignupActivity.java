package app.gps.gps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    EditText username, email, password;
    Button signupBtn;
    TextView toLogin;

    FirebaseAuth auth;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Realtime Database
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance("https://login-vdrones-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        // UI Components
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        signupBtn = findViewById(R.id.signupBtn);
        toLogin = findViewById(R.id.toLogin);

        // Navigate to LoginActivity
        toLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        // Sign Up Button Click
        signupBtn.setOnClickListener(v -> {
            String enteredUsername = username.getText().toString().trim();
            String enteredEmail = email.getText().toString().trim();
            String enteredPassword = password.getText().toString().trim();

            // Input validation
            if (enteredUsername.isEmpty()) {
                username.setError("Username is required");
                return;
            }
            if (enteredEmail.isEmpty()) {
                email.setError("Email is required");
                return;
            }
            if (enteredPassword.isEmpty()) {
                password.setError("Password is required");
                return;
            }

            // Register with Firebase Authentication
            auth.createUserWithEmailAndPassword(enteredEmail, enteredPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = auth.getCurrentUser().getUid();

                            // Store user profile data in database (without password)
                            UserModel newUser = new UserModel(userId, enteredUsername, enteredEmail, null);

                            usersRef.child(userId).setValue(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(SignupActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                                        Log.d("SignupActivity", "User created: " + userId);
                                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(SignupActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}





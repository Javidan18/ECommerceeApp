package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText email, password;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
    }

    public void signIn(View view) {
        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail)) {
            showToast("Please enter your email address!");
            return;
        }
        if (TextUtils.isEmpty(userPassword)) {
            showToast("Please enter your password!");
            return;
        }
        if (userPassword.length() < 6) {
            showToast("Password is too short! Enter at least 6 characters.");
            return;
        }

        auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            showToast("Login Successful");
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // Check for unregistered account error
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("There is no user record corresponding to this identifier")) {
                                showToast("This email is not registered. Please sign up.");
                            } else {
                                handleLoginError(errorMessage);
                            }
                        }
                    }
                });
    }

    public void signUp(View view) {
        startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
    }

    private void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleLoginError(String errorMessage) {
        if (errorMessage.contains("The password is invalid")) {
            showToast("Invalid password! Please try again.");
        } else if (errorMessage.contains("badly formatted")) {
            showToast("Invalid email format!");
        } else {
            showToast("Error: " + errorMessage);
        }
    }
}
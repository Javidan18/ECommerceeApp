package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.MailCode.MailAPI;
import com.example.ecommerceapp.OtpCode.OtpCode;
import com.example.ecommerceapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegistrationActivity extends AppCompatActivity {

    EditText name, email, password, otpCode;
    private FirebaseAuth auth;
    private String generatedOtp = ""; // OTP kodu
    private long lastOtpSentTime = 0; // Son OTP gönderim zamanı

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
            finish();
        }

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        otpCode = findViewById(R.id.otp_code);

        sharedPreferences = getSharedPreferences("onBoardingScreen", MODE_PRIVATE);
        boolean isFirstTime = sharedPreferences.getBoolean("firstTime", true);

        if (isFirstTime) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstTime", false);
            editor.apply();

            Intent intent = new Intent(RegistrationActivity.this, OnBoardingActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void sendOtpCode(View view) {
        String userEmail = email.getText().toString();

        // Mail adresi boşsa hata mesajı göster
        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Enter Email Address!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Her 10 saniyede bir kod gönderme kontrolü
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastOtpSentTime < 10000) { // 10 saniye kontrolü
            long remainingTime = 10 - ((currentTime - lastOtpSentTime) / 1000);
            Toast.makeText(this, "Please wait " + remainingTime + " seconds before requesting a new OTP!", Toast.LENGTH_SHORT).show();
            return;
        }

        // OTP oluştur ve gönder
        OtpCode otpCodeGenerator = new OtpCode();
        generatedOtp = otpCodeGenerator.invoke(userEmail);
        lastOtpSentTime = currentTime; // Son OTP gönderim zamanını güncelle

        Toast.makeText(this, "OTP code sent to your email!", Toast.LENGTH_SHORT).show();
    }

    public void signup(View view) {
        String userName = name.getText().toString();
        String userEmail = email.getText().toString();
        String userPassword = password.getText().toString();
        String enteredOtp = otpCode.getText().toString(); // Kullanıcının girdiği OTP

        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, "Enter Name!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Enter Email Address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(userPassword)) {
            Toast.makeText(this, "Enter Password!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userPassword.length() < 6) {
            Toast.makeText(this, "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kullanıcının OTP kodunu girip girmediğini kontrol et
        if (TextUtils.isEmpty(enteredOtp)) {
            Toast.makeText(this, "Enter OTP Code!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kullanıcının girdiği OTP'nin doğru olup olmadığını kontrol et
        if (!enteredOtp.equals(generatedOtp)) {
            Toast.makeText(this, "Invalid OTP Code!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(RegistrationActivity.this, "Successfully Registered", Toast.LENGTH_SHORT).show();

                    // E-posta gönderme işlemi
                    String subject = "Successful Registration / Kayıt Başarılı";
                    String messageBody = "Your registration for the E-commerce app has been successfully completed. Welcome! / E-commerce uygulamasına kaydınız başarıyla olmuştur. Hoş geldiniz!";
                    new MailAPI(userEmail, subject, messageBody, "-1").execute();

                    // Kayıt sonrası LoginActivity'e yönlendir
                    startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(RegistrationActivity.this, "Registration Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

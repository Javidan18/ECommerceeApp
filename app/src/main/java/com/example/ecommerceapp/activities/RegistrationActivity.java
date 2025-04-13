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

        // Kullanıcı zaten giriş yapmışsa MainActivity'e yönlendir
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
            finish();
            return; // onCreate'in geri kalanının çalışmasını engelle
        }

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        otpCode = findViewById(R.id.otp_code); // Bu ID'nin layout'ta olduğundan emin olun

        // OnBoarding ekranı kontrolü
        sharedPreferences = getSharedPreferences("onBoardingScreen", MODE_PRIVATE);
        boolean isFirstTime = sharedPreferences.getBoolean("firstTime", true);

        if (isFirstTime) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstTime", false);
            editor.apply();

            Intent intent = new Intent(RegistrationActivity.this, OnBoardingActivity.class);
            startActivity(intent);
            finish();
            return; // OnBoarding gösteriliyorsa Registration'ın geri kalanıyla devam etme
        }
    }

    public void sendOtpCode(View view) {
        // --- YENİ EKLENEN KONTROLLER ---
        String userName = name.getText().toString();
        String userEmail = email.getText().toString();
        String userPassword = password.getText().toString();

        // Ad alanı boş mu kontrolü
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, "Enter Name!", Toast.LENGTH_SHORT).show();
            // Alternatif olarak: name.setError("Enter Name!"); // Alanın üzerine hata gösterme
            return; // Fonksiyondan çık, OTP gönderme
        }

        // E-posta alanı boş mu kontrolü
        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Enter Email Address!", Toast.LENGTH_SHORT).show();
            // Alternatif olarak: email.setError("Enter Email Address!");
            return; // Fonksiyondan çık
        }

        // Şifre alanı boş mu kontrolü
        if (TextUtils.isEmpty(userPassword)) {
            Toast.makeText(this, "Enter Password!", Toast.LENGTH_SHORT).show();
            // Alternatif olarak: password.setError("Enter Password!");
            return; // Fonksiyondan çık
        }

        // Şifre uzunluğu kontrolü
        if (userPassword.length() < 6) {
            Toast.makeText(this, "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
            // Alternatif olarak: password.setError("Password too short (min 6 chars)!");
            return; // Fonksiyondan çık
        }
        // --- KONTROLLER SONU ---


        // Her 10 saniyede bir kod gönderme kontrolü
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastOtpSentTime < 10000) { // 10 saniye (10000 ms) kontrolü
            long remainingTime = 10 - ((currentTime - lastOtpSentTime) / 1000);
            Toast.makeText(this, "Please wait " + remainingTime + " seconds before requesting a new OTP!", Toast.LENGTH_SHORT).show();
            return; // Fonksiyondan çık
        }


        Toast.makeText(this, "Generating OTP...", Toast.LENGTH_SHORT).show(); // Kullanıcıya bilgi ver

        OtpCode otpCodeGenerator = new OtpCode();
        generatedOtp = otpCodeGenerator.invoke(userEmail); // OTP oluşturulur
        lastOtpSentTime = currentTime; // Son OTP gönderim zamanını güncelle

        // BURADA GERÇEK E-POSTA GÖNDERİMİNİ BAŞLATMALISINIZ
        new MailAPI(userEmail, "Your OTP Code", "Your verification code is: ", generatedOtp).execute();

        Toast.makeText(this, "OTP code has been generated (simulating sending).", Toast.LENGTH_LONG).show();

        String otpSubject = "Your E-commerce App OTP Code";
        String otpMessage = "Your verification code is: ";
        new MailAPI(userEmail, otpSubject, otpMessage, generatedOtp).execute();

        Toast.makeText(this, "OTP code sent to your email!", Toast.LENGTH_SHORT).show(); // Bu mesaj MailAPI başarılı olunca gösterilmeli idealde.


    }

    public void signup(View view) {
        String userName = name.getText().toString();
        String userEmail = email.getText().toString();
        String userPassword = password.getText().toString();
        String enteredOtp = otpCode.getText().toString(); // Kullanıcının girdiği OTP

        // --- Bu kontroller zaten signup içinde vardı, doğru yerde duruyorlar ---
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
        // --- Signup kontrolleri sonu ---

        // Kullanıcının OTP kodunu girip girmediğini kontrol et
        if (TextUtils.isEmpty(enteredOtp)) {
            Toast.makeText(this, "Enter OTP Code!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Üretilmiş OTP yoksa (örneğin sendOtpCode hiç çağrılmadıysa veya hata aldıysa)
        if (TextUtils.isEmpty(generatedOtp)) {
            Toast.makeText(this, "Please request an OTP code first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kullanıcının girdiği OTP'nin doğru olup olmadığını kontrol et
        if (!enteredOtp.equals(generatedOtp)) {
            Toast.makeText(this, "Invalid OTP Code!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tüm kontroller başarılıysa Firebase'e kaydı başlat
        auth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegistrationActivity.this, "Successfully Registered", Toast.LENGTH_SHORT).show();

                            // Başarılı kayıt sonrası bilgilendirme e-postası gönder
                            String subject = "Successful Registration / Kayıt Başarılı";
                            String messageBody = "Your registration for the E-commerce app has been successfully completed. Welcome! / E-commerce uygulamasına kaydınız başarıyla olmuştur. Hoş geldiniz!";
                            new MailAPI(userEmail, subject, messageBody, "-1").execute(); // OTP kodu olmadan (-1 ile)

                            // Kayıt sonrası LoginActivity'e yönlendir ve bu aktiviteyi kapat
                            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                            finish();

                        } else {
                            // Firebase'den gelen hata mesajını göster
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration Failed";
                            Toast.makeText(RegistrationActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void signin(View view) {
        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
        // finish(); // Genellikle kayıt ekranına geri dönülebilmesi için kapatılmaz
    }
}

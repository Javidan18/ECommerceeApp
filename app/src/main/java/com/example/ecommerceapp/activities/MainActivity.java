package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.content.SharedPreferences; // Eklendi
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.fragments.HomeFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    Fragment homeFragment;
    FirebaseAuth auth;
    Toolbar toolbar;

    // --- SharedPreferences Anahtarları ---
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboardingCompleted";
    // --- ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        toolbar = findViewById(R.id.home_toolbar);
        setSupportActionBar(toolbar);
        // Menü ikonunu tekrar aktif hale getirdim (isteğe bağlı)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24); // Menü ikonu için

        homeFragment = new HomeFragment();
        loadFragment(homeFragment);
    }

    private void loadFragment(Fragment homeFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.home_container, homeFragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Menü seçeneklerini `if-else if` yapısıyla kontrol etmek daha standarttır
        if (id == R.id.menu_logout) {
            auth.signOut();

            // Onboarding durumunu sıfırla
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_ONBOARDING_COMPLETED, false);
            editor.apply();

            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;

        } else if (id == R.id.menu_my_cart) {
            startActivity(new Intent(MainActivity.this, CartActivity.class));
            return true;

        } else if (id == R.id.menu_my_profile) { // YENİ EKLENEN KISIM
            startActivity(new Intent(MainActivity.this, ProfileActivity.class)); // ProfileActivity'e git
            return true; // İşlem tamamlandı

        }



        // Eşleşen başka bir durum yoksa varsayılan işlemi yap
        return super.onOptionsItemSelected(item);
    }


}
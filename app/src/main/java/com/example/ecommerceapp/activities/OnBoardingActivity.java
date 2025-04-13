package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.content.SharedPreferences; // Eklendi
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.SliderAdapter;
import com.google.firebase.auth.FirebaseAuth; // Eğer Firebase Auth kullanıyorsan ekle

public class OnBoardingActivity extends AppCompatActivity {


    ViewPager viewPager;
    LinearLayout dotsLayout;
    Button btn, btnNext;
    SliderAdapter sliderAdapter;
    Animation animation;
    TextView[] dots;

    // SharedPreferences anahtarı (key)
    private static final String PREFS_NAME = "AppPrefs"; // Tercih dosyasının adı
    private static final String KEY_ONBOARDING_COMPLETED = "onboardingCompleted"; // Onboarding tamamlandı mı?
    // Opsiyonel: Giriş durumunu kontrol etmek için (Firebase Auth kullanmıyorsan)
    // private static final String KEY_USER_LOGGED_IN = "userLoggedIn";

    // Eğer Firebase Auth kullanıyorsan
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- YENİ KOD: Onboarding kontrolü ---
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isOnboardingCompleted = sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);

        // Eğer Firebase Auth kullanıyorsan başlat
        mAuth = FirebaseAuth.getInstance();

        if (isOnboardingCompleted) {
            // Onboarding daha önce tamamlanmış, şimdi giriş durumunu kontrol et
            // --- GİRİŞ KONTROLÜ ---
            // Yöntem 1: Firebase Auth ile (Önerilen)
            if (mAuth.getCurrentUser() != null) {
                // Kullanıcı giriş yapmış -> HomeActivity'e git
                startActivity(new Intent(OnBoardingActivity.this, MainActivity.class)); // HomeActivity ismini kendi aktivitenle değiştir
            } else {
                // Kullanıcı giriş yapmamış -> RegistrationActivity'e git (veya LoginActivity)
                startActivity(new Intent(OnBoardingActivity.this, LoginActivity.class));
            }

            /*
            // Yöntem 2: Başka bir SharedPreferences değeri ile (Firebase Auth yoksa)
            boolean isUserLoggedIn = sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false);
            if (isUserLoggedIn) {
                 // Kullanıcı giriş yapmış -> HomeActivity'e git
                startActivity(new Intent(OnBoardingActivity.this, HomeActivity.class)); // HomeActivity ismini kendi aktivitenle değiştir
            } else {
                // Kullanıcı giriş yapmamış -> RegistrationActivity'e git (veya LoginActivity)
                startActivity(new Intent(OnBoardingActivity.this, RegistrationActivity.class));
            }
            */
            // --- GİRİŞ KONTROLÜ SONU ---

            finish(); // OnBoardingActivity'i kapat ve geri dönülmesini engelle
            return; // onCreate'in geri kalanının çalışmasını engelle
        }
        // --- YENİ KOD SONU ---


        // Onboarding daha önce tamamlanmadıysa, normal akışa devam et:
        //hide status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_on_boarding);

        viewPager=findViewById(R.id.slider);
        btnNext=findViewById(R.id.next_btn);
        dotsLayout=findViewById(R.id.dots);
        btn=findViewById(R.id.get_started_btn);

        addDots(0);

        viewPager.addOnPageChangeListener(changeListener);

        //call adapter
        sliderAdapter=new SliderAdapter(this);
        viewPager.setAdapter(sliderAdapter);

        // Ortak bir Listener oluşturup ikisine de atayabiliriz
        View.OnClickListener registrationClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // --- YENİ KOD: Onboarding'i tamamlandı olarak işaretle ---
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_ONBOARDING_COMPLETED, true);
                editor.apply(); // Kaydet (asenkron)
                // --- YENİ KOD SONU ---

                startActivity(new Intent(OnBoardingActivity.this, RegistrationActivity.class));
                finish();
            }
        };

        btn.setOnClickListener(registrationClickListener);
        btnNext.setOnClickListener(registrationClickListener); // Next butonu da aynı işi yapıyor
    }

    private void addDots(int position){
        dots = new TextView[3]; // Slider sayfan kadar olmalı
        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView( this);
            dots[i].setText(Html.fromHtml("•"));
            dots[i].setTextSize(35);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            // Aktif noktayı renklendir
            dots[position].setTextColor(getResources().getColor(R.color.pink)); // R.color.pink olduğundan emin ol
        }
    }

    ViewPager.OnPageChangeListener changeListener=new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

        @Override
        public void onPageSelected(int position) {
            addDots(position);

            // Son sayfada "Get Started" butonunu göster, diğerlerinde gizle/Next'i göster
            if (position == dots.length - 1) { // Son sayfa indexi (0'dan başladığı için length - 1)
                animation = AnimationUtils.loadAnimation(OnBoardingActivity.this, R.anim.slide_animation);
                btn.setAnimation(animation);
                btn.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.INVISIBLE); // Son sayfada Next'i gizle
            } else {
                btn.setVisibility(View.INVISIBLE);
                btnNext.setVisibility(View.VISIBLE); // Diğer sayfalarda Next'i göster
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) { }
    };

    // HomeActivity sınıfının var olduğundan emin olmalısın. Yoksa oluştur.
    // Örnek bir HomeActivity:
     /*
     public class HomeActivity extends AppCompatActivity {
         @Override
         protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_home); // activity_home.xml layout dosyan olmalı
             // ... Home ekranı kodların ...
         }
     }
     */

}
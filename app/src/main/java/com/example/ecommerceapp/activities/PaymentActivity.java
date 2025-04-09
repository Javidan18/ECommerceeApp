package com.example.ecommerceapp.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.ecommerceapp.MailCode.MailAPI;
import com.example.ecommerceapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class PaymentActivity extends AppCompatActivity {

    Toolbar toolbar;
    TextView subTotal, discount, shipping, total;
    Button paymentBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Toolbar
        toolbar = findViewById(R.id.payment_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        double amount = getIntent().getDoubleExtra("amount", 0.0);


        subTotal = findViewById(R.id.sub_total);
        discount = findViewById(R.id.textView17);
        shipping = findViewById(R.id.textView18);
        total = findViewById(R.id.total_amt);
        paymentBtn = findViewById(R.id.pay_btn);

        subTotal.setText(amount + "$");

        paymentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sipariş onayı e-posta gönder
                sendOrderConfirmationEmail();

                // Ödeme sayfasını aç
                paymentMethod();
            }
        });
    }

    private void paymentMethod() {
        String paymentUrl = "https://buy.stripe.com/test_8wM8zS0qC91n8fK9AA";

        // Ödeme sayfasını aç
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(paymentUrl));
    }

    private void sendOrderConfirmationEmail() {
        // Firebase Authentication ile oturum açan kullanıcının e-posta adresini al
        String emailAddress = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        if (emailAddress == null) {
            // Kullanıcı e-posta adresi ile giriş yapmamışsa bir hata mesajı verebilirsiniz
            return;
        }

        String subject = "Order Received";
        String message = "Your order has been successfully received. Your payment has been processed. Order details:\n\n" +
                "Total Amount: " + subTotal.getText().toString() + "\n\n" +
                "-----------------------------\n\n" +
                "Siparişiniz başarıyla alındı. Ödemeniz alınmıştır. Sipariş detaylarınız:\n\n" +
                "Toplam Tutar: " + subTotal.getText().toString();


        // MailAPI ile e-posta gönder
        new MailAPI(emailAddress, subject, message, "-1").execute();
    }
}

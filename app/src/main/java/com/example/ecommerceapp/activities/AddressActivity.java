package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Loglama için eklendi
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.AddressAdapter;
import com.example.ecommerceapp.models.AddressModel;
// Bu importlar muhtemelen artık gereksiz (obj kullanılmayacak)
// import com.example.ecommerceapp.models.NewProductsModel;
// import com.example.ecommerceapp.models.PopularProductsModel;
// import com.example.ecommerceapp.models.ShowAllModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddressActivity extends AppCompatActivity implements AddressAdapter.SelectedAddress {

    private static final String TAG = "AddressActivity"; // Loglama için etiket

    Button addAddress;
    RecyclerView recyclerView;
    private List<AddressModel> addressModelList;
    private AddressAdapter addressAdapter;

    FirebaseFirestore firestore;
    FirebaseAuth auth;
    Toolbar toolbar;
    Button paymentBtn;
    String mAddress = "";

    // CartActivity'den gelen toplam tutarı saklamak için değişken
    private double totalAmountFromCart = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        toolbar = findViewById(R.id.address_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(view -> finish());

        // ---!!! DÜZELTME 1: CartActivity'den gelen TUTARI AL !!!---
        if (getIntent() != null && getIntent().hasExtra("amount")) {
            totalAmountFromCart = getIntent().getDoubleExtra("amount", 0.0);
            Log.d(TAG, "Received total amount from cart: " + totalAmountFromCart);
        } else {
            // Tutar gelmediyse hata yönetimi (opsiyonel ama önerilir)
            Log.e(TAG, "Amount extra not received from CartActivity!");
            Toast.makeText(this, "Error: Could not retrieve total amount.", Toast.LENGTH_SHORT).show();
            // Bu durumda belki aktiviteyi kapatmak daha iyi olabilir
            // finish();
        }
        // -------------------------------------------------------

        // Bu 'obj' satırı muhtemelen farklı bir akış içindi,
        // Sepet akışı için buna gerek yok. Şimdilik yorum satırı yapalım veya silelim.
        // Object obj = getIntent().getSerializableExtra("item");

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.address_recycler);
        paymentBtn = findViewById(R.id.payment_btn);
        addAddress = findViewById(R.id.add_address_btn);

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        addressModelList = new ArrayList<>();
        addressAdapter = new AddressAdapter(getApplicationContext(), addressModelList, this);
        recyclerView.setAdapter(addressAdapter);

        // Firestore'dan adresleri alma kısmı aynı kalabilir
        firestore.collection("CurrentUser").document(auth.getCurrentUser().getUid())
                .collection("Address").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        addressModelList.clear(); // Önce listeyi temizle (güncelleme için)
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            AddressModel addressModel = doc.toObject(AddressModel.class);
                            addressModelList.add(addressModel);
                        }
                        addressAdapter.notifyDataSetChanged(); // Adapter'ı güncelle
                    } else {
                        Log.e(TAG, "Error getting addresses: ", task.getException());
                        Toast.makeText(AddressActivity.this, "Error loading addresses.", Toast.LENGTH_SHORT).show();
                    }
                });


        paymentBtn.setOnClickListener(view -> {
            // Adres seçimi kontrolleri aynı kalabilir
            if (addressModelList.isEmpty()) {
                // Eğer adres yoksa yeni adres eklemeye yönlendirilebilir
                Toast.makeText(AddressActivity.this, "Please add an address first.", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(AddressActivity.this, AddAddressActivity.class));
                return;
            }
            if (mAddress.isEmpty()) {
                Toast.makeText(AddressActivity.this, "Please select an address.", Toast.LENGTH_SHORT).show();
                return;
            }

            // ---!!! DÜZELTME 2: PaymentActivity'e DOĞRU TUTARI GÖNDER !!!---
            double amountToSend = totalAmountFromCart; // onCreate'de aldığımız tutarı kullan
            // -------------------------------------------------------------

            // Bu kısım artık gerekli değil ve hatalıydı, kaldırıldı.
            /*
            double amount = 0.0;
            if (obj instanceof NewProductsModel) {
                amount = ((NewProductsModel) obj).getPrice();
            } else if (obj instanceof PopularProductsModel) {
                amount = ((PopularProductsModel) obj).getPrice();
            } else if (obj instanceof ShowAllModel) {
                amount = ((ShowAllModel) obj).getPrice();
            }
            */

            Log.d(TAG, "Passing amount to PaymentActivity: " + amountToSend);
            Intent intent = new Intent(AddressActivity.this, PaymentActivity.class);
            intent.putExtra("amount", amountToSend); // Doğru tutarı gönder

            // Seçilen adresi de göndermek isteyebilirsiniz (opsiyonel)
            intent.putExtra("selected_address", mAddress);

            startActivity(intent);
        });


        addAddress.setOnClickListener(view -> startActivity(new Intent(AddressActivity.this, AddAddressActivity.class)));
    }

    @Override
    public void setAddress(String address) {
        mAddress = address;
        Log.d(TAG, "Selected Address: " + mAddress); // Seçilen adresi logla
    }
}
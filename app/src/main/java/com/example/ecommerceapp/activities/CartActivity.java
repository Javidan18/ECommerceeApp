package com.example.ecommerceapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.MyCartAdapter;
import com.example.ecommerceapp.models.MyCartModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    int overAllTotalAmount;
    TextView overAllAmount;
    Toolbar toolbar;
    RecyclerView recyclerView;
    List<MyCartModel> cartModelList;
    MyCartAdapter cartAdapter;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    Button buyNow, deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.my_cart_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(view -> finish());

        // Alışveriş sepeti adapter için veri al
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver, new IntentFilter("MyTotalAmount"));

        buyNow = findViewById(R.id.buy_now);
        deleteButton = findViewById(R.id.delete);
        overAllAmount = findViewById(R.id.textView3);
        recyclerView = findViewById(R.id.cart_rec);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartModelList = new ArrayList<>();
        cartAdapter = new MyCartAdapter(this, cartModelList);
        recyclerView.setAdapter(cartAdapter);

        // Firestore'dan sepet verilerini al
        fetchCartItems();

        buyNow.setOnClickListener(v -> {
            if (cartModelList.isEmpty()) {
                Toast.makeText(CartActivity.this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            } else {
                double totalPrice = 0.0;
                for (MyCartModel cartItem : cartModelList) {
                    totalPrice += cartItem.getTotalPrice();
                }
                Intent intent = new Intent(CartActivity.this, AddressActivity.class);
                intent.putExtra("amount", totalPrice);
                startActivity(intent);
            }
        });

        // DELETE Butonuna Tıklanınca Sepeti Temizle
        deleteButton.setOnClickListener(v -> clearCart());
    }

    private void fetchCartItems() {
        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartModelList.clear();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            MyCartModel myCartModel = doc.toObject(MyCartModel.class);
                            cartModelList.add(myCartModel);
                        }
                        cartAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void clearCart() {
        if (cartModelList.isEmpty()) {
            Toast.makeText(CartActivity.this, "Your cart is already empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            document.getReference().delete(); // Firestore'dan Sil
                        }
                        cartModelList.clear(); // RecyclerView'dan Sil
                        cartAdapter.notifyDataSetChanged();
                        overAllAmount.setText("Total Amount : 0$"); // Toplam Tutarı Sıfırla
                        Toast.makeText(CartActivity.this, "Your cart has been cleared", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int totalBill = intent.getIntExtra("totalAmount", 0);
            overAllAmount.setText("Total Amount : " + totalBill + "$");
        }
    };
}

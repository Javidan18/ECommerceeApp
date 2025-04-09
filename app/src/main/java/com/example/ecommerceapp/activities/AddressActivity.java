package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.ecommerceapp.models.NewProductsModel;
import com.example.ecommerceapp.models.PopularProductsModel;
import com.example.ecommerceapp.models.ShowAllModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddressActivity extends AppCompatActivity implements AddressAdapter.SelectedAddress {

    Button addAddress;
    RecyclerView recyclerView;
    private List<AddressModel> addressModelList;
    private AddressAdapter addressAdapter;

    FirebaseFirestore firestore;
    FirebaseAuth auth;
    Toolbar toolbar;
    Button paymentBtn;
    String mAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        toolbar = findViewById(R.id.address_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(view -> finish());

        Object obj = getIntent().getSerializableExtra("item");

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.address_recycler);
        paymentBtn = findViewById(R.id.payment_btn);
        addAddress = findViewById(R.id.add_address_btn);

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        addressModelList = new ArrayList<>();
        addressAdapter = new AddressAdapter(getApplicationContext(), addressModelList, this);
        recyclerView.setAdapter(addressAdapter);

        firestore.collection("CurrentUser").document(auth.getCurrentUser().getUid())
                .collection("Address").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            AddressModel addressModel = doc.toObject(AddressModel.class);
                            addressModelList.add(addressModel);
                            addressAdapter.notifyDataSetChanged();
                        }
                    }
                });


        paymentBtn.setOnClickListener(view -> {
            if (addressModelList.isEmpty()) {
                Toast.makeText(AddressActivity.this, "No saved address found.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mAddress.isEmpty()) {
                Toast.makeText(AddressActivity.this, "Please select an address.", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = 0.0;
            if (obj instanceof NewProductsModel) {
                amount = ((NewProductsModel) obj).getPrice();
            } else if (obj instanceof PopularProductsModel) {
                amount = ((PopularProductsModel) obj).getPrice();
            } else if (obj instanceof ShowAllModel) {
                amount = ((ShowAllModel) obj).getPrice();
            }

            Intent intent = new Intent(AddressActivity.this, PaymentActivity.class);
            intent.putExtra("amount", amount);
            startActivity(intent);
        });


        addAddress.setOnClickListener(view -> startActivity(new Intent(AddressActivity.this, AddAddressActivity.class)));
    }

    @Override
    public void setAddress(String address) {
        mAddress = address;
    }
}

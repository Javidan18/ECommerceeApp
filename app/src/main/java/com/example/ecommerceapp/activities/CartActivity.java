package com.example.ecommerceapp.activities;

// Gerekli importlar
import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Loglama için
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Bu importu ekleyin (Task için)
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
// LocalBroadcastManager importları KALDIRILDI
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.MyCartAdapter;
import com.example.ecommerceapp.models.MyCartModel;
import com.google.android.gms.tasks.OnCompleteListener; // Bu importu ekleyin
import com.google.android.gms.tasks.Task; // Bu importu ekleyin
import com.google.android.gms.tasks.Tasks; // Bu importu ekleyin (clearCart için)
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot; // Bu importu ekleyin (task için)


import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Locale importunu ekleyin

public class CartActivity extends AppCompatActivity {

    // int overAllTotalAmount; // Bu değişkene gerek kalmadı, doğrudan hesaplanacak
    TextView overAllAmount;
    Toolbar toolbar;
    RecyclerView recyclerView;
    List<MyCartModel> cartModelList;
    MyCartAdapter cartAdapter;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    Button buyNow, deleteButton;

    private static final String TAG = "CartActivity"; // Loglama için etiket

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

        // --- BroadcastReceiver kaydı KALDIRILDI ---
        // LocalBroadcastManager.getInstance(this)
        //        .registerReceiver(mMessageReceiver, new IntentFilter("MyTotalAmount"));

        buyNow = findViewById(R.id.buy_now);
        deleteButton = findViewById(R.id.delete);
        overAllAmount = findViewById(R.id.textView3); // ID'nin textView3 olduğundan emin olun
        recyclerView = findViewById(R.id.cart_rec);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartModelList = new ArrayList<>();
        cartAdapter = new MyCartAdapter(this, cartModelList); // Adapter'ı burada oluştur
        recyclerView.setAdapter(cartAdapter); // Adapter'ı set et

        // --- Başlangıçta toplamı 0 olarak göster ---
        // calculateAndDisplayTotal(); // Boş liste ile çağırarak 0 olmasını sağla
        overAllAmount.setText(String.format(Locale.getDefault(), "Total Amount : %.2f$", 0.0));


        // Firestore'dan sepet verilerini al
        fetchCartItems(); // Bu metot sonunda calculateAndDisplayTotal çağıracak

        buyNow.setOnClickListener(v -> {
            if (cartModelList.isEmpty()) {
                Toast.makeText(CartActivity.this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            } else {
                // Toplamı burada tekrar hesaplamak en doğrusu
                // çünkü listeye bir şekilde müdahale edilmiş olabilir.
                double totalPrice = 0.0;
                for (MyCartModel cartItem : cartModelList) {
                    totalPrice += cartItem.getTotalPrice();
                }
                Intent intent = new Intent(CartActivity.this, AddressActivity.class);
                intent.putExtra("amount", totalPrice); // Double olarak gönderiyoruz
                startActivity(intent);
            }
        });

        // DELETE Butonuna Tıklanınca Sepeti Temizle
        deleteButton.setOnClickListener(v -> clearCart()); // Bu metot sonunda toplamı güncelleyecek
    }

    // Toplam tutarı hesaplar ve TextView'ı günceller
    private void calculateAndDisplayTotal() {
        double calculatedTotal = 0.0;
        if (cartModelList != null) {
            for (MyCartModel item : cartModelList) {
                calculatedTotal += item.getTotalPrice();
            }
        }
        // Tutarı formatlayarak TextView'a yaz
        overAllAmount.setText(String.format(Locale.getDefault(), "Total Amount : %.2f$", calculatedTotal));
        Log.d(TAG, "Calculated Total: " + calculatedTotal); // Loglama
    }


    private void fetchCartItems() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in. Cannot fetch cart items.");
            // Kullanıcı giriş yapmamışsa belki LoginActivity'e yönlendir
            // startActivity(new Intent(this, LoginActivity.class));
            // finish();
            return;
        }

        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() { // Task tipini belirttik
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            cartModelList.clear(); // Önce listeyi temizle
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                MyCartModel myCartModel = doc.toObject(MyCartModel.class);
                                if (myCartModel != null) {
                                    // Belge ID'sini modele eklemek silme/güncelleme için faydalı olabilir
                                    myCartModel.setDocumentId(doc.getId());
                                    cartModelList.add(myCartModel);
                                } else {
                                    Log.w(TAG, "Firestore document could not be mapped to MyCartModel: " + doc.getId());
                                }
                            }
                            cartAdapter.notifyDataSetChanged(); // Adapter'ı güncelle

                            // ---!!! VERİ ALINDIKTAN SONRA TOPLAMI HESAPLA VE GÖSTER !!!---
                            calculateAndDisplayTotal();
                            // -------------------------------------------------------

                        } else {
                            Log.e(TAG, "Error fetching cart items: ", task.getException());
                            Toast.makeText(CartActivity.this, "Error loading cart.", Toast.LENGTH_SHORT).show();
                            // Hata durumunda toplamı sıfırla
                            overAllAmount.setText(String.format(Locale.getDefault(), "Total Amount : %.2f$", 0.0));
                        }
                    }
                });
    }

    private void clearCart() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in. Cannot clear cart.");
            return;
        }
        if (cartModelList.isEmpty()) {
            Toast.makeText(CartActivity.this, "Your cart is already empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Silme işlemini başlatmadan önce kullanıcıya sorulabilir (opsiyonel)
        // AlertDialog.Builder builder = new AlertDialog.Builder(this); ...

        Log.d(TAG,"Attempting to clear cart for user: " + auth.getCurrentUser().getUid());
        // Doğrudan tüm alt koleksiyonu silmek daha verimli olabilir ama liste üzerinden gitmek de bir yöntem.
        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Task<Void>> deleteTasks = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "Scheduling deletion for document: " + document.getId());
                                deleteTasks.add(document.getReference().delete());
                            }

                            // Tüm silme işlemlerinin bitmesini bekle
                            Tasks.whenAllComplete(deleteTasks)
                                    .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() { // Task tipini belirttik
                                        @Override
                                        public void onComplete(@NonNull Task<List<Task<?>>> allDeleteTask) {
                                            if (allDeleteTask.isSuccessful()){
                                                Log.d(TAG, "All cart items deleted successfully from Firestore.");
                                                cartModelList.clear(); // Yerel listeyi temizle
                                                cartAdapter.notifyDataSetChanged(); // Adapter'ı güncelle
                                                // ---!!! SEPET TEMİZLENDİKTEN SONRA TOPLAMI GÜNCELLE !!!---
                                                calculateAndDisplayTotal(); // Toplam 0 olacak
                                                Toast.makeText(CartActivity.this, "Your cart has been cleared", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // En az bir silme işlemi başarısız oldu
                                                Log.e(TAG, "Error deleting some cart items from Firestore: ", allDeleteTask.getException());
                                                Toast.makeText(CartActivity.this, "Error clearing some cart items.", Toast.LENGTH_SHORT).show();
                                                // Başarısız olsa bile listeyi ve toplamı UI'da güncelleyelim
                                                // (Firestore ile UI arasında geçici tutarsızlık olabilir)
                                                cartModelList.clear();
                                                cartAdapter.notifyDataSetChanged();
                                                calculateAndDisplayTotal();
                                            }
                                        }
                                    });
                        } else {
                            Log.e(TAG, "Error getting documents to delete cart: ", task.getException());
                            Toast.makeText(CartActivity.this, "Error accessing cart to clear.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // --- BroadcastReceiver KALDIRILDI ---
    /*
    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int totalBill = intent.getIntExtra("totalAmount", 0);
            overAllAmount.setText("Total Amount : " + totalBill + "$");
        }
    };
    */

    // Aktivite tekrar görünür olduğunda sepeti yenilemek iyi bir pratik olabilir
    // (Başka bir aktivitede değişiklik yapılmışsa vb.)
    @Override
    protected void onResume() {
        super.onResume();
        // Sepeti tekrar çekerek güncel kalmasını sağla (isteğe bağlı)
        // fetchCartItems(); // Dikkat: Bu, aktivite her açıldığında tekrar yükleme yapar.
        // Sadece gerekliyse kullanın veya daha akıllı bir güncelleme mekanizması kurun.
    }
}
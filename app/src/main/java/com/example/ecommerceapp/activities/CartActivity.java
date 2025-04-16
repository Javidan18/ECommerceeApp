package com.example.ecommerceapp.activities;

// Gerekli importlar
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Canvas; // Opsiyonel onChildDraw için
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper; // EKLENDİ
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.MyCartAdapter;
import com.example.ecommerceapp.models.MyCartModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar; // EKLENDİ
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    TextView overAllAmount;
    Toolbar toolbar;
    RecyclerView recyclerView;
    List<MyCartModel> cartModelList;
    MyCartAdapter cartAdapter;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    Button buyNow, deleteButton; // deleteButton tümünü sil içindi, ismi kafa karıştırabilir

    private static final String TAG = "CartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Firebase instance'ları
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Toolbar Kurulumu
        toolbar = findViewById(R.id.my_cart_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> finish());

        // View Bağlantıları
        buyNow = findViewById(R.id.buy_now);
        deleteButton = findViewById(R.id.delete); // Tümünü Sil butonu
        overAllAmount = findViewById(R.id.textView3);
        recyclerView = findViewById(R.id.cart_rec);

        // RecyclerView Kurulumu
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartModelList = new ArrayList<>();
        cartAdapter = new MyCartAdapter(this, cartModelList);
        recyclerView.setAdapter(cartAdapter);

        // --- KAYDIRARAK SİLME İÇİN ItemTouchHelper KURULUMU ---
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        // -----------------------------------------------------

        // Başlangıçta toplamı 0 göster
        overAllAmount.setText(String.format(Locale.getDefault(), "Total Amount : %.2f$", 0.0));

        // Sepet verilerini Firestore'dan çek
        fetchCartItems();

        // Satın Al Butonu
        buyNow.setOnClickListener(v -> {
            if (cartModelList.isEmpty()) {
                Toast.makeText(CartActivity.this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            } else {
                double totalPrice = 0.0;
                for (MyCartModel cartItem : cartModelList) {
                    totalPrice += cartItem.getTotalPrice(); // Modeldeki totalPrice'ı kullan
                }
                Intent intent = new Intent(CartActivity.this, AddressActivity.class);
                intent.putExtra("amount", totalPrice);
                // İsterseniz listeyi de gönderebilirsiniz (MyCartModel Serializable olmalı)
                // intent.putExtra("itemList", (ArrayList<MyCartModel>) cartModelList);
                startActivity(intent);
            }
        });

        // Tümünü Sil Butonu (Opsiyonel)
        deleteButton.setOnClickListener(v -> clearCart());
    }

    // Toplam tutarı hesaplar ve gösterir
    private void calculateAndDisplayTotal() {
        double calculatedTotal = 0.0;
        if (cartModelList != null) {
            for (MyCartModel item : cartModelList) {
                calculatedTotal += item.getTotalPrice();
            }
        }
        overAllAmount.setText(String.format(Locale.getDefault(), "Total Amount : %.2f$", calculatedTotal));
        Log.d(TAG, "Calculated Total: " + calculatedTotal);
    }

    // Firestore'dan sepet verilerini çeker
    private void fetchCartItems() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in.");
            return;
        }
        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartModelList.clear();
                        if (task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                MyCartModel myCartModel = doc.toObject(MyCartModel.class);
                                if (myCartModel != null) {
                                    myCartModel.setDocumentId(doc.getId()); // Belge ID'sini ekle
                                    cartModelList.add(myCartModel);
                                } else {
                                    Log.w(TAG, "Firestore document could not be mapped: " + doc.getId());
                                }
                            }
                        }
                        cartAdapter.notifyDataSetChanged();
                        calculateAndDisplayTotal(); // Veri geldikten sonra toplamı hesapla
                    } else {
                        Log.e(TAG, "Error fetching cart items: ", task.getException());
                        Toast.makeText(CartActivity.this, "Error loading cart.", Toast.LENGTH_SHORT).show();
                        overAllAmount.setText(String.format(Locale.getDefault(), "Total Amount : %.2f$", 0.0));
                    }
                });
    }

    // Tüm sepeti temizler
    private void clearCart() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in.");
            return;
        }
        if (cartModelList.isEmpty()) {
            Toast.makeText(CartActivity.this, "Cart is already empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG,"Attempting to clear cart for user: " + auth.getCurrentUser().getUid());
        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Task<Void>> deleteTasks = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            deleteTasks.add(document.getReference().delete());
                        }
                        Tasks.whenAllComplete(deleteTasks)
                                .addOnCompleteListener(allDeleteTask -> {
                                    if (allDeleteTask.isSuccessful()){
                                        Log.d(TAG, "All cart items deleted from Firestore.");
                                        cartModelList.clear();
                                        cartAdapter.notifyDataSetChanged();
                                        calculateAndDisplayTotal(); // Toplamı güncelle (0 olacak)
                                        Toast.makeText(CartActivity.this, "Cart cleared", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.e(TAG, "Error deleting some items: ", allDeleteTask.getException());
                                        Toast.makeText(CartActivity.this, "Error clearing some items.", Toast.LENGTH_SHORT).show();
                                        // Hata olsa bile UI'ı temizle
                                        cartModelList.clear();
                                        cartAdapter.notifyDataSetChanged();
                                        calculateAndDisplayTotal();
                                    }
                                });
                    } else {
                        Log.e(TAG, "Error getting documents to delete: ", task.getException());
                        Toast.makeText(CartActivity.this, "Error accessing cart.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- KAYDIRARAK SİLME İÇİN ItemTouchHelper Callback ---
    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(
            0, // Sürükleme kapalı
            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // Sağa ve sola kaydırma
    ) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false; // Sürükleme yok
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            // Geçersiz pozisyon kontrolü
            if (position == RecyclerView.NO_POSITION || position >= cartModelList.size()) {
                return;
            }

            final MyCartModel deletedItem = cartModelList.get(position);
            final int deletedIndex = position;
            final String documentIdToDelete = deletedItem.getDocumentId();

            // Belge ID kontrolü
            if (documentIdToDelete == null || documentIdToDelete.isEmpty()) {
                Log.e(TAG, "Cannot delete item at position " + position + ", document ID missing!");
                cartAdapter.notifyItemChanged(position); // Görünümü yenile
                Toast.makeText(CartActivity.this, "Error: Item ID missing.", Toast.LENGTH_SHORT).show();
                return;
            }

            // UI'dan Kaldır ve Toplamı Güncelle
            cartModelList.remove(position);
            cartAdapter.notifyItemRemoved(position);
            calculateAndDisplayTotal();

            // Geri Alma Seçeneği Sun
            String itemName = deletedItem.getProductName() != null ? deletedItem.getProductName() : "Item";
            Snackbar snackbar = Snackbar.make(recyclerView, itemName + " deleted", Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.undo, view -> { // strings.xml'de <string name="undo">UNDO</string> eklekme
                cartModelList.add(deletedIndex, deletedItem);
                cartAdapter.notifyItemInserted(deletedIndex);
                calculateAndDisplayTotal();
                recyclerView.scrollToPosition(deletedIndex); // Odağı kaydır
                Log.d(TAG, "Undo swipe action for item: " + documentIdToDelete);
            });
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    if (event != DISMISS_EVENT_ACTION) {
                        // Geri alınmadıysa Firestore'dan sil
                        Log.d(TAG, "Snackbar dismissed (not undo), deleting from Firestore: " + documentIdToDelete);
                        deleteSingleItemFromFirestore(documentIdToDelete, deletedItem, deletedIndex);
                    } else {
                        Log.d(TAG, "Snackbar dismissed via UNDO for: " + documentIdToDelete);
                    }
                }
            });
            snackbar.show();
        }

        // Opsiyonel: Kaydırma Arkaplanı
        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            // İsterseniz buraya kaydırma efekti ekleyebilirsiniz
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };
    // -----------------------------------------------------


    // --- Firestore'dan Tek Öğeyi Silme Metodu ---
    private void deleteSingleItemFromFirestore(String documentId, MyCartModel deletedItemForRollback, int deletedIndexForRollback) {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in. Cannot delete.");
            Toast.makeText(this, "Login required.", Toast.LENGTH_SHORT).show();
            // Belki burada UI'ı geri almak mantıklı olabilir
            restoreItemToUI(deletedItemForRollback, deletedIndexForRollback);
            return;
        }

        Log.d(TAG, "Deleting item from Firestore: " + documentId);
        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore delete success: " + documentId);
                    // Başarılı mesajı göstermeye gerek yok, Snackbar zaten bilgi verdi.
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore delete failed: " + documentId, e);
                    Toast.makeText(CartActivity.this, "Error deleting item", Toast.LENGTH_SHORT).show();
                    // HATA: Firestore'dan silinemedi, UI'ı geri al
                    restoreItemToUI(deletedItemForRollback, deletedIndexForRollback);
                });
    }

    // Yardımcı metot: Silme başarısız olursa veya geri alınırsa UI'ı eski haline getirir
    private void restoreItemToUI(MyCartModel item, int position) {
        if (position < 0 || position > cartModelList.size() || cartModelList.contains(item)) {
            // Eğer pozisyon geçersizse veya öğe zaten listede varsa (örn. UNDO ile eklendi) işlem yapma
            Log.d(TAG, "Item restore not needed or already restored: " + (item != null ? item.getDocumentId() : "null"));
            return;
        }
        cartModelList.add(position, item);
        cartAdapter.notifyItemInserted(position);
        calculateAndDisplayTotal(); // Toplamı tekrar düzelt
        Log.d(TAG, "Restored item to UI at position " + position + ": " + item.getDocumentId());
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

}
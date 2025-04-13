package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// import androidx.activity.EdgeToEdge; // Bu import muhtemelen gereksiz
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.NewProductsModel;
import com.example.ecommerceapp.models.PopularProductsModel;
import com.example.ecommerceapp.models.ShowAllModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp; // <-- Timestamp import edildi

// import java.text.SimpleDateFormat; // Artık gerekli değil
// import java.util.Calendar;        // Artık gerekli değil
import java.util.HashMap;

public class DetailedActivity extends AppCompatActivity {

    ImageView detailedImg;
    TextView name,description,price,rating,quantity;
    Button addToCart,buyNow;
    ImageView addItems,removeItems;

    Toolbar toolbar;
    int totalQuantity=1;
    int totalPrice=0;

    //new Products
    NewProductsModel newProductsModel=null;

    //popular Products
    PopularProductsModel popularProductsModel=null;

    //Show all
    ShowAllModel showAllModel=null;

    FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed);

        toolbar=findViewById(R.id.detailed_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        firestore=FirebaseFirestore.getInstance();
        auth= FirebaseAuth.getInstance();
        final Object obj = getIntent().getSerializableExtra("detailed");


        if(obj instanceof NewProductsModel) {
            newProductsModel = (NewProductsModel) obj;
        }else if(obj instanceof PopularProductsModel) {
            popularProductsModel = (PopularProductsModel) obj;
        }else if(obj instanceof ShowAllModel) {
            showAllModel = (ShowAllModel) obj;
        }

        quantity=findViewById(R.id.quantity);
        detailedImg=findViewById(R.id.detailed_img);
        name=findViewById(R.id.detailed_name);
        rating=findViewById(R.id.rating);
        description=findViewById(R.id.detailed_desc);
        price=findViewById(R.id.detailed_price);

        addItems=findViewById(R.id.add_item);
        removeItems=findViewById(R.id.remove_item);

        addToCart=findViewById(R.id.add_to_cart);
        buyNow=findViewById(R.id.buy_now);

        // --- Ürün Bilgilerini Yükleme (Tek bir yerde yapmak daha iyi) ---
        String productName = "";
        String productRating = "";
        String productDescription = "";
        String productImageUrl = "";
        int productPrice = 0; // Fiyatı int olarak alalım

        if (newProductsModel != null) {
            productName = newProductsModel.getName();
            productRating = newProductsModel.getRating();
            productDescription = newProductsModel.getDescription();
            productImageUrl = newProductsModel.getImg_url();
            productPrice = newProductsModel.getPrice();
        } else if (popularProductsModel != null) {
            productName = popularProductsModel.getName();
            productRating = popularProductsModel.getRating();
            productDescription = popularProductsModel.getDescription();
            productImageUrl = popularProductsModel.getImg_url();
            productPrice = popularProductsModel.getPrice();
        } else if (showAllModel != null) {
            productName = showAllModel.getName();
            productRating = showAllModel.getRating();
            productDescription = showAllModel.getDescription();
            productImageUrl = showAllModel.getImg_url();
            productPrice = showAllModel.getPrice();
        }

        // UI elemanlarını set et
        Glide.with(getApplicationContext()).load(productImageUrl).into(detailedImg);
        name.setText(productName);
        rating.setText(productRating);
        description.setText(productDescription);
        price.setText(String.valueOf(productPrice) + "$"); // Para birimi ekle
        quantity.setText(String.valueOf(totalQuantity)); // Miktarı başlangıçta ayarla

        // Başlangıç totalPrice'ı hesapla
        totalPrice = productPrice * totalQuantity;

        //-------------------------------------------------------------

        //Buy Now (Bu kısım değişmedi, sadece totalPrice'ın yukarıda hesaplandığından emin olun)
        buyNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(DetailedActivity.this,AddressActivity.class);

                // ---!!! DİKKAT: Buy Now için tutar gönderme mantığı !!!---
                // AddressActivity'nin sepetten gelen tutarı beklediğini varsayarsak,
                // burası SEPETTEKİ TOPLAM TUTARI DEĞİL, sadece BU ÜRÜNÜN o anki
                // seçili miktarının tutarını göndermeli. Eğer AddressActivity
                // hem sepetten hem de buradan gelen akışı yönetiyorsa, farklı
                // intent ekstraları kullanmak daha iyi olabilir.
                // Şimdilik sadece mevcut ürünü gönderiyoruz, ama tutarı göndermiyoruz.
                // Gerekirse 'amount' ekstrası buraya EKLENMELİDİR:
                // intent.putExtra("amount", (double)totalPrice); // double'a cast et
                // ---------------------------------------------------------

                if (newProductsModel !=null){
                    intent.putExtra("item",newProductsModel);
                }
                if (popularProductsModel != null){
                    intent.putExtra("item",popularProductsModel);
                }
                if (showAllModel != null){
                    intent.putExtra("item",showAllModel);
                }
                startActivity(intent);
            }
        });


        //Add to Cart
        addToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToCart(); // Metodu çağır
            }
        });


        // --- Miktar Değiştirme (Tek bir yerde yapmak daha iyi) ---
        addItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (totalQuantity < 10) {
                    totalQuantity++;
                    quantity.setText(String.valueOf(totalQuantity));
                    updateTotalPrice(); // Toplam fiyatı güncelle
                }
            }
        });

        removeItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (totalQuantity > 1) {
                    totalQuantity--;
                    quantity.setText(String.valueOf(totalQuantity));
                    updateTotalPrice(); // Toplam fiyatı güncelle
                }
            }
        });
        //-------------------------------------------------------------
    }

    // Toplam fiyatı güncelleyen yardımcı metot
    private void updateTotalPrice() {
        int productPrice = 0;
        if (newProductsModel != null) {
            productPrice = newProductsModel.getPrice();
        } else if (popularProductsModel != null) {
            productPrice = popularProductsModel.getPrice();
        } else if (showAllModel != null) {
            productPrice = showAllModel.getPrice();
        }
        totalPrice = productPrice * totalQuantity;
        // İsterseniz fiyat TextView'ını da burada güncelleyebilirsiniz,
        // ama genellikle sadece sepete eklerkenki toplam fiyat önemlidir.
    }


    // Sepete ekleme metodu güncellendi
    private void addToCart() {

        // String saveCurrentTime,saveCurrentDate; // Artık gerekli değil

        // Calendar calForDate = Calendar.getInstance(); // Artık gerekli değil

        // SimpleDateFormat currentDate = new SimpleDateFormat("dd MM, yyyy"); // Artık gerekli değil
        // saveCurrentDate = currentDate.format(calForDate.getTime()); // Artık gerekli değil

        // SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a"); // Artık gerekli değil
        // saveCurrentTime = currentTime.format(calForDate.getTime()); // Artık gerekli değil

        final HashMap<String,Object> cartMap =new HashMap<>();

        // --- Ürün bilgilerini modelden almak daha güvenli ---
        String productName = "";
        String productPriceStr = ""; // String olarak alalım (Firestore'a yazdığımız gibi)
        int productPriceInt = 0; // int olarak da alalım (totalPrice hesaplaması için)

        if (newProductsModel != null) {
            productName = newProductsModel.getName();
            productPriceInt = newProductsModel.getPrice();
        } else if (popularProductsModel != null) {
            productName = popularProductsModel.getName();
            productPriceInt = popularProductsModel.getPrice();
        } else if (showAllModel != null) {
            productName = showAllModel.getName();
            productPriceInt = showAllModel.getPrice();
        }
        // Fiyat TextView'ından almak yerine modelden aldık
        productPriceStr = String.valueOf(productPriceInt);
        // Toplam fiyatı tekrar hesapla (miktar değişmiş olabilir)
        int currentTotalPrice = productPriceInt * totalQuantity;


        cartMap.put("productName", productName); // Modelden alınan isim
        cartMap.put("productPrice", productPriceStr); // Modelden alınan fiyat (String)
        // cartMap.put("currentTime",saveCurrentTime); // KALDIRILDI
        // cartMap.put("currentDate",saveCurrentDate); // KALDIRILDI
        cartMap.put("totalQuantity", String.valueOf(totalQuantity)); // Miktar (String)
        cartMap.put("totalPrice", currentTotalPrice); // Hesaplanan toplam fiyat (int)
        cartMap.put("lastUpdated", Timestamp.now()); // <-- ZAMAN DAMGASI EKLENDİ

        firestore.collection("AddToCart").document(auth.getCurrentUser().getUid())
                .collection("User").add(cartMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) { // Başarı kontrolü eklendi
                            Toast.makeText(DetailedActivity.this, "Added To Cart", Toast.LENGTH_SHORT).show();
                            finish(); // Başarılıysa aktiviteyi kapat
                        } else {
                            // Hata durumunda kullanıcıyı bilgilendir
                            Toast.makeText(DetailedActivity.this, "Failed to add to cart: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
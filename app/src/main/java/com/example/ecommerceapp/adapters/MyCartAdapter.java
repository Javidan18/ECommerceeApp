package com.example.ecommerceapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log; // Loglama için eklendi
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
// import com.example.ecommerceapp.models.CategoryModel; // Bu import kullanılmıyor gibi görünüyor
import com.example.ecommerceapp.models.MyCartModel;
import com.google.firebase.Timestamp; // Timestamp import edildi

import java.text.SimpleDateFormat; // Tarih formatlama için import edildi
import java.util.Date;             // Date import edildi
import java.util.List;
import java.util.Locale;           // Locale import edildi

public class MyCartAdapter extends RecyclerView.Adapter<MyCartAdapter.ViewHolder> {
    Context context;
    List<MyCartModel> list;
    // int totalAmount=0; // Bu değişkenin burada olması ve onBindViewHolder içinde güncellenmesi hatalı bir mantıktır.

    public MyCartAdapter(Context context, List<MyCartModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyCartAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.my_cart_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyCartAdapter.ViewHolder holder, int position) {

        MyCartModel currentItem = list.get(position);

        // ---!!! DÜZELTME: Timestamp'ı alıp formatla !!!---
        Timestamp timestamp = currentItem.getLastUpdated();
        if (timestamp != null) {
            Date dateObject = timestamp.toDate(); // Timestamp'ı Date objesine çevir
            // İstediğiniz formatları belirleyin
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            // TextView'lara formatlanmış tarih ve saati ata
            holder.date.setText(dateFormat.format(dateObject));
            holder.time.setText(timeFormat.format(dateObject));
        } else {
            // Eğer timestamp null ise (veri eski veya kaydedilmemişse) varsayılan bir şey göster
            holder.date.setText("N/A");
            holder.time.setText("N/A");
            Log.w("MyCartAdapter", "Timestamp is null for item: " + currentItem.getProductName());
        }
        // ---------------------------------------------------

        // Fiyatın sonuna para birimi eklemek daha doğru olabilir.
        // ProductPrice'ın sadece sayı içerdiğini varsayıyoruz. Formatlama gerekebilir.
        holder.price.setText(currentItem.getProductPrice() + "$"); // Veya TL
        holder.name.setText(currentItem.getProductName());
        // Toplam fiyatı gösterirken para birimi eklemek de iyi olabilir.
        holder.totalPrice.setText(currentItem.getTotalPrice() + "$"); // Veya TL
        holder.totalQuantity.setText(currentItem.getTotalQuantity());


        // ---!!! ÖNEMLİ UYARI: Toplam tutar hesaplaması ve broadcast burada OLMAMALI !!!---
        /*
        // Bu kısım hatalı mantık içeriyor ve kaldırılmalı.
        // Toplam tutar, tüm liste yüklendikten sonra CartActivity içinde HESAPLANMALIDIR.
        // Her bir öğe için broadcast göndermek çok verimsiz ve yanlıştır.
        totalAmount =totalAmount + list.get(position).getTotalPrice(); // YANLIŞ YERDE
        Intent intent =new Intent("MyTotalAmount");
        intent.putExtra("totalAmount",totalAmount); // YANLIŞ DEĞER GÖNDERİR

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent); // GEREKSİZ YERE ÇOK FAZLA GÖNDERİM
        */
        // --------------------------------------------------------------------------------

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView date,time,price,name,totalQuantity,totalPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            date=itemView.findViewById(R.id.current_date);
            time=itemView.findViewById(R.id.current_time);
            price=itemView.findViewById(R.id.product_price);
            name=itemView.findViewById(R.id.product_name);
            totalQuantity=itemView.findViewById(R.id.total_quantity);
            totalPrice=itemView.findViewById(R.id.total_price);
        }
    }

    // --- ÖNERİ: Toplam tutarı hesaplamak için yardımcı bir metot ---
    // Bu metot adapter içinde değil, CartActivity içinde olmalı.

    private double calculateOverallTotal(List<MyCartModel> cartList) {
        double total = 0.0;
        if (cartList != null) {
            for (MyCartModel item : cartList) {
                total += item.getTotalPrice();
            }
        }
        return total;
    }

}
package com.example.ecommerceapp.models;

import com.google.firebase.Timestamp; // Firebase Timestamp import edildi
import java.io.Serializable;        // Serializable import edildi

// Serializable arayüzünü implemente etmek, bu objeyi Intent ile göndermeyi kolaylaştırır
public class MyCartModel implements Serializable {

    // Eski currentDate ve currentTime alanları kaldırıldı,
    // çünkü Timestamp zaten hem tarih hem de saat bilgisini içerir.
    // Bu bilgileri göstermek isterseniz, Timestamp'ı Android tarafında formatlayabilirsiniz.

    private String productName;
    private String productPrice;  // Fiyat için String yerine double veya BigDecimal daha iyi olabilir
    private String totalQuantity; // Miktar için String yerine int daha iyi olabilir
    private int totalPrice;       // Bu alan int, bu iyi.
    private String documentId;    // Firestore belge ID'sini tutmak için eklendi
    private Timestamp lastUpdated; // Sepetin son güncellenme zamanını tutmak için eklendi

    // Firestore'un objeyi dönüştürebilmesi için boş constructor gerekli
    public MyCartModel() {
    }

    // Güncellenmiş parametreli constructor (currentDate ve currentTime kaldırıldı, lastUpdated eklendi)
    public MyCartModel(String productName, String productPrice, String totalQuantity, int totalPrice, Timestamp lastUpdated) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.totalQuantity = totalQuantity;
        this.totalPrice = totalPrice;
        this.lastUpdated = lastUpdated;
        // documentId genellikle Firestore'dan okunduktan sonra set edilir
    }

    // --- Getter ve Setter Metotları ---

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(String productPrice) {
        this.productPrice = productPrice;
    }

    public String getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(String totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getDocumentId() {
        return documentId;
    }

    // Bu metodu Firestore'dan veri çektikten sonra kullanın:
    // MyCartModel model = doc.toObject(MyCartModel.class);
    // model.setDocumentId(doc.getId());
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    // Bu metodu sepete ürün eklerken/güncellerken kullanın:
    // model.setLastUpdated(Timestamp.now());
    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
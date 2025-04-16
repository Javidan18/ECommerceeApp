const {onSchedule} = require("firebase-functions/v2/scheduler");

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const sgMail = require("@sendgrid/mail");

admin.initializeApp();
const db = admin.firestore();

// SendGrid API Anahtarını Ortam Değişkenlerinden Al
const SENDGRID_API_KEY = functions.config().sendgrid.key;
// API anahtarının varlığını kontrol et
if (!SENDGRID_API_KEY) {
  console.error(
      "FATAL ERROR: SendGrid API Key not configured. ",
      "Run 'firebase functions:config:set sendgrid.key=YOUR_KEY'",
  );
} else {
  sgMail.setApiKey(SENDGRID_API_KEY); // Kütüphaneye API anahtarını tanıt
}


// --- ÖNEMLİ: SendGrid'de Doğruladığın Gönderici E-posta Adresi ---
// const FROM_EMAIL = "noreply@seninuygulaman.com"; // Kendi adresini yaz
const FROM_EMAIL = "cavidanbitirme@gmail.com";
const FROM_NAME = "ECommerce App"; // Gönderici adı

// Örneğin her gün sabah 9'da çalışacak bir zamanlanmış fonksiyon
// Yeni v2 syntax'ı ile zamanlanmış fonksiyon tanımı
exports.sendAbandonedCartEmails = onSchedule({
  schedule: "0 9 * * *", // Cron zamanlaması
  timeZone: "Europe/Istanbul", // Zaman dilimi
  // Gerekirse timeout ve memory gibi diğer seçenekleri ekleyebilirsin
  // timeoutSeconds: 540,
  // memory: "1GiB",
}, async (event) => { // Dikkat: context yerine event kullanılır
  // eslint-disable-next-line no-console
  console.log("Checking for abandoned carts...");

  // 1. Terk Edilme Süresini Hesapla (Örn: 24 saat önce)
  const abandonmentThreshold = new Date();
  // 24 saat öncesini ayarla
  abandonmentThreshold.setHours(abandonmentThreshold.getHours() - 24);

  try { // <<< ANA TRY BLOĞU BAŞLANGICI >>>
    // ... (try bloğunun geri kalan içeriği aynı kalacak) ...
    // ... (cartsSnapshot sorgusu, for döngüsü, email gönderme vb.) ...

    // 2. Terk Edilmiş Olabilecek Sepetleri Sorgula
    const cartsSnapshot = await db
        .collection("carts") // Sepet koleksiyonu
        .where("status", "==", "active") // Sadece aktif sepetler
        .where("updatedAt", "<=",
            admin.firestore.Timestamp.fromDate(abandonmentThreshold),
        )
        .where("reminderSentAt", "==", null)
        .get();

    if (cartsSnapshot.empty) {
      // eslint-disable-next-line no-console
      console.log("No abandoned carts found requiring reminders.");
      return null; // Fonksiyonu sonlandır
    }

    // eslint-disable-next-line no-console
    console.log(`Found ${cartsSnapshot.size} potential abandoned carts.`);

    // 3. Her sepeti işle
    const emailPromises = [];
    for (const cartDoc of cartsSnapshot.docs) {
      const cartData = cartDoc.data();
      const cartId = cartDoc.id;
      const userId = cartData.userId;

      if (!userId || !cartData) {
        console.warn(`Cart ${cartId} is missing data. Skipping.`);
        continue; // Eksik veri varsa atla
      }

      // --- Ek Kontroller ---
      // a) Sepette hala ürün var mı?
      const itemsSnapshot = await db.collection("carts")
          .doc(cartId)
          .collection("items")
          .limit(1)
          .get();
      if (itemsSnapshot.empty) {
        console.log(`Cart ${cartId} is empty, skipping.`);
        continue; // Sepet boşsa atla
      }

      // b) Kullanıcı sipariş vermiş mi?
      const ordersSnapshot = await db.collection("orders")
          .where("userId", "==", userId)
          .where("orderDate", ">", cartData.updatedAt)
          .limit(1)
          .get();
      if (!ordersSnapshot.empty) {
        console.log(
            `User ${userId} ordered post-update. Skipping.`,
        );
        // await cartDoc.ref.update({ status: 'ordered' });
        continue; // Sipariş verilmişse atla
      }
      // --- Kontroller Sonu ---

      // 4. Kullanıcı Bilgilerini ve İznini Al
      const userRef = db.collection("users").doc(userId);
      const userDoc = await userRef.get();

      if (!userDoc.exists) {
        console.warn(
            `User doc ${userId} not found for cart ${cartId}.`,
            "Skipping.",
        );
        continue; // Kullanıcı yoksa atla
      }

      const userData = userDoc.data();
      const userEmail = userData.email; // E-posta alanı
      const wantsReminders = userData.emailConsent === true; // İzin

      if (!userEmail) {
        console.warn(`Email not found for user ${userId}. Skipping.`);
        continue; // E-posta yoksa atla
      }

      if (!wantsReminders) {
        console.log(
            `User ${userId} (${userEmail}) opted out. Skipping.`,
        );
        continue; // İzin yoksa atla
      }

      // 5. E-posta İçeriğini Oluştur
      const userName = userData.name || ""; // İsim
      const unsubscribeLink =
        `https://seninuygulaman.com/unsubscribe?email=${userEmail}`;
      const cartLink = "https://seninuygulaman.com/cart"; // Sepet linki

      const msg = {
        to: userEmail,
        from: {
          email: FROM_EMAIL,
          name: FROM_NAME,
        },
        subject: "Sepetinde ürün unuttun! 🛒",
        html: `
          <h1>Merhaba ${userName}!</h1>
          <p>Görünüşe göre sepetinde bazı ürünler bıraktın.
             Onlar seni bekliyor!</p>
          <p>Sepetindeki ürünleri tamamlamak veya gözden geçirmek için
             aşağıdaki bağlantıya tıklayabilirsin:</p>
          <a href="${cartLink}"
             style="padding: 10px 15px; background-color: #007bff;
                    color: white; text-decoration: none;
                    border-radius: 5px;"
          >Sepetime Git</a>
          <br><br>
          <p>İyi alışverişler!</p>
          <hr>
          <p style="font-size: 0.8em; color: #6c757d;">
            E-postaları istemiyorsan ayarlardan değiştir veya
            <a href="${unsubscribeLink}">abonelikten çık</a>.
          </p>
        `,
      };

      // 6. E-postayı Gönder ve Başarı Durumunda Sepeti Güncelle
      console.log(
          `Attempting send to ${userEmail} for cart ${cartId}...`,
      );
      const sendPromise = sgMail.send(msg)
          .then(() => {
            console.log(`Reminder sent successfully to ${userEmail}`);
            return cartDoc.ref.update({
              reminderSentAt: admin.firestore.FieldValue.serverTimestamp(),
              // status: "abandoned", // İsteğe bağlı
            });
          })
          .catch((error) => {
            console.error(
                `Failed sending to ${userEmail} for cart ${cartId}:`,
                error.toString(),
            );
            if (error.response) {
              console.error("SendGrid Error Body:", error.response.body);
            }
            return null; // Hata durumunda devam et
          });

      emailPromises.push(sendPromise);
    } // for döngüsü sonu

    await Promise.all(emailPromises); // Tüm işlemlerin bitmesini bekle
    // eslint-disable-next-line no-console
    console.log("Finished processing abandoned carts.");
    return null; // Başarılı bitiş

    // ... (try bloğunun geri kalan içeriği aynı kalacak) ...
  } catch (error) { // <<< ANA TRY BLOĞUNA AİT CATCH BLOĞU >>>
    // Genel hata yakalama
    console.error("Error checking/processing abandoned carts:", error);
    return null; // Hata durumunda normal bitir
  } // <<< ANA CATCH BLOĞU SONU >>>
}); // onSchedule sonu

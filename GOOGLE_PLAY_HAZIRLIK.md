# ShortBan — Google Play Console Yükleme ve Onay Rehberi

Google Play Console üzerinden **ShortBan** (`com.scrollblock.app`) uygulamasını yayınlarken sorulacak tüm formlar, açıklamalar, gizlilik politikası bağlantıları ve görsel dosyaları aşağıda kopyala-yapıştır yapmaya hazır şekilde hazırlanmıştır.

---

## 1. Hazır Görsel Dosyaları (`playstore/` Klasörü)

Google Play Console **Ana Mağaza Girişi (Main Store Listing)** ekranında istenecek zorunlu görseller projenin içinde hazırdır:
- **Uygulama İkonu (512 × 512 px PNG):** `playstore/icon-512x512.png`
- **Özellik Grafiği / Feature Graphic (1024 × 500 px PNG):** `playstore/feature-graphic-1024x500.png`

---

## 2. Gizlilik Politikası (Privacy Policy) URL'si

Google Play Console **Uygulama İçeriği (App Content) → Gizlilik Politikası (Privacy Policy)** alanına şu adresi yapıştırın:

```text
https://vsolken.com/privacy
```
*(Alternatif adres: `https://vsolken.com/shortban/privacy.html` — Her iki adres de Türkçe + İngilizce Google Play Erişilebilirlik Beyanı içeren resmi gizlilik politikası sayfasını açar.)*

---

## 3. Mağaza Girişi Metinleri (Store Listing)

### Uygulama Adı (En fazla 30 karakter):
```text
ShortBan: Kısa Video Engelleyici
```

### Kısa Açıklama (En fazla 80 karakter):
```text
Reels, Shorts ve TikTok kısa videolarını ve toksik yorumları akıllıca engelleyin
```

### Tam Açıklama (En fazla 4000 karakter):
```text
ShortBan, sosyal medya uygulamalarını tamamen silmek zorunda kalmadan sonsuz kısa video kaydırma döngüsünden (doomscrolling) kurtulmanız için tasarlanmış akıllı bir odaklanma ve dijital refah kalkanıdır.

Uygulamadan atmadan, yalnızca dikkatinizi dağıtan kısa video akışlarını ve isteğe bağlı olarak yorum pencerelerini engeller:

• INSTAGRAM REELS & KEŞFET KORUMASI:
Alt bardaki Reels sekmesine veya Keşfet'teki videolara dokunduğunuzda uygulamayı kapatmaz; sizi anında güvenli Ana Sayfa akışına döndürür. Mesajlaşma (DM), hikayeler ve normal gönderiler kesintisiz çalışır.

• TIKTOK AKIŞ KORUMASI:
"Sizin İçin" (For You) sonsuz video akışını engeller ve sizi doğrudan Gelen Kutusu (Mesajlar) sekmesine yönlendirir. Arkadaşlarınızla mesajlaşmaya devam edebilirsiniz.

• YOUTUBE SHORTS KORUMASI (YOUTUBE MUSIC MUAFİYETLİ):
Yalnızca dikey YouTube Shorts oynatıcısını kapatır. Normal yatay/uzun YouTube videolarını, aboneliklerinizi ve YouTube Music uygulamasını hiçbir kısıtlama olmadan kullanabilirsiniz.

• TOKSİK YORUM KALKANI (YORUMLARI ENGELLE):
Instagram, TikTok ve YouTube için ayrı ayrı açılabilen Yorum Kalkanı sayesinde videoların veya gönderilerin altındaki yorum pencereleri açıldığı anda otomatik olarak kapatılır.

• DM TEK VİDEO İZNİ:
Arkadaşınızın DM üzerinden gönderdiği ilk Reels veya TikTok videosunu izlemenize izin verir; ancak parmağınızla 2. videoya kaydırdığınız salisede oynatıcıyı kapatıp sizi sohbete döndürür.

• GEÇİCİ MOLA (COOLDOWN) VE EBEVEYN PIN KİLİDİ:
Günlük mola süresi (5, 10, 15, 30 dk) ve günlük mola hakkı sınırı belirleyebilir, tüm ayarları 4 haneli PIN şifresiyle koruma altına alabilirsiniz.

🔒 ERİŞİLEBİLİRLİK SERVİSİ (ACCESSIBILITY SERVICE API) VE %100 GİZLİLİK BEYANI:
ShortBan, yalnızca kullanıcının seçtiği uygulamalarda (Instagram, TikTok, YouTube) kısa video ekranlarının veya yorum pencerelerinin açıldığını tespit edip kullanıcıyı güvenli sekmeye yönlendirmek amacıyla Android AccessibilityService API'sini kullanır.
- Hiçbir kişisel veri, mesaj, şifre veya ekran içeriği kaydedilmez, toplanmaz ve cihaz dışına aktarılmaz.
- Tüm tespit %100 cihaz içinde (on-device) anlık olarak gerçekleşir.
- Reklam, takip kodu (tracker) veya analitik SDK içermez.
```

---

## 4. Google Play Console — Erişilebilirlik API Beyanı (Accessibility Service Declaration)

Google Play Console'da **Uygulama İçeriği (App Content) → Erişilebilirlik (Accessibility API)** bölümünde *"Uygulamanız neden AccessibilityService API kullanıyor?"* sorusuna aşağıdaki metni kopyalayıp yapıştırın (hem Türkçe hem İngilizce inceleme ekibi için):

```text
ShortBan uses the Android AccessibilityService API solely to help users manage digital wellbeing and screen time by detecting when short-form video players (Instagram Reels, TikTok For You feed, YouTube Shorts) or comment sheets are opened on screen, and automatically navigating back to a safe tab according to user preferences.

ShortBan does NOT collect, store, or transmit any personal or sensitive user data, screen text, messages, or credentials off the device. All UI view ID checks happen 100% locally in volatile memory on the device. Before enabling the Accessibility Service, the app displays a clear Prominent Disclosure dialog explaining this functionality to the user.
```

---

## 5. Google Play Console — Veri Güvenliği (Data Safety) Formu Yanıtları

Google Play Console **Veri Güvenliği (Data Safety)** formunda:
1. **"Uygulamanız gerekli kullanıcı veri türlerinden herhangi birini topluyor veya paylaşıyor mu?"**
   → **Hayır (No)** seçeneğini işaretleyin.
2. Böylece diğer tüm alt veri sorularını doldurmanıza gerek kalmadan mağaza sayfasında **"Veri toplanmıyor / Üçüncü taraflarla paylaşılan veri yok"** rozeti otomatik olarak görünecektir.

---

## 6. Google Play İçin `.AAB` (Android App Bundle) ÜretmeNotu

- Şu anki APK içinde **GitHub üzerinden uygulama içi otomatik güncelleme sistemi (`UpdateManager.kt`)** aktiftir (`v4.1` / `versionCode = 33`).
- Google Play Console'a yükleme yapacağınız gün:
  1. Google Play politikası gereği mağazaya yüklenecek `.aab` paketinde harici APK indirme (`UpdateManager`) tek satırlık bayrakla kapatılır (`gradlew.bat :app:bundleRelease` komutu çalıştırılır).
  2. Oluşan `app/build/outputs/bundle/release/app-release.aab` dosyası doğrudan Google Play Console'a yüklenir!


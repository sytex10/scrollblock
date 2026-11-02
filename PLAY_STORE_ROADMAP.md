# ScrollBlock — Google Play Store Uyumluluk Yol Haritası (Notlar)

> **Mevcut Kararlı Yedek Noktası:** `v2.9-stable` (`versionCode = 20`, commit `e483645`)
> - **Git Tag:** `v2.9-stable`
> - **Git Yedek Dalı (Branch):** `backup/v2.9-stable`
> - **Yerel Arşiv Yedeği:** `c:\Users\Emin\.gemini\antigravity-ide\scratch\scrollblock_backup_v2.9.zip`

---

## 1. İzin Politikaları (Kritik Red Sebepleri)
Google Play Store, kendi kendini güncelleyen APK yükleyicilere ve buna bağlı izinlere kesinlikle izin vermez:
- [ ] **`REQUEST_INSTALL_PACKAGES` İzninin Kaldırılması:** `AndroidManifest.xml` içinden bu izin kaldırılmalı (veya `github` / `playstore` olarak iki ayrı Build Flavor yapılarak Play Store sürümünden tamamen çıkarılmalı).
- [ ] **Uygulama İçi APK İndirme / Yükleme (`UpdateManager`) Kaldırılması:** Play Store politikası gereği Google Play'den indirilen bir uygulama dışarıdan (GitHub vb.) `.apk` indirip kuramaz. Play Store sürümünde güncelleme kontrolü ya tamamen kaldırılmalı ya da Google Play In-App Updates API'ye bağlanmalı.
- [ ] **`INTERNET` İzni Kontrolü:** Eğer dışarıdan güncelleme kontrolü kaldırılırsa ve uygulama tamamen çevrimdışı çalışıyorsa `INTERNET` izni bile kaldırılabilir. Bu sayede Google Play'in **"Veri Güvenliği (Data Safety)"** bölümünde *"Hiçbir veri toplanmaz ve üçüncü taraflarla paylaşılmaz"* beyanı %100 kanıtlanmış olur.

---

## 2. AccessibilityService (Erişilebilirlik Servisi) Politikası (En Önemli Adım)
Google Play, `AccessibilityService` kullanan uygulamaları çok sıkı inceler. Onay almak için gerekenler:
- [ ] **Açık Beyan Penceresi (Prominent Disclosure Dialog):**
  - Kullanıcıyı Android Erişilebilirlik Ayarları'na göndermeden **hemen önce**, ekranda tam ve net bir onay penceresi gösterilmeli:
    1. Uygulamanın **Erişilebilirlik Servisi API'sini (`AccessibilityService`)** neden kullandığı (Instagram Reels, TikTok ve YouTube Shorts kısa video ekranlarını tespit edip odaklanmayı korumak için),
    2. Hangi verilere baktığı (yalnızca ekrandaki pencere/sekme yapısı),
    3. Hiçbir kişisel veriyi, mesajı veya şifreyi **kaydetmediği, toplamadığı ve hiçbir sunucuya göndermediği**,
    4. Açıkça **"Kabul Ediyorum / İzin Ver"** ve **"Reddet / Vazgeç"** butonları.
- [ ] **Play Console Beyan Videosu (YouTube Unlisted Video):**
  - Play Console'da uygulamayı gönderirken Google, uygulamanın içinde bu Prominent Disclosure penceresinin nasıl göründüğünü ve Erişilebilirlik izninin nasıl açıldığını gösteren 30-60 saniyelik bir ekran kaydı videosu linki ister.
- [ ] **`accessibility_service_config.xml` Açıklaması:**
  - `accessibility_service_description` metni Google Play politikasına uygun şekilde; veri toplamadığını ve yalnızca kısa video akışlarını engellemek için kullanıldığını açıkça belirtmeli.

---

## 3. Paket Adı (`applicationId`), App Bundle (`.aab`) ve R8 Küçültme
- [ ] **`applicationId` Kontrolü:** Şu an `com.scrollblock.app` kullanılıyor. Play Store'da benzersiz olduğundan emin olunmalı.
- [ ] **Android App Bundle (`.aab`) Üretimi:** Play Store artık `.apk` değil, `./gradlew bundleRelease` ile üretilen `.aab` dosyasını kabul ediyor.
- [ ] **R8 / ProGuard Aktifleştirme (`isMinifyEnabled = true`, `isShrinkResources = true`):** Uygulama boyutunu küçültmek ve Play Store'un sembol dosyası (`mapping.txt`) beklentisini karşılamak için Release derlemesinde R8 açılmalı.

---

## 4. Gizlilik Politikası (Privacy Policy) ve Mağaza Sayfası
- [ ] **Gizlilik Politikası Sayfası ve Uygulama İçi Bağlantı:**
  - GitHub Pages üzerinden ücretsiz barındırılan Türkçe/İngilizce bir Gizlilik Politikası URL'si hazırlanmalı ve uygulamanın Ayarlar/Sistem kartına *"Gizlilik Politikası"* tıklanabilir bağlantısı eklenmeli.
- [ ] **Ticari Marka (Trademark) Kuralları:**
  - Play Store mağaza başlığında ve açıklamasında *"Instagram için..."* yerine *"ScrollBlock: Kısa Video & Reels Engelleyici (Instagram, TikTok ve YouTube Shorts ile uyumlu)"* formatı kullanılmalı; uygulama ikonunda başka markaların logoları yer almamalı.


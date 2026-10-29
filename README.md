# ScrollBlock — Instant Short-Form Video Blocker for Android

ScrollBlock, Android cihazlarda kısa video akışlarını (**YouTube Shorts**, **Instagram Reels**, **TikTok**) anında ve sessizce engelleyerek sonsuz kaydırma bağımlılığını sonlandıran yerel bir Android uygulamasıdır.

## 🚀 Özellikler

* **Anında ve Sessiz Müdahale:** Hiçbir uyarı penceresi veya onay olmadan kısa video açıldığı milisaniyede otomatik `GERİ (BACK)` veya akıllı yönlendirmeyi tetikler.
* **Akıllı Koruma:**
  * **YouTube:** Yalnızca tam ekran Shorts oynatıcısını engeller; normal videolar, arama ve kitaplık kesintisiz çalışır.
  * **Instagram:** Reels sekmesini Ana Sayfa'ya yönlendirir; ana sayfa akışı, hikayeler, direkt mesajlar (DM) ve profiller serbesttir.
  * **TikTok:** Ana video akışını doğrudan Gelen Kutusu'na yönlendirir; gelen kutusunda kaydırma, profil yönetimi ve sohbetler serbesttir.
* **⏱️ Ayarlanabilir Mola / Cooldown Modu:** 5, 10, 15 veya 30 dakikalık geçici mola hakkı; isteğe bağlı günlük kullanım limiti ve ana ekranda canlı geri sayım sayacı. Süre bitince servis otomatik olarak engellemeye devam eder.
* **🔒 Ebeveyn / Güvenlik PIN Kilidi:** 4 haneli PIN şifresi ile engelleme ayarlarını, platform anahtarlarını ve mola başlatmayı yetkisiz değişikliklere karşı korur (PBKDF2 tuzlu hash ve brute-force koruması).
* **📩 DM'den Gelen Reels İzni (Tek Video):** Arkadaşlarınızın DM'den gönderdiği ilk Reel'ı izlemenize izin verir; ancak bir sonraki videoya kaydırmaya çalıştığınız an algılayıp hemen kapatır.
* **%100 Gizlilik:** İnternet üzerinden hiçbir ekran görüntüsü veya kişisel veri aktarılmaz.
* **Uygulama İçi Doğrudan Güncelleme:** GitHub üzerinden tek tıkla doğrudan son sürüme güncelleme desteği.

## 📥 İndir (APK)

En güncel derlenmiş imzalı APK dosyası:
* [ScrollBlock-release.apk](release/ScrollBlock-release.apk)
* Doğrudan İndirme Linki: `https://github.com/sytex10/scrollblock/raw/main/release/ScrollBlock-release.apk`

## 🛠️ Kurulum ve İzinler

1. APK dosyasını kurun.
2. Uygulamayı açıp **"1. Adım: Kısıtlı Ayar Kilidini Aç"** butonuna basarak sağ üstteki 3 noktadan (⋮) *"Kısıtlı ayarlara izin ver"* seçeneğini onaylayın (Android 13+).
3. **"2. Adım: Erişilebilirlik Ayarlarına Git"** butonuna basıp ScrollBlock servisini etkinleştirin.

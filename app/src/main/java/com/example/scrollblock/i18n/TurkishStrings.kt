package com.example.scrollblock.i18n

object TurkishStrings : AppStrings {
    // Top Bar & General Status
    override val brandShort = "SHORT"
    override val brandBan = "BAN"
    override val versionTag = "v1.0"
    override val statusActive = "Aktif"
    override val statusPaused = "Durduruldu"
    override val statusBreakMode = "Mola"
    override val statusServiceOff = "Servis Kapalı"
    override val statusNeedPermission = "İzin Gerekli"

    // Permission Setup Banner
    override val permBannerTitle = "Erişilebilirlik Servisi Kapalı"
    override val permBannerDesc = "Android 13+ Kısıtlı Ayarlar kilidini açmak için aşağıdaki sırayı izleyin."
    override val permBannerRefresh = "Yenile"
    override val permBannerStepGuide = "1. Önce '1. Erişilebilirlik' butonuna basıp listeden ShortBan'a bir kez dokunun ('Kısıtlı Ayar' uyarısı çıkmalı; aksi halde üç nokta görünmez).\n" +
            "2. Ardından '2. Uygulama Bilgisi' ekranında sağ üstteki (⋮) menüsünden 'Kısıtlı ayarlara izin ver' deyin.\n" +
            "3. Tekrar Erişilebilirlik menüsüne dönüp ShortBan anahtarını açın.\n\n" +
            "Not: Uygulamayı Google Play üzerinden indirdiğinizde bu uyarı çıkmaz. APK ile kurulum yapıyorsanız Play Store > Profil > Play Protect > Ayarlar üzerinden taramayı geçici olarak kapatıp kurulumu tamamlayın."
    override val permBannerBtnStep1 = "1. Erişilebilirlik"
    override val permBannerBtnStep2 = "2. Uygulama Bilgisi (⋮)"

    // Stats Overview Card
    override val statsTitle = "Toplam Engellenen Akış"
    override val statsResetButton = "Sıfırla"
    override val statsNoBlocksYet = "Henüz hiçbir kısa video akışı engellenmedi"
    override val statsLastBlockTemplate = "Son engelleme: %s · %s"

    // Reset Stats Dialog
    override val resetDialogTitle = "İstatistikleri Sıfırla"
    override val resetDialogMessage = "Toplam engellenen akış sayacını sıfırlamak istediğinize emin misiniz?"
    override val resetDialogCancel = "İptal"
    override val resetDialogConfirm = "Sıfırla"

    // Section Headers
    override val sectionHeaderDashboard = "GÖSTERGE PANELİ"
    override val sectionHeaderBreak = "MOLA VE DİNLENME"
    override val sectionHeaderPlatforms = "KORUMA PLATFORMLARI"
    override val sectionHeaderSecurity = "GÜVENLİK VE SİSTEM"

    // Break / Cooldown Card
    override val breakTitle = "Geçici Mola Modu"
    override val breakDesc = "Belirli bir süre için engellemeyi geçici olarak duraklatın."
    override val breakLimitUnlimited = "Sınırsız"
    override val breakLimitUsedTemplate = "%d/%d kullanıldı"
    override val breakDurationLabel = "Mola Süresi Seçin"
    override val breakDailyLimitLabel = "Günlük Mola Hakkı Sınırı"
    override val breakLimit1x = "1 kez"
    override val breakLimit2x = "2 kez"
    override val breakLimit3x = "3 kez"
    override val breakStartButtonTemplate = "%d Dakika Mola Başlat"
    override val breakLimitReached = "Bugünkü Mola Hakkınız Doldu"
    override val breakRemainingHeader = "MOLA AKTİF — KALAN SÜRE"
    override val breakActiveDesc = "Engelleme geçici olarak durduruldu. Süre dolunca otomatik olarak tekrar devreye girecektir."
    override val breakEndAndReturn = "Molayı Bitir ve Korumaya Dön"

    // Platforms Card
    override val platformInstagramTitle = "Instagram"
    override val platformInstagramDesc = "Reels sekmesini Ana Sayfa'ya yönlendirir (uygulamadan atmaz)"
    override val platformInstagramDmTitle = "DM'den gelen ilk Reels'a izin ver"
    override val platformInstagramDmDesc = "Yalnızca sohbetten açılan ilk videoya izin verir; sonraki videoya kaydırdığınız an kapatır."
    override val platformInstagramCommentsTitle = "Yorumları engelle (Yorum Kalkanı)"
    override val platformInstagramCommentsDesc = "Gönderi ve Reels yorumlar penceresi açıldığında otomatik olarak kapatır."

    override val platformTikTokTitle = "TikTok"
    override val platformTikTokDesc = "Sizin İçin akışını Gelen Kutusu'na yönlendirir (uygulamadan atmaz)"
    override val platformTikTokDmTitle = "DM'den gelen ilk videoya izin ver"
    override val platformTikTokDmDesc = "Sohbetten açılan ilk video izlenir; sonraki videoya kaydırınca sohbete döner."
    override val platformTikTokCommentsTitle = "Yorumları engelle (Yorum Kalkanı)"
    override val platformTikTokCommentsDesc = "Video yorumları penceresi açıldığında otomatik olarak kapatır."

    override val platformYouTubeTitle = "YouTube Shorts"
    override val platformYouTubeDesc = "Shorts oynatıcısını kapatır, normal uzun videolar çalışır"
    override val platformYouTubeCommentsTitle = "Yorumları engelle (Yorum Kalkanı)"
    override val platformYouTubeCommentsDesc = "Uzun videolarda ve Shorts'ta yorumlar paneli açıldığında otomatik kapatır."

    // Security & System Card
    override val securityPinTitle = "Ebeveyn / Güvenlik PIN Kilidi"
    override val securityPinDescOn = "Koruma anahtarları ve mola 4 haneli PIN ile korunuyor."
    override val securityPinDescOff = "Ayarları ve molayı 4 haneli şifreyle koruma altına alın."
    override val securityChangePinBtn = "Değiştir"

    override val securityAccessibilityTitle = "Erişilebilirlik Servis Ayarları"
    override val securityAccessibilityDescRunning = "Servis arka planda çalışıyor · Ayarları aç"
    override val securityAccessibilityDescStopped = "Servis kapalı · İzin vermek için dokunun"
    override val securityOpenBtn = "Aç"

    override val securityGuideTitle = "Kullanım ve Kurulum Rehberi"
    override val securityGuideDesc = "Uygulamanın nasıl çalıştığını ve izin adımlarını görüntüleyin"
    override val securityGuideBtn = "Rehber"

    override val securityPrivacyTitle = "Gizlilik Politikası ve Erişilebilirlik Beyanı"
    override val securityPrivacyDesc = "Sıfır veri toplama taahhüdü ve izin kullanım detayları"
    override val securityReadBtn = "Oku"

    override val securityLanguageTitle = "Uygulama Dili"
    override val securityLanguageDesc = "Türkçe ve İngilizce arasında geçiş yapın"

    override val securityVersionTitle = "Sürüm"
    override val securityVersionSub = "ShortBan Mobil"

    // PIN Dialogs & Prompts
    override val pinVerifyDefaultTitle = "PIN Doğrulaması"
    override val pinVerifyDefaultSub = "Devam etmek için 4 haneli PIN kodunuzu girin."
    override val pinFieldPlaceholder = "4 haneli PIN"
    override val pinConfirmFieldPlaceholder = "PIN'i tekrar girin"
    override val pinDialogCancelBtn = "İptal"
    override val pinDialogConfirmBtn = "Onayla"

    override val pinSetupNewHeader = "Yeni PIN Belirleyin"
    override val pinSetupSubHeader = "Ayarları ve molayı korumak için 4 haneli bir şifre girin."
    override val pinSetupMismatch = "PIN kodları eşleşmiyor."
    override val pinSetupMustBe4 = "PIN 4 haneli olmalıdır."
    override val pinSetupSaveBtn = "Kaydet"

    // Specific PIN Protection Subtitles
    override val dialogPinResetStatsSub = "Sayacı sıfırlamak için PIN girin."
    override val dialogPinDurationTitle = "Mola Süresi"
    override val dialogPinDurationSub = "Mola süresini değiştirmek için PIN girin."
    override val dialogPinDailyLimitTitle = "Günlük Limit"
    override val dialogPinDailyLimitSub = "Günlük mola limitini değiştirmek için PIN girin."
    override val dialogPinStartBreakTitle = "Mola Başlat"
    override val dialogPinStartBreakSub = "Geçici molayı başlatmak için PIN girin."
    override val dialogPinRemoveTitle = "PIN Kilidini Kaldır"
    override val dialogPinRemoveSub = "Kaldırmak için mevcut PIN kodunuzu girin."
    override val dialogPinChangeTitle = "PIN Değiştir"
    override val dialogPinChangeSub = "Önce mevcut PIN kodunuzu doğrulayın."
    override val dialogPinIgDisableSub = "Instagram engelini kapatmak için PIN girin."
    override val dialogPinIgDmSub = "Instagram DM video iznini açmak için PIN girin."
    override val dialogPinIgCommentsSub = "Instagram yorum kalkanını kapatmak için PIN girin."
    override val dialogPinTtDisableSub = "TikTok engelini kapatmak için PIN girin."
    override val dialogPinTtDmSub = "TikTok DM video iznini açmak için PIN girin."
    override val dialogPinTtCommentsSub = "TikTok yorum kalkanını kapatmak için PIN girin."
    override val dialogPinYtDisableSub = "YouTube Shorts engelini kapatmak için PIN girin."
    override val dialogPinYtCommentsSub = "YouTube yorum kalkanını kapatmak için PIN girin."

    // Toasts
    override val toastMolaBitirildi = "Mola bitirildi. Koruma devrede."
    override val toastMolaHakkiDoldu = "Bugünkü mola hakkınız doldu."
    override val toastPinRemoved = "PIN kilidi kaldırıldı."
    override val toastPinSaved = "PIN kaydedildi."
    override val toastPinLength = "PIN 4 rakamdan oluşmalıdır."
    override val toastWrongPinLeftTemplate = "Hatalı PIN (%d deneme kaldı)"
    override val toastLockedOutTemplate = "Çok fazla hatalı deneme. %d sn bekleyin."

    // Privacy & Disclosure Dialog
    override val privacyDialogTitle = "Açık Rıza ve Gizlilik Politikası Beyanı"
    override val privacyDialogIntro = "ShortBan, yalnızca seçtiğiniz platformlardaki (Instagram Reels, TikTok, YouTube Shorts) kısa video akışlarını ve yorum pencerelerini tespit edip sizi güvenli sekmeye yönlendirmek için çalışır."
    override val privacySection1 = "1. Erişilebilirlik İzni Açık Rıza Beyanı:\nUygulama yalnızca hedef platformlarda (Instagram, TikTok, YouTube) kısa video ekranlarının veya yorum pencerelerinin açıldığını tespit etmek için Android AccessibilityService API'sini kullanarak arayüz kimliklerini anlık kontrol eder. Özel mesajlar (DM), klavye, banka uygulamaları ve diğer uygulamalara dokunulmaz."
    override val privacySection2 = "2. Sıfır Veri Toplama ve Aktarım Taahhüdü:\nHiçbir kişisel veriniz, mesajınız, şifreniz, ekran görüntünüz veya gezinme geçmişiniz kaydedilmez, toplanmaz, uzak sunuculara gönderilmez ve üçüncü taraflarla paylaşılmaz. Tüm işlemler %100 cihazınızın içinde gerçekleşir."
    override val privacySection3 = "3. Cihaz İçi Yerel Tercihler:\nKoruma anahtarları, mola süreleri, engellenen akış sayacı ve isteğe bağlı 4 haneli PIN kilidi yalnızca cihazınızın kendi yerel hafızasında saklanır."
    override val privacySection4 = "4. Reklam ve Analitik Yoktur:\nShortBan içerisinde hiçbir reklam SDK'sı, kullanıcı takip aracı (tracker) veya üçüncü taraf analitik servisi bulunmamaktadır."
    override val privacyDialogDismiss = "Vazgeç"
    override val privacyDialogAcknowledge = "Anladım ve Kapat"
    override val privacyDialogAcceptSettings = "Kabul Et ve Ayarları Aç"

    // Onboarding Screen General
    override val onboardingBadgeGuide = "REHBER"
    override val onboardingBtnSkip = "Rehberi Geç"
    override val onboardingBtnClose = "Kapat"
    override val onboardingBtnBack = "Geri"
    override val onboardingBtnAcceptContinue = "Kabul Et ve Devam Et"
    override val onboardingBtnNext = "Sonraki Adım"
    override val onboardingBtnFinish = "Kurulumu Tamamla ve Başla"
    override val onboardingServiceActive = "Erişilebilirlik Servisi Şu An AÇIK (Hazır)"
    override val onboardingServiceInactive = "Erişilebilirlik Servisi Şu An KAPALI (İzin Bekleniyor)"

    // Onboarding Step 1
    override val onboardingStep1LabelTemplate = "ADIM 01 / %s · NASIL ÇALIŞIR?"
    override val onboardingStep1Title = "Kısa Videoları ve Yorumları Akıllıca Engelle"
    override val onboardingStep1Subtitle = "ShortBan uygulamaları tamamen kapatmaz; yalnızca sonsuz kısa video akışlarını ve toksik yorumları yakalayıp sizi güvenli sekmeye yönlendirir:"
    override val onboardingStep1IgTitle = "Instagram Reels -> Ana Sayfa"
    override val onboardingStep1IgBody = "Alt bardaki Reels sekmesini gizler, tıklansa bile uygulamadan atmaz; sizi doğrudan Ana Sayfa akışına döndürür. Hikayeler, gönderiler, profil ve DM mesajlaşması tamamen serbesttir."
    override val onboardingStep1TtTitle = "TikTok -> Gelen Kutusu (Mesajlar)"
    override val onboardingStep1TtBody = "Sizin İçin (Ana Sayfa) video akışını engeller ve sizi otomatik olarak Gelen Kutusu sekmesine geçirir. Arkadaşlarınızla mesajlaşabilir ve profilinizi yönetebilirsiniz."
    override val onboardingStep1YtTitle = "YouTube Shorts -> Normal Video"
    override val onboardingStep1YtBody = "Shorts girişini gizler ve tam ekran dikey Shorts oynatıcısını kapatır. Normal uzun YouTube videolarını ve aboneliklerinizi kesintisiz kullanabilirsiniz."
    override val onboardingStep1Footer = "Art arda hızlıca (spam) tıklasanız bile anti-spam koruması devreye girer ve kısa video akışının açılmasına izin vermez."

    // Onboarding Step 2
    override val onboardingStep2LabelTemplate = "ADIM 02 / %s · ÖZELLİKLER"
    override val onboardingStep2Title = "Arayüz Koruması, Yorum Kalkanı, Mola ve PIN"
    override val onboardingStep2Subtitle = "Günlük kullanımınızı kısıtlamadan iradenizi korumanız için tasarlanan özel araçlar:"
    override val onboardingStep2HideTag = "GİZLE"
    override val onboardingStep2HideTitle = "Kısa Video Girişlerini Gizle (Yeni)"
    override val onboardingStep2HideBody = "Instagram Reels, YouTube Shorts ve TikTok kısa video giriş butonlarını uygulamanın o anki Koyu/Açık tema rengiyle maskeler ve yanlışlıkla dokunmanızı engeller."
    override val onboardingStep2DmTag = "DM"
    override val onboardingStep2DmTitle = "DM'den Gelen İlk Videoya İzin Ver"
    override val onboardingStep2DmBody = "Arkadaşınızın Instagram veya TikTok DM'den gönderdiği ilk videoyu izleyebilirsiniz. Ancak parmağınızla 2. videoya kaydırdığınız salisede oynatıcı kapanır ve sohbete dönersiniz."
    override val onboardingStep2PinTag = "PIN"
    override val onboardingStep2PinTitle = "Ayarlanabilir Mola & 4 Haneli PIN Kilidi"
    override val onboardingStep2PinBody = "İhtiyacınız olduğunda geçici mola başlatabilir, kendinizi veya çocuğunuzu korumak için ayarları 4 haneli şifre ile kilitleyebilirsiniz."
    override val onboardingStep2Footer = "Tüm bu kuralları ana ekrandan tek dokunuşla dilediğiniz gibi açıp kapatabilirsiniz."

    // Onboarding Step 3 (Privacy)
    override val onboardingStep3LabelTemplate = "ADIM 03 / %s · AÇIK RIZA VE GİZLİLİK"
    override val onboardingStep3Title = "Açık Rıza ve Gizlilik Politikası Beyanı"
    override val onboardingStep3Subtitle = "ShortBan'ı kullanmaya başlamadan önce Erişilebilirlik izni ve gizlilik ilkelerimiz:"
    override val onboardingStep3Item1Title = "Erişilebilirlik İzni Açık Rıza Beyanı"
    override val onboardingStep3Item1Body = "Uygulama yalnızca hedef platformlarda (Instagram, TikTok, YouTube) kısa video ekranlarının veya yorum pencerelerinin açıldığını tespit etmek için Android AccessibilityService API'sini kullanarak arayüz kimliklerini anlık kontrol eder. Özel mesajlar (DM), klavye, banka uygulamaları ve diğer uygulamalara dokunulmaz."
    override val onboardingStep3Item2Title = "Sıfır Veri Toplama ve Aktarım Taahhüdü"
    override val onboardingStep3Item2Body = "Hiçbir kişisel veriniz, mesajınız, şifreniz, ekran görüntünüz veya gezinme geçmişiniz kaydedilmez, toplanmaz, uzak sunuculara gönderilmez ve üçüncü taraflarla paylaşılmaz. Tüm işlemler %100 cihazınızın içinde gerçekleşir."
    override val onboardingStep3Item3Title = "Cihaz İçi Yerel Tercihler & Sıfır Reklam"
    override val onboardingStep3Item3Body = "Koruma anahtarları, mola süreleri, engellenen akış sayacı ve 4 haneli PIN kilidi yalnızca cihazınızın kendi yerel hafızasında saklanır. Hiçbir reklam SDK'sı veya takip aracı (tracker) bulunmaz."
    override val onboardingStep3Footer = "Sonraki adıma geçerek ve Erişilebilirlik Servisi'ni etkinleştirerek bu Açık Rıza ve Gizlilik Politikası Beyanı'nı okuduğunuzu ve kabul ettiğinizi onaylamış olursunuz."

    // Onboarding Step 4 (Android 12)
    override val onboardingStep4A12Label = "ADIM 04 / 04 · HIZLI KURULUM"
    override val onboardingStep4A12Title = "Erişilebilirlik Servisini Etkinleştirin"
    override val onboardingStep4A12Subtitle = "Cihazınızda (Android 12 ve altı) kısıtlı ayar kilidi bulunmadığı için doğrudan tek adımla korumayı başlatabilirsiniz:"
    override val onboardingStep4A12Button = "Erişilebilirlik Ayarlarını Aç"
    override val onboardingStep4A12Footer = "Servisi açtıktan sonra aşağıdaki 'Kurulumu Tamamla ve Başla' butonuna basarak ana ekrana geçebilirsiniz."

    // Onboarding Step 4 (Android 13+)
    override val onboardingStep4A13Label = "ADIM 04 / 05 · KURULUM (1. AŞAMA)"
    override val onboardingStep4A13Title = "Android 13+ Kısıtlı Ayar Kilidini Açma"
    override val onboardingStep4A13Subtitle = "Uygulama Bilgisi ekranında sağ üstteki üç nokta (⋮) menüsünün görünmesi için şu sırayı eksiksiz uygulayın:"
    override val onboardingStep4A13Item1Title = "Önce Erişilebilirlikte ShortBan'a Dokunun (Zorunlu)"
    override val onboardingStep4A13Item1Body = "Aşağıdaki '1. Adım: Erişilebilirlikte ShortBan'a Dokun' butonuna basın, listeden ShortBan'ı seçip anahtara bir kez dokunun. Ekrana 'Kısıtlı Ayar' uyarısı çıkınca Tamam deyin. (Bu yapılmazsa üç nokta menüsü çıkmaz!)"
    override val onboardingStep4A13Item2Title = "Uygulama Bilgisi > Sağ Üst Üç Nokta (⋮)"
    override val onboardingStep4A13Item2Body = "Ardından aşağıdaki '2. Adım: Uygulama Bilgisi (⋮) Aç' butonuna basın ve sağ üst köşedeki üç nokta (⋮) simgesine dokunun."
    override val onboardingStep4A13Item3Title = "'Kısıtlı Ayarlara İzin Ver' Seçeneğini Onaylayın"
    override val onboardingStep4A13Item3Body = "Açılan menüdeki 'Kısıtlı ayarlara izin ver' seçeneğine dokunun ve telefonunuzun ekran kilidiyle onaylayın."
    override val onboardingStep4A13BtnStep1 = "1. Adım: Erişilebilirlikte ShortBan'a Dokun (Tetikle)"
    override val onboardingStep4A13BtnStep2 = "2. Adım: Uygulama Bilgisi (⋮) > Kısıtlı Ayarlara İzin Ver"
    override val onboardingStep4A13Footer = "Önemli İpucu: Sağ üstte üç nokta (⋮) görünmüyorsa 1. adımdaki Erişilebilirlik menüsünde ShortBan'a henüz dokunmamışsınız demektir.\n\nNot: Uygulamayı Google Play üzerinden indirdiğinizde bu uyarı çıkmaz. APK ile kurulum yapıyorsanız Play Store > Profil > Play Protect > Ayarlar üzerinden taramayı geçici olarak kapatıp kurulumu tamamlayın."

    // Onboarding Step 5 (Android 13+)
    override val onboardingStep5A13Label = "ADIM 05 / 05 · KURULUM (2. AŞAMA)"
    override val onboardingStep5A13Title = "Erişilebilirlik Servisini Etkinleştirin"
    override val onboardingStep5A13Subtitle = "Kısıtlı ayar kilidini açtığınıza göre artık ShortBan korumasını aktif edebilirsiniz:"
    override val onboardingStep5A13Item1Title = "Erişilebilirlik Ayarlarını Tekrar Açın"
    override val onboardingStep5A13Item1Body = "Aşağıdaki '3. Adım: Erişilebilirlik Ayarlarını Aç ve Aktif Et' butonuna dokunun."
    override val onboardingStep5A13Item2Title = "ShortBan Anahtarını Açık Konuma Getirin"
    override val onboardingStep5A13Item2Body = "'İndirilen Uygulamalar' (veya Yüklü Hizmetler) menüsünden ShortBan'a girin ve anahtarı açın."
    override val onboardingStep5A13Item3Title = "Tam Gizlilik ve Sıfır Veri Toplama"
    override val onboardingStep5A13Item3Body = "ShortBan hiçbir kişisel verinizi, mesajınızı veya ekran görüntünüzü kaydetmez ve internete göndermez. Tüm tespit cihaz içinde anlık yapılır."
    override val onboardingStep5A13BtnStep3 = "3. Adım: Erişilebilirlik Ayarlarını Aç ve Aktif Et"
    override val onboardingStep5A13Footer = "Servisi açtıktan sonra aşağıdaki 'Kurulumu Tamamla ve Başla' butonuna basarak ana ekrana geçebilirsiniz."

    // Onboarding Privacy Modal
    override val onboardingModalTitle = "Açık Rıza ve Gizlilik Politikası Beyanı"
    override val onboardingModalDesc = "ShortBan, yalnızca seçtiğiniz platformlardaki (Instagram Reels, TikTok, YouTube Shorts) kısa video akışlarını ve yorum pencerelerini tespit edip sizi güvenli sekmeye yönlendirmek için Android AccessibilityService API'sini kullanır."
    override val onboardingModalBullet1 = "• Sıfır Veri Toplama: Hiçbir kişisel veriniz, mesajınız, şifreniz veya ekran görüntünüz kaydedilmez ve internete gönderilmez."
    override val onboardingModalBullet2 = "• Yalnızca Cihaz İçi İşlem: Tüm tespit anlık olarak telefonunuzun içinde gerçekleşir; özel mesajlara (DM), klavyeye ve diğer uygulamalara dokunulmaz."
    override val onboardingModalCancel = "Vazgeç"
    override val onboardingModalAccept = "Kabul Et ve Ayarları Aç"

    // Quick Settings Tile
    override val tileLabel = "ShortBan"
    override val tileStateActive = "Koruma Açık"
    override val tileStatePaused = "Durduruldu"
    override val tileStateBreak = "Mola Aktif"
    override val tileStateServiceOff = "Servis Kapalı"
}

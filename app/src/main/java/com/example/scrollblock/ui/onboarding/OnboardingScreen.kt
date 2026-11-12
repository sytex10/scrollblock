package com.example.scrollblock.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.scrollblock.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollblock.android12.core.Android12PermissionGuide
import com.example.scrollblock.theme.AccentPrimary
import com.example.scrollblock.theme.AccentSignal
import com.example.scrollblock.theme.AccentSignalSoft
import com.example.scrollblock.theme.ActiveGreen
import com.example.scrollblock.theme.ActiveGreenContainer
import com.example.scrollblock.theme.ActiveGreenText
import com.example.scrollblock.theme.CardBackground
import com.example.scrollblock.theme.CardBackgroundElevated
import com.example.scrollblock.theme.CardBorder
import com.example.scrollblock.theme.HairlineDivider
import com.example.scrollblock.theme.NearBlack
import com.example.scrollblock.theme.TextMuted
import com.example.scrollblock.theme.TextPrimary
import com.example.scrollblock.theme.TextSecondary
import com.example.scrollblock.util.AccessibilityUtils

data class OnboardingHighlightItem(
    val tag: String,
    val title: String,
    val body: String
)

data class OnboardingPageData(
    val stepLabel: String,
    val title: String,
    val subtitle: String,
    val items: List<OnboardingHighlightItem>,
    val footerNote: String,
    val actionButtonText: String? = null,
    val actionType: OnboardingActionType = OnboardingActionType.NONE
)

enum class OnboardingActionType {
    NONE,
    OPEN_APP_DETAILS,
    OPEN_ACCESSIBILITY,
    ANDROID13_RESTRICTED_FLOW,
    ANDROID12_DIRECT_FLOW
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    isServiceActive: Boolean = false,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAndroid12OrBelow = remember { Android12PermissionGuide.isAndroid12OrBelow() }
    val totalStepsLabel = if (isAndroid12OrBelow) "04" else "05"

    val pages = remember(isAndroid12OrBelow) {
        val basePages = listOf(
            OnboardingPageData(
                stepLabel = "ADIM 01 / $totalStepsLabel · NASIL ÇALIŞIR?",
                title = "Kısa Videoları ve Yorumları Akıllıca Engelle",
                subtitle = "ShortBan uygulamaları tamamen kapatmaz; yalnızca sonsuz kısa video akışlarını ve toksik yorumları yakalayıp sizi güvenli sekmeye yönlendirir:",
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "IG",
                        title = "Instagram Reels -> Ana Sayfa",
                        body = "Alt bardaki Reels sekmesini gizler, tıklansa bile uygulamadan atmaz; sizi doğrudan Ana Sayfa akışına döndürür. Hikayeler, gönderiler, profil ve DM mesajlaşması tamamen serbesttir."
                    ),
                    OnboardingHighlightItem(
                        tag = "TT",
                        title = "TikTok -> Gelen Kutusu (Mesajlar)",
                        body = "Sizin İçin (Ana Sayfa) video akışını engeller ve sizi otomatik olarak Gelen Kutusu sekmesine geçirir. Arkadaşlarınızla mesajlaşabilir ve profilinizi yönetebilirsiniz."
                    ),
                    OnboardingHighlightItem(
                        tag = "YT",
                        title = "YouTube Shorts -> Normal Video",
                        body = "Shorts girişini gizler ve tam ekran dikey Shorts oynatıcısını kapatır. Normal uzun YouTube videolarını ve aboneliklerinizi kesintisiz kullanabilirsiniz."
                    )
                ),
                footerNote = "Art arda hızlıca (spam) tıklasanız bile anti-spam koruması devreye girer ve kısa video akışının açılmasına izin vermez."
            ),
            OnboardingPageData(
                stepLabel = "ADIM 02 / $totalStepsLabel · ÖZELLİKLER",
                title = "Arayüz Koruması, Yorum Kalkanı, Mola ve PIN",
                subtitle = "Günlük kullanımınızı kısıtlamadan iradenizi korumanız için tasarlanan özel araçlar:",
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "GİZLE",
                        title = "Kısa Video Girişlerini Gizle (Yeni)",
                        body = "Instagram Reels, YouTube Shorts ve TikTok kısa video giriş butonlarını uygulamanın o anki Koyu/Açık tema rengiyle maskeler ve yanlışlıkla dokunmanızı engeller."
                    ),
                    OnboardingHighlightItem(
                        tag = "DM",
                        title = "DM'den Gelen İlk Videoya İzin Ver",
                        body = "Arkadaşınızın Instagram veya TikTok DM'den gönderdiği ilk videoyu izleyebilirsiniz. Ancak parmağınızla 2. videoya kaydırdığınız salisede oynatıcı kapanır ve sohbete dönersiniz."
                    ),
                    OnboardingHighlightItem(
                        tag = "PIN",
                        title = "Ayarlanabilir Mola & 4 Haneli PIN Kilidi",
                        body = "İhtiyacınız olduğunda geçici mola başlatabilir, kendinizi veya çocuğunuzu korumak için ayarları 4 haneli şifre ile kilitleyebilirsiniz."
                    )
                ),
                footerNote = "Tüm bu kuralları ana ekrandan tek dokunuşla dilediğiniz gibi açıp kapatabilirsiniz."
            ),
            OnboardingPageData(
                stepLabel = "ADIM 03 / $totalStepsLabel · AÇIK RIZA VE GİZLİLİK",
                title = "Açık Rıza ve Gizlilik Politikası Beyanı",
                subtitle = "ShortBan'ı kullanmaya başlamadan önce Erişilebilirlik izni ve gizlilik ilkelerimiz:",
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "1",
                        title = "Erişilebilirlik İzni Açık Rıza Beyanı",
                        body = "Uygulama yalnızca hedef platformlarda (Instagram, TikTok, YouTube) kısa video ekranlarının veya yorum pencerelerinin açıldığını tespit etmek için Android AccessibilityService API'sini kullanarak arayüz kimliklerini anlık kontrol eder. Özel mesajlar (DM), klavye, banka uygulamaları ve diğer uygulamalara dokunulmaz."
                    ),
                    OnboardingHighlightItem(
                        tag = "2",
                        title = "Sıfır Veri Toplama ve Aktarım Taahhüdü",
                        body = "Hiçbir kişisel veriniz, mesajınız, şifreniz, ekran görüntünüz veya gezinme geçmişiniz kaydedilmez, toplanmaz, uzak sunuculara gönderilmez ve üçüncü taraflarla paylaşılmaz. Tüm işlemler %100 cihazınızın içinde gerçekleşir."
                    ),
                    OnboardingHighlightItem(
                        tag = "3",
                        title = "Cihaz İçi Yerel Tercihler & Sıfır Reklam",
                        body = "Koruma anahtarları, mola süreleri, engellenen akış sayacı ve 4 haneli PIN kilidi yalnızca cihazınızın kendi yerel hafızasında saklanır. Hiçbir reklam SDK'sı veya takip aracı (tracker) bulunmaz."
                    )
                ),
                footerNote = "Sonraki adıma geçerek ve Erişilebilirlik Servisi'ni etkinleştirerek bu Açık Rıza ve Gizlilik Politikası Beyanı'nı okuduğunuzu ve kabul ettiğinizi onaylamış olursunuz."
            )
        )

        if (isAndroid12OrBelow) {
            basePages + listOf(
                OnboardingPageData(
                    stepLabel = "ADIM 04 / 04 · HIZLI KURULUM",
                    title = "Erişilebilirlik Servisini Etkinleştirin",
                    subtitle = "Cihazınızda (Android 12 ve altı) kısıtlı ayar kilidi bulunmadığı için doğrudan tek adımla korumayı başlatabilirsiniz:",
                    items = emptyList(),
                    footerNote = "Servisi açtıktan sonra aşağıdaki 'Kurulumu Tamamla ve Başla' butonuna basarak ana ekrana geçebilirsiniz.",
                    actionButtonText = "Erişilebilirlik Ayarlarını Aç",
                    actionType = OnboardingActionType.ANDROID12_DIRECT_FLOW
                )
            )
        } else {
            basePages + listOf(
                OnboardingPageData(
                    stepLabel = "ADIM 04 / 05 · KURULUM (1. AŞAMA)",
                    title = "Android 13+ Kısıtlı Ayar Kilidini Açma",
                    subtitle = "Uygulama Bilgisi ekranında sağ üstteki üç nokta (⋮) menüsünün görünmesi için şu sırayı eksiksiz uygulayın:",
                    items = listOf(
                        OnboardingHighlightItem(
                            tag = "1",
                            title = "Önce Erişilebilirlikte ShortBan'a Dokunun (Zorunlu)",
                            body = "Aşağıdaki '1. Adım: Erişilebilirlikte ShortBan'a Dokun' butonuna basın, listeden ShortBan'ı seçip anahtara bir kez dokunun. Ekrana 'Kısıtlı Ayar' uyarısı çıkınca Tamam deyin. (Bu yapılmazsa üç nokta menüsü çıkmaz!)"
                        ),
                        OnboardingHighlightItem(
                            tag = "2",
                            title = "Uygulama Bilgisi > Sağ Üst Üç Nokta (⋮)",
                            body = "Ardından aşağıdaki '2. Adım: Uygulama Bilgisi (⋮) Aç' butonuna basın ve sağ üst köşedeki üç nokta (⋮) simgesine dokunun."
                        ),
                        OnboardingHighlightItem(
                            tag = "3",
                            title = "'Kısıtlı Ayarlara İzin Ver' Seçeneğini Onaylayın",
                            body = "Açılan menüdeki 'Kısıtlı ayarlara izin ver' seçeneğine dokunun ve telefonunuzun ekran kilidiyle onaylayın."
                        )
                    ),
                    footerNote = "Önemli İpucu: Sağ üstte üç nokta (⋮) görünmüyorsa 1. adımdaki Erişilebilirlik menüsünde ShortBan'a henüz dokunmamışsınız demektir.",
                    actionButtonText = "1. Adım: Erişilebilirlikte ShortBan'a Dokun",
                    actionType = OnboardingActionType.ANDROID13_RESTRICTED_FLOW
                ),
                OnboardingPageData(
                    stepLabel = "ADIM 05 / 05 · KURULUM (2. AŞAMA)",
                    title = "Erişilebilirlik Servisini Etkinleştirin",
                    subtitle = "Kısıtlı ayar kilidini açtığınıza göre artık ShortBan korumasını aktif edebilirsiniz:",
                    items = listOf(
                        OnboardingHighlightItem(
                            tag = "1",
                            title = "Erişilebilirlik Ayarlarını Tekrar Açın",
                            body = "Aşağıdaki '3. Adım: Erişilebilirlik Ayarlarını Aç ve Aktif Et' butonuna dokunun."
                        ),
                        OnboardingHighlightItem(
                            tag = "2",
                            title = "ShortBan Anahtarını Açık Konuma Getirin",
                            body = "'İndirilen Uygulamalar' (veya Yüklü Hizmetler) menüsünden ShortBan'a girin ve anahtarı açın."
                        ),
                        OnboardingHighlightItem(
                            tag = "%100",
                            title = "Tam Gizlilik ve Sıfır Veri Toplama",
                            body = "ShortBan hiçbir kişisel verinizi, mesajınızı veya ekran görüntünüzü kaydetmez ve internete göndermez. Tüm tespit cihaz içinde anlık yapılır."
                        )
                    ),
                    footerNote = "Servisi açtıktan sonra aşağıdaki 'Kurulumu Tamamla ve Başla' butonuna basarak ana ekrana geçebilirsiniz.",
                    actionButtonText = "3. Adım: Erişilebilirlik Ayarlarını Aç ve Aktif Et",
                    actionType = OnboardingActionType.OPEN_ACCESSIBILITY
                )
            )
        }
    }

    var currentStep by remember { mutableIntStateOf(0) }
    var showOnboardingDisclosureDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = NearBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst Bar: Logo, Adım Sayacı ve Atla Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.shortban_logo),
                        contentDescription = "ShortBan Logo",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SHORT",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "BAN",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = androidx.compose.ui.graphics.Color(0xFFF21B2D),
                            letterSpacing = (-0.4).sp
                        )
                    }
                    Text(
                        text = "REHBER",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CardBackgroundElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = if (currentStep < pages.size - 1) "Rehberi Geç" else "Kapat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .clickable { onComplete() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Orta Kısım: Kaydırılabilir Adım İçeriği
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding_page"
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = page.stepLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )

                    Text(
                        text = page.title,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 31.sp,
                        letterSpacing = (-0.6).sp
                    )

                    Text(
                        text = page.subtitle,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )

                    // Son adımda canlı servis durum rozeti
                    if (pageIndex == pages.size - 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isServiceActive) ActiveGreenContainer else AccentSignalSoft)
                                .border(
                                    1.dp,
                                    if (isServiceActive) ActiveGreen.copy(alpha = 0.4f) else AccentSignal.copy(alpha = 0.4f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceActive) ActiveGreen else AccentSignal)
                            )
                            Text(
                                text = if (isServiceActive) {
                                    "Erişilebilirlik Servisi Şu An AÇIK (Hazır)"
                                } else {
                                    "Erişilebilirlik Servisi Şu An KAPALI (İzin Bekleniyor)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isServiceActive) ActiveGreenText else TextPrimary
                            )
                        }
                    }

                    // Detay Kartları
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        page.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardBackground)
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .padding(13.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardBackgroundElevated)
                                        .border(1.dp, HairlineDivider, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.tag,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPrimary
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = item.body,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // Varsa Doğrudan Sistem Ayarı Açma Bölümü
                    when (page.actionType) {
                        OnboardingActionType.ANDROID12_DIRECT_FLOW -> {
                            Android12PermissionGuide.OnboardingStepsCard(
                                isServiceEnabled = isServiceActive,
                                onOpenAccessibility = { showOnboardingDisclosureDialog = true }
                            )
                        }
                        OnboardingActionType.ANDROID13_RESTRICTED_FLOW -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showOnboardingDisclosureDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentPrimary,
                                        contentColor = NearBlack
                                    )
                                ) {
                                    Text(
                                        text = "1. Adım: Erişilebilirlikte ShortBan'a Dokun (Tetikle)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { AccessibilityUtils.openAppDetailsSettings(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Text(
                                        text = "2. Adım: Uygulama Bilgisi (⋮) > Kısıtlı Ayarlara İzin Ver",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        OnboardingActionType.OPEN_APP_DETAILS,
                        OnboardingActionType.OPEN_ACCESSIBILITY -> {
                            if (page.actionButtonText != null) {
                                OutlinedButton(
                                    onClick = {
                                        if (page.actionType == OnboardingActionType.OPEN_APP_DETAILS) {
                                            AccessibilityUtils.openAppDetailsSettings(context)
                                        } else {
                                            showOnboardingDisclosureDialog = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Text(
                                        text = page.actionButtonText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        OnboardingActionType.NONE -> {}
                    }

                    // Alt Bilgi Kutusu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBackgroundElevated.copy(alpha = 0.6f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = page.footerNote,
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alt Kısım: Adım Çubukları + Geri / İleri Butonları
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentStep
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(4.dp)
                                .width(if (isSelected) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentPrimary else CardBorder)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(0.38f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 13.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text(
                                text = "Geri",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < pages.size - 1) {
                                currentStep++
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier.weight(if (currentStep > 0) 0.62f else 1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = NearBlack
                        ),
                        contentPadding = PaddingValues(vertical = 13.dp)
                    ) {
                        Text(
                            text = if (currentStep == 2) {
                                "Kabul Et ve Devam Et"
                            } else if (currentStep < pages.size - 1) {
                                "Sonraki Adım"
                            } else {
                                "Kurulumu Tamamla ve Başla"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (showOnboardingDisclosureDialog) {
            Dialog(onDismissRequest = { showOnboardingDisclosureDialog = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Açık Rıza ve Gizlilik Politikası Beyanı",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "ShortBan, yalnızca seçtiğiniz platformlardaki (Instagram Reels, TikTok, YouTube Shorts) kısa video akışlarını ve yorum pencerelerini tespit edip sizi güvenli sekmeye yönlendirmek için Android AccessibilityService API'sini kullanır.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBackgroundElevated)
                            .border(1.dp, HairlineDivider, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "• Sıfır Veri Toplama: Hiçbir kişisel veriniz, mesajınız, şifreniz veya ekran görüntünüz kaydedilmez ve internete gönderilmez.",
                            fontSize = 11.5.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "• Yalnızca Cihaz İçi İşlem: Tüm tespit anlık olarak telefonunuzun içinde gerçekleşir; özel mesajlara (DM), klavyeye ve diğer uygulamalara dokunulmaz.",
                            fontSize = 11.5.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showOnboardingDisclosureDialog = false }) {
                            Text("Vazgeç", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                showOnboardingDisclosureDialog = false
                                AccessibilityUtils.openAccessibilitySettings(context)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPrimary,
                                contentColor = NearBlack
                            )
                        ) {
                            Text("Kabul Et ve Ayarları Aç", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}

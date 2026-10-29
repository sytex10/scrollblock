package com.example.scrollblock.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    OPEN_ACCESSIBILITY
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    isServiceActive: Boolean = false,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val pages = remember {
        listOf(
            OnboardingPageData(
                stepLabel = "ADIM 01 / 04 · NASIL ÇALIŞIR?",
                title = "Uygulamadan Atmadan Akıllı Yönlendirme",
                subtitle = "ScrollBlock uygulamaları tamamen kapatmaz; yalnızca sonsuz kısa video akışlarını yakalayıp sizi güvenli sekmeye yönlendirir:",
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "IG",
                        title = "Instagram Reels -> Ana Sayfa",
                        body = "Alt bardaki Reels sekmesine bastığınızda uygulamadan atmaz; sizi doğrudan Ana Sayfa akışına döndürür. Hikayeler, gönderiler, profil ve DM mesajlaşması tamamen serbesttir."
                    ),
                    OnboardingHighlightItem(
                        tag = "TT",
                        title = "TikTok -> Gelen Kutusu (Mesajlar)",
                        body = "Sizin İçin (Ana Sayfa) video akışını engeller ve sizi otomatik olarak Gelen Kutusu sekmesine geçirir. Arkadaşlarınızla mesajlaşabilir ve profilinizi yönetebilirsiniz."
                    ),
                    OnboardingHighlightItem(
                        tag = "YT",
                        title = "YouTube Shorts -> Normal Video",
                        body = "Yalnızca tam ekran dikey Shorts oynatıcısını kapatır. Normal uzun YouTube videolarını, aramayı ve aboneliklerinizi kesintisiz kullanabilirsiniz."
                    )
                ),
                footerNote = "Art arda hızlıca (spam) tıklasanız bile anti-spam koruması devreye girer ve kısa video akışının açılmasına izin vermez."
            ),
            OnboardingPageData(
                stepLabel = "ADIM 02 / 04 · ÖZELLİKLER",
                title = "DM Tek Video İzni, Mola ve PIN Koruması",
                subtitle = "Günlük kullanımınızı kısıtlamadan iradenizi korumanız için tasarlanan 3 özel araç:",
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "DM",
                        title = "DM'den Gelen İlk Videoya İzin Ver",
                        body = "Bu ayarı açtığınızda arkadaşınızın Instagram veya TikTok DM'den gönderdiği ilk videoyu izleyebilirsiniz. Ancak parmağınızla 2. videoya kaydırdığınız salisede oynatıcı kapanır ve sohbete dönersiniz."
                    ),
                    OnboardingHighlightItem(
                        tag = "MOLA",
                        title = "Ayarlanabilir Mola & Günlük Sınır",
                        body = "İhtiyacınız olduğunda 5, 10, 15 veya 30 dakikalık geçici mola başlatabilirsiniz. Günlük mola sayısını (ör. günde en fazla 1 kez) sınırlandırabilirsiniz. Süre bitince koruma kendiliğinden başlar."
                    ),
                    OnboardingHighlightItem(
                        tag = "PIN",
                        title = "4 Haneli Güvenlik / Ebeveyn PIN Kilidi",
                        body = "Kendinizi veya çocuğunuzu korumak için 4 haneli şifre belirleyin. PIN aktifken koruma kapatılamaz, mola başlatılamaz ve ayarlar değiştirilemez."
                    )
                ),
                footerNote = "Tüm bu kuralları ana ekrandan tek dokunuşla dilediğiniz gibi özelleştirebilirsiniz."
            ),
            OnboardingPageData(
                stepLabel = "ADIM 03 / 04 · KURULUM (1. AŞAMA)",
                title = "Android 13+ Kısıtlı Ayar Kilidini Açma",
                subtitle = "Android 13 ve üzeri cihazlarda APK ile kurulan uygulamalar için sistem güvenlik kilidi bulunur. Erişilebilirlik izni verebilmek için önce şu 3 adımı uygulayın:",
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "1",
                        title = "Uygulama Bilgisi Ekranını Açın",
                        body = "Aşağıdaki '1. Adım: Uygulama Bilgisi Ayarlarını Aç' butonuna dokunun."
                    ),
                    OnboardingHighlightItem(
                        tag = "2",
                        title = "Sağ Üstteki Üç Nokta (⋮) Menüsüne Dokunun",
                        body = "Açılan sistem sayfasının sağ üst köşesinde bulunan üç nokta (⋮) simgesine basın."
                    ),
                    OnboardingHighlightItem(
                        tag = "3",
                        title = "'Kısıtlı Ayarlara İzin Ver' Seçeneğini Onaylayın",
                        body = "Menüdeki 'Kısıtlı ayarlara izin ver' seçeneğine dokunun ve telefonunuzun ekran kilidiyle onaylayın."
                    )
                ),
                footerNote = "Not: Uygulama Bilgisi ekranının sağ üst köşesinde üç nokta (⋮) simgesi yoksa telefonunuzda kısıtlı ayar kilidi yoktur; doğrudan bir sonraki adıma geçebilirsiniz.",
                actionButtonText = "1. Adım: Uygulama Bilgisi Ayarlarını Aç",
                actionType = OnboardingActionType.OPEN_APP_DETAILS
            ),
            OnboardingPageData(
                stepLabel = "ADIM 04 / 04 · KURULUM (2. AŞAMA)",
                title = "Erişilebilirlik Servisini Etkinleştirin",
                subtitle = "Son olarak ScrollBlock'un kısa video akışlarını algılayabilmesi için Erişilebilirlik Servisi'ni açın:",
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "1",
                        title = "Erişilebilirlik Ayarlarını Açın",
                        body = "Aşağıdaki '2. Adım: Erişilebilirlik Ayarlarını Aç' butonuna dokunun."
                    ),
                    OnboardingHighlightItem(
                        tag = "2",
                        title = "ScrollBlock'u Bulup Açık Konuma Getirin",
                        body = "'İndirilen Uygulamalar' (veya Yüklü Servisler) menüsünden ScrollBlock'a girin ve servisi etkinleştirin."
                    ),
                    OnboardingHighlightItem(
                        tag = "%100",
                        title = "Tam Gizlilik ve Sıfır Veri Toplama",
                        body = "ScrollBlock hiçbir kişisel verinizi, mesajınızı veya ekran görüntünüzü kaydetmez ve internete göndermez. Tüm tespit cihaz içinde anlık yapılır."
                    )
                ),
                footerNote = "Servisi açtıktan sonra aşağıdaki 'Kurulumu Tamamla ve Başla' butonuna basarak ana ekrana geçebilirsiniz.",
                actionButtonText = "2. Adım: Erişilebilirlik Ayarlarını Aç",
                actionType = OnboardingActionType.OPEN_ACCESSIBILITY
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
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
                    Text(
                        text = "scrollblock",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
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
                    if (pageIndex == 3) {
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

                    // Varsa Doğrudan Sistem Ayarı Açma Butonu
                    if (page.actionButtonText != null) {
                        OutlinedButton(
                            onClick = {
                                when (page.actionType) {
                                    OnboardingActionType.OPEN_APP_DETAILS ->
                                        AccessibilityUtils.openAppDetailsSettings(context)
                                    OnboardingActionType.OPEN_ACCESSIBILITY ->
                                        AccessibilityUtils.openAccessibilitySettings(context)
                                    OnboardingActionType.NONE -> {}
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
                            text = if (currentStep < pages.size - 1) "Sonraki Adım" else "Kurulumu Tamamla ve Başla",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollblock.theme.AccentPrimary
import com.example.scrollblock.theme.CardBackground
import com.example.scrollblock.theme.CardBorder
import com.example.scrollblock.theme.NearBlack
import com.example.scrollblock.theme.TextMuted
import com.example.scrollblock.theme.TextPrimary
import com.example.scrollblock.theme.TextSecondary

data class OnboardingStep(
    val badge: String,
    val title: String,
    val description: String,
    val detail: String
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                badge = "01 / ODAK",
                title = "Sonsuz Kaydırmaya\nSon Verin",
                description = "YouTube Shorts, Instagram Reels ve TikTok bağımlılık akışlarını açıldığı an sessizce engeller veya ana ekrana yönlendirir.",
                detail = "Normal videolar, DM mesajlaşmaları, profiller ve hikayeler kesintisiz çalışmaya devam eder."
            ),
            OnboardingStep(
                badge = "02 / ESNEKLİK",
                title = "Akıllı Kontrol &\nMola Modu",
                description = "Arkadaşlarınızın DM üzerinden gönderdiği ilk videoyu rahatça izleyin; bir sonraki videoya kaydırdığınız an oynatıcı kapatılır.",
                detail = "İstediğinizde 5-30 dakikalık geçici mola alabilir, ayarlarınızı 4 haneli PIN kilidi ile koruyabilirsiniz."
            ),
            OnboardingStep(
                badge = "03 / GİZLİLİK",
                title = "Tek Gereken:\nErişilebilirlik İzni",
                description = "ScrollBlock'un kısa videoları yakalayabilmesi için Android Erişilebilirlik İzni gereklidir.",
                detail = "%100 yerel çalışır. Hiçbir ekran görüntüsü veya kişisel veri kaydedilmez veya internete gönderilmez."
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]

    Surface(
        modifier = modifier.fillMaxSize(),
        color = NearBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst Kısım: Logo ve Atla Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "scrollblock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )

                if (currentStep < steps.size - 1) {
                    Text(
                        text = "Atla",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .clickable { onComplete() }
                            .padding(8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }

            // Orta Kısım: Adım İçeriği
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { targetIndex ->
                val current = steps[targetIndex]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = current.badge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = current.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 38.sp,
                        letterSpacing = (-1).sp
                    )

                    Text(
                        text = current.description,
                        fontSize = 15.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBackground)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = current.detail,
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Alt Kısım: Gösterge ve İlerleme Butonları
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Adım Noktaları (Dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        val isSelected = index == currentStep
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentPrimary else CardBorder)
                        )
                    }
                }

                // Buton
                Button(
                    onClick = {
                        if (currentStep < steps.size - 1) {
                            currentStep++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = NearBlack
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = if (currentStep < steps.size - 1) "İlerle" else "ScrollBlock'u Başlat",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


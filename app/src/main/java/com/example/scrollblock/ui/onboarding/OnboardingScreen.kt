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
import com.example.scrollblock.i18n.LanguageManager
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
    val s = LanguageManager.LocalAppStrings.current
    val isAndroid12OrBelow = remember { Android12PermissionGuide.isAndroid12OrBelow() }
    val totalStepsLabel = if (isAndroid12OrBelow) "04" else "05"

    val pages = remember(isAndroid12OrBelow, s) {
        val basePages = listOf(
            OnboardingPageData(
                stepLabel = String.format(s.onboardingStep1LabelTemplate, totalStepsLabel),
                title = s.onboardingStep1Title,
                subtitle = s.onboardingStep1Subtitle,
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "IG",
                        title = s.onboardingStep1IgTitle,
                        body = s.onboardingStep1IgBody
                    ),
                    OnboardingHighlightItem(
                        tag = "TT",
                        title = s.onboardingStep1TtTitle,
                        body = s.onboardingStep1TtBody
                    ),
                    OnboardingHighlightItem(
                        tag = "YT",
                        title = s.onboardingStep1YtTitle,
                        body = s.onboardingStep1YtBody
                    )
                ),
                footerNote = s.onboardingStep1Footer
            ),
            OnboardingPageData(
                stepLabel = String.format(s.onboardingStep2LabelTemplate, totalStepsLabel),
                title = s.onboardingStep2Title,
                subtitle = s.onboardingStep2Subtitle,
                items = listOf(
                    OnboardingHighlightItem(
                        tag = s.onboardingStep2HideTag,
                        title = s.onboardingStep2HideTitle,
                        body = s.onboardingStep2HideBody
                    ),
                    OnboardingHighlightItem(
                        tag = s.onboardingStep2DmTag,
                        title = s.onboardingStep2DmTitle,
                        body = s.onboardingStep2DmBody
                    ),
                    OnboardingHighlightItem(
                        tag = s.onboardingStep2PinTag,
                        title = s.onboardingStep2PinTitle,
                        body = s.onboardingStep2PinBody
                    )
                ),
                footerNote = s.onboardingStep2Footer
            ),
            OnboardingPageData(
                stepLabel = String.format(s.onboardingStep3LabelTemplate, totalStepsLabel),
                title = s.onboardingStep3Title,
                subtitle = s.onboardingStep3Subtitle,
                items = listOf(
                    OnboardingHighlightItem(
                        tag = "1",
                        title = s.onboardingStep3Item1Title,
                        body = s.onboardingStep3Item1Body
                    ),
                    OnboardingHighlightItem(
                        tag = "2",
                        title = s.onboardingStep3Item2Title,
                        body = s.onboardingStep3Item2Body
                    ),
                    OnboardingHighlightItem(
                        tag = "3",
                        title = s.onboardingStep3Item3Title,
                        body = s.onboardingStep3Item3Body
                    )
                ),
                footerNote = s.onboardingStep3Footer
            )
        )

        if (isAndroid12OrBelow) {
            basePages + listOf(
                OnboardingPageData(
                    stepLabel = s.onboardingStep4A12Label,
                    title = s.onboardingStep4A12Title,
                    subtitle = s.onboardingStep4A12Subtitle,
                    items = emptyList(),
                    footerNote = s.onboardingStep4A12Footer,
                    actionButtonText = s.onboardingStep4A12Button,
                    actionType = OnboardingActionType.ANDROID12_DIRECT_FLOW
                )
            )
        } else {
            basePages + listOf(
                OnboardingPageData(
                    stepLabel = s.onboardingStep4A13Label,
                    title = s.onboardingStep4A13Title,
                    subtitle = s.onboardingStep4A13Subtitle,
                    items = listOf(
                        OnboardingHighlightItem(
                            tag = "1",
                            title = s.onboardingStep4A13Item1Title,
                            body = s.onboardingStep4A13Item1Body
                        ),
                        OnboardingHighlightItem(
                            tag = "2",
                            title = s.onboardingStep4A13Item2Title,
                            body = s.onboardingStep4A13Item2Body
                        ),
                        OnboardingHighlightItem(
                            tag = "3",
                            title = s.onboardingStep4A13Item3Title,
                            body = s.onboardingStep4A13Item3Body
                        )
                    ),
                    footerNote = s.onboardingStep4A13Footer,
                    actionButtonText = s.onboardingStep4A13BtnStep1,
                    actionType = OnboardingActionType.ANDROID13_RESTRICTED_FLOW
                ),
                OnboardingPageData(
                    stepLabel = s.onboardingStep5A13Label,
                    title = s.onboardingStep5A13Title,
                    subtitle = s.onboardingStep5A13Subtitle,
                    items = listOf(
                        OnboardingHighlightItem(
                            tag = "1",
                            title = s.onboardingStep5A13Item1Title,
                            body = s.onboardingStep5A13Item1Body
                        ),
                        OnboardingHighlightItem(
                            tag = "2",
                            title = s.onboardingStep5A13Item2Title,
                            body = s.onboardingStep5A13Item2Body
                        ),
                        OnboardingHighlightItem(
                            tag = "%100",
                            title = s.onboardingStep5A13Item3Title,
                            body = s.onboardingStep5A13Item3Body
                        )
                    ),
                    footerNote = s.onboardingStep5A13Footer,
                    actionButtonText = s.onboardingStep5A13BtnStep3,
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
                            text = s.brandShort,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = s.brandBan,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = androidx.compose.ui.graphics.Color(0xFFF21B2D),
                            letterSpacing = (-0.4).sp
                        )
                    }
                    Text(
                        text = s.onboardingBadgeGuide,
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
                    text = if (currentStep < pages.size - 1) s.onboardingBtnSkip else s.onboardingBtnClose,
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
                                    s.onboardingServiceActive
                                } else {
                                    s.onboardingServiceInactive
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
                                        text = s.onboardingStep4A13BtnStep1,
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
                                        text = s.onboardingStep4A13BtnStep2,
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
                                text = s.onboardingBtnBack,
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
                                s.onboardingBtnAcceptContinue
                            } else if (currentStep < pages.size - 1) {
                                s.onboardingBtnNext
                            } else {
                                s.onboardingBtnFinish
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
                        text = s.onboardingModalTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = s.onboardingModalDesc,
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
                            text = s.onboardingModalBullet1,
                            fontSize = 11.5.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = s.onboardingModalBullet2,
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
                            Text(s.onboardingModalCancel, color = TextSecondary)
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
                            Text(s.onboardingModalAccept, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}

package com.example.scrollblock.ui.main

import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.scrollblock.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scrollblock.android12.core.Android12PermissionGuide
import com.example.scrollblock.i18n.AppLanguage
import com.example.scrollblock.i18n.LanguageManager
import com.example.scrollblock.preferences.BreakPolicy
import com.example.scrollblock.security.PinManager
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
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val s = LanguageManager.LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()

    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isYouTubeEnabled by viewModel.isYouTubeEnabled.collectAsStateWithLifecycle()
    val isInstagramEnabled by viewModel.isInstagramEnabled.collectAsStateWithLifecycle()
    val isTikTokEnabled by viewModel.isTikTokEnabled.collectAsStateWithLifecycle()
    val isInstagramDmReelsAllowed by viewModel.isInstagramDmReelsAllowed.collectAsStateWithLifecycle()
    val isTikTokDmVideosAllowed by viewModel.isTikTokDmVideosAllowed.collectAsStateWithLifecycle()
    val isInstagramCommentsBlocked by viewModel.isInstagramCommentsBlocked.collectAsStateWithLifecycle()
    val isTikTokCommentsBlocked by viewModel.isTikTokCommentsBlocked.collectAsStateWithLifecycle()
    val isYouTubeCommentsBlocked by viewModel.isYouTubeCommentsBlocked.collectAsStateWithLifecycle()

    val totalBlockedCount by viewModel.totalBlockedCount.collectAsStateWithLifecycle()
    val lastBlockedPlatform by viewModel.lastBlockedPlatform.collectAsStateWithLifecycle()
    val lastBlockedTimestamp by viewModel.lastBlockedTimestamp.collectAsStateWithLifecycle()

    // Mola / Cooldown
    val isBreakActive by viewModel.isBreakActive.collectAsStateWithLifecycle()
    val breakRemainingSeconds by viewModel.breakRemainingSeconds.collectAsStateWithLifecycle()
    val breakDurationMinutes by viewModel.breakDurationMinutes.collectAsStateWithLifecycle()
    val breakDailyLimit by viewModel.breakDailyLimit.collectAsStateWithLifecycle()
    val breakUsedToday by viewModel.breakUsedToday.collectAsStateWithLifecycle()

    // PIN
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()

    var showPinVerifyDialog by remember { mutableStateOf(false) }
    var pinVerifyTitle by remember(s) { mutableStateOf(s.pinVerifyDefaultTitle) }
    var pinVerifySubtitle by remember(s) { mutableStateOf(s.pinVerifyDefaultSub) }
    var onPinVerifiedAction by remember { mutableStateOf<() -> Unit>({}) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var pendingOpenAccessibilityAfterDisclosure by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val currentVersionCode = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (_: Exception) {
            41
        }
    }

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    fun executeProtectedAction(
        title: String = s.pinVerifyDefaultTitle,
        subtitle: String = s.pinVerifyDefaultSub,
        action: () -> Unit
    ) {
        if (!isPinEnabled) {
            action()
        } else {
            pinVerifyTitle = title
            pinVerifySubtitle = subtitle
            onPinVerifiedAction = action
            showPinVerifyDialog = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshServiceStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = NearBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Üst Bar & Durum Hapı
            TopBarSection(
                versionName = currentVersionName,
                isServiceActive = isServiceActive,
                isBreakActive = isBreakActive,
                onStatusClick = {
                    viewModel.refreshServiceStatus()
                    if (!isServiceActive) {
                        pendingOpenAccessibilityAfterDisclosure = true
                        showPrivacyDialog = true
                    }
                }
            )

            // 2. Erişilebilirlik Uyarısı (Servis kapalıysa görünür)
            if (!isServiceActive) {
                PermissionSetupBanner(
                    onOpenAppDetails = { AccessibilityUtils.openAppDetailsSettings(context) },
                    onOpenAccessibilitySettings = {
                        pendingOpenAccessibilityAfterDisclosure = true
                        showPrivacyDialog = true
                    },
                    onRefresh = { viewModel.refreshServiceStatus() }
                )
            }

            // 3. Gösterge Paneli: Engellenen Akış Sayacı
            StatsOverviewCard(
                totalCount = totalBlockedCount,
                lastPlatform = lastBlockedPlatform,
                lastTimestamp = lastBlockedTimestamp,
                onResetStats = {
                    executeProtectedAction(subtitle = s.dialogPinResetStatsSub) {
                        viewModel.resetStatistics()
                    }
                }
            )

            // 4. Mola / Cooldown Bölümü
            SectionHeader(title = s.sectionHeaderBreak)
            BreakCooldownCard(
                isBreakActive = isBreakActive,
                remainingSeconds = breakRemainingSeconds,
                selectedDurationMin = breakDurationMinutes,
                onSelectDuration = { duration ->
                    executeProtectedAction(
                        title = s.dialogPinDurationTitle,
                        subtitle = s.dialogPinDurationSub
                    ) {
                        viewModel.setBreakDurationMinutes(duration)
                    }
                },
                dailyLimit = breakDailyLimit,
                onSelectDailyLimit = { limit ->
                    executeProtectedAction(
                        title = s.dialogPinDailyLimitTitle,
                        subtitle = s.dialogPinDailyLimitSub
                    ) {
                        viewModel.setBreakDailyLimit(limit)
                    }
                },
                usedToday = breakUsedToday,
                canStartBreak = viewModel.canStartBreak(),
                onStartBreak = {
                    executeProtectedAction(
                        title = s.dialogPinStartBreakTitle,
                        subtitle = s.dialogPinStartBreakSub
                    ) {
                        val started = viewModel.startBreak()
                        if (!started) {
                            Toast.makeText(context, s.toastMolaHakkiDoldu, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onEndBreak = {
                    viewModel.endBreak()
                    Toast.makeText(context, s.toastMolaBitirildi, Toast.LENGTH_SHORT).show()
                }
            )

            // 5. Koruma Platformları & DM İzinleri
            SectionHeader(title = s.sectionHeaderPlatforms)
            PlatformsGroupCard(
                isInstagramEnabled = isInstagramEnabled,
                onInstagramToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = s.dialogPinIgDisableSub) {
                            viewModel.setInstagramEnabled(false)
                        }
                    } else {
                        viewModel.setInstagramEnabled(true)
                    }
                },
                isInstagramDmReelsAllowed = isInstagramDmReelsAllowed,
                onInstagramDmReelsToggle = { allowed ->
                    if (allowed) {
                        executeProtectedAction(subtitle = s.dialogPinIgDmSub) {
                            viewModel.setInstagramDmReelsAllowed(true)
                        }
                    } else {
                        viewModel.setInstagramDmReelsAllowed(false)
                    }
                },
                isInstagramCommentsBlocked = isInstagramCommentsBlocked,
                onInstagramCommentsToggle = { blocked ->
                    if (!blocked) {
                        executeProtectedAction(subtitle = s.dialogPinIgCommentsSub) {
                            viewModel.setInstagramCommentsBlocked(false)
                        }
                    } else {
                        viewModel.setInstagramCommentsBlocked(true)
                    }
                },
                isTikTokEnabled = isTikTokEnabled,
                onTikTokToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = s.dialogPinTtDisableSub) {
                            viewModel.setTikTokEnabled(false)
                        }
                    } else {
                        viewModel.setTikTokEnabled(true)
                    }
                },
                isTikTokDmVideosAllowed = isTikTokDmVideosAllowed,
                onTikTokDmVideosToggle = { allowed ->
                    if (allowed) {
                        executeProtectedAction(subtitle = s.dialogPinTtDmSub) {
                            viewModel.setTikTokDmVideosAllowed(true)
                        }
                    } else {
                        viewModel.setTikTokDmVideosAllowed(false)
                    }
                },
                isTikTokCommentsBlocked = isTikTokCommentsBlocked,
                onTikTokCommentsToggle = { blocked ->
                    if (!blocked) {
                        executeProtectedAction(subtitle = s.dialogPinTtCommentsSub) {
                            viewModel.setTikTokCommentsBlocked(false)
                        }
                    } else {
                        viewModel.setTikTokCommentsBlocked(true)
                    }
                },
                isYouTubeEnabled = isYouTubeEnabled,
                onYouTubeToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = s.dialogPinYtDisableSub) {
                            viewModel.setYouTubeEnabled(false)
                        }
                    } else {
                        viewModel.setYouTubeEnabled(true)
                    }
                },
                isYouTubeCommentsBlocked = isYouTubeCommentsBlocked,
                onYouTubeCommentsToggle = { blocked ->
                    if (!blocked) {
                        executeProtectedAction(subtitle = s.dialogPinYtCommentsSub) {
                            viewModel.setYouTubeCommentsBlocked(false)
                        }
                    } else {
                        viewModel.setYouTubeCommentsBlocked(true)
                    }
                }
            )

            // 6. Güvenlik & Sistem Ayarları
            SectionHeader(title = s.sectionHeaderSecurity)
            SystemSecurityCard(
                isPinEnabled = isPinEnabled,
                onTogglePin = { enable ->
                    if (enable) {
                        showPinSetupDialog = true
                    } else {
                        executeProtectedAction(
                            title = s.dialogPinRemoveTitle,
                            subtitle = s.dialogPinRemoveSub
                        ) {
                            viewModel.disablePin()
                            Toast.makeText(context, s.toastPinRemoved, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onChangePin = {
                    executeProtectedAction(
                        title = s.dialogPinChangeTitle,
                        subtitle = s.dialogPinChangeSub
                    ) {
                        showPinSetupDialog = true
                    }
                },
                isServiceActive = isServiceActive,
                onOpenAccessibilitySettings = {
                    if (!isServiceActive) {
                        pendingOpenAccessibilityAfterDisclosure = true
                        showPrivacyDialog = true
                    } else {
                        AccessibilityUtils.openAccessibilitySettings(context)
                    }
                },
                onReopenOnboarding = {
                    viewModel.reopenOnboarding()
                },
                onOpenPrivacyPolicy = {
                    pendingOpenAccessibilityAfterDisclosure = false
                    showPrivacyDialog = true
                },
                versionName = currentVersionName,
                versionCode = currentVersionCode
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // PIN Doğrulama Diyaloğu
        if (showPinVerifyDialog) {
            PinVerificationDialog(
                title = pinVerifyTitle,
                subtitle = pinVerifySubtitle,
                onDismiss = { showPinVerifyDialog = false },
                onVerify = { pin ->
                    when (val result = viewModel.verifyPin(pin)) {
                        is PinManager.VerifyResult.Success -> {
                            showPinVerifyDialog = false
                            onPinVerifiedAction()
                            true
                        }
                        is PinManager.VerifyResult.Wrong -> {
                            Toast.makeText(context, String.format(s.toastWrongPinLeftTemplate, result.attemptsLeft), Toast.LENGTH_SHORT).show()
                            false
                        }
                        is PinManager.VerifyResult.LockedOut -> {
                            val sec = result.remainingMs / 1000L
                            Toast.makeText(context, String.format(s.toastLockedOutTemplate, sec), Toast.LENGTH_LONG).show()
                            false
                        }
                    }
                }
            )
        }

        // PIN Belirleme Diyaloğu
        if (showPinSetupDialog) {
            PinSetupDialog(
                onDismiss = { showPinSetupDialog = false },
                onSavePin = { pin ->
                    if (viewModel.setPin(pin)) {
                        Toast.makeText(context, s.toastPinSaved, Toast.LENGTH_SHORT).show()
                        showPinSetupDialog = false
                    } else {
                        Toast.makeText(context, s.toastPinLength, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Google Play Uyumlu Erişilebilirlik Açık Rıza Beyanı & Gizlilik Politikası Diyaloğu
        if (showPrivacyDialog) {
            PrivacyAndDisclosureDialog(
                isPromptBeforePermission = pendingOpenAccessibilityAfterDisclosure,
                onDismiss = {
                    showPrivacyDialog = false
                    pendingOpenAccessibilityAfterDisclosure = false
                },
                onAcceptAndOpenSettings = {
                    showPrivacyDialog = false
                    pendingOpenAccessibilityAfterDisclosure = false
                    AccessibilityUtils.openAccessibilitySettings(context)
                }
            )
        }

    }
}

// ----------------- YARDIMCI BÖLÜM VE BİLEŞENLER -----------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMuted,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
    )
}

@Composable
private fun TopBarSection(
    versionName: String,
    isServiceActive: Boolean,
    isBreakActive: Boolean,
    onStatusClick: () -> Unit
) {
    val s = LanguageManager.LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.shortban_logo),
                contentDescription = "ShortBan Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = s.brandShort,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = s.brandBan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF21B2D),
                        letterSpacing = (-0.5).sp
                    )
                }
                Text(
                    text = "v$versionName",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }

        val pillBg by animateColorAsState(
            targetValue = when {
                isBreakActive -> AccentSignalSoft
                isServiceActive -> ActiveGreenContainer
                else -> AccentSignalSoft
            },
            label = "pillBg"
        )
        val dotColor = when {
            isBreakActive -> AccentSignal
            isServiceActive -> ActiveGreen
            else -> AccentSignal
        }
        val labelColor = when {
            isBreakActive -> Color(0xFFFCA5A5)
            isServiceActive -> ActiveGreenText
            else -> Color(0xFFFCA5A5)
        }
        val statusText = when {
            isBreakActive -> s.statusBreakMode
            isServiceActive -> s.statusActive
            else -> s.statusNeedPermission
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(pillBg)
                .border(1.dp, dotColor.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                .clickable { onStatusClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = statusText,
                color = labelColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PermissionSetupBanner(
    onOpenAppDetails: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val s = LanguageManager.LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, AccentSignal.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = s.permBannerTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = s.permBannerDesc,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = s.permBannerRefresh,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackgroundElevated)
                    .clickable { onRefresh() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        Text(
            text = s.permBannerStepGuide,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = NearBlack
                )
            ) {
                Text(
                    text = s.permBannerBtnStep1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onOpenAppDetails,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(
                    text = s.permBannerBtnStep2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatsOverviewCard(
    totalCount: Int,
    lastPlatform: String,
    lastTimestamp: Long,
    onResetStats: () -> Unit
) {
    val s = LanguageManager.LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = s.statsTitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "$totalCount",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = (-1.5).sp
                )
            }

            if (totalCount > 0) {
                Text(
                    text = s.statsResetButton,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardBackgroundElevated)
                        .clickable { onResetStats() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        val statusText = if (totalCount == 0 || lastPlatform.isEmpty()) {
            s.statsNoBlocksYet
        } else {
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                lastTimestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            String.format(s.statsLastBlockTemplate, lastPlatform, timeAgo)
        }

        Text(
            text = statusText,
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun BreakCooldownCard(
    isBreakActive: Boolean,
    remainingSeconds: Long,
    selectedDurationMin: Int,
    onSelectDuration: (Int) -> Unit,
    dailyLimit: Int,
    onSelectDailyLimit: (Int) -> Unit,
    usedToday: Int,
    canStartBreak: Boolean,
    onStartBreak: () -> Unit,
    onEndBreak: () -> Unit
) {
    val s = LanguageManager.LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isBreakActive) {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val timeFormatted = "%02d:%02d".format(minutes, seconds)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s.breakRemainingHeader,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentSignal,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = timeFormatted,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = s.breakActiveDesc,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Button(
                onClick = onEndBreak,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentSignalSoft,
                    contentColor = Color(0xFFFCA5A5)
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = s.breakEndAndReturn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Başlık ve Günlük Hak Bilgisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = s.breakTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = s.breakDesc,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                val limitBadge = if (dailyLimit == 0) s.breakLimitUnlimited else String.format(s.breakLimitUsedTemplate, usedToday, dailyLimit)
                Text(
                    text = limitBadge,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardBackgroundElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

            // Süre Seçici Çipler
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = s.breakDurationLabel,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakPolicy.DURATION_OPTIONS_MIN.forEach { duration ->
                        val isSelected = duration == selectedDurationMin
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentPrimary else CardBackgroundElevated)
                                .border(1.dp, if (isSelected) AccentPrimary else HairlineDivider, RoundedCornerShape(8.dp))
                                .clickable { onSelectDuration(duration) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$duration m",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) NearBlack else TextSecondary
                            )
                        }
                    }
                }
            }

            // Günlük Limit Seçici
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = s.breakDailyLimitLabel,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakPolicy.DAILY_LIMIT_OPTIONS.forEach { limit ->
                        val isSelected = limit == dailyLimit
                        val label = when (limit) {
                            0 -> s.breakLimitUnlimited
                            1 -> s.breakLimit1x
                            2 -> s.breakLimit2x
                            3 -> s.breakLimit3x
                            else -> "$limit x"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentPrimary else CardBackgroundElevated)
                                .border(1.dp, if (isSelected) AccentPrimary else HairlineDivider, RoundedCornerShape(8.dp))
                                .clickable { onSelectDailyLimit(limit) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) NearBlack else TextSecondary
                            )
                        }
                    }
                }
            }

            // Tam Genişlikte Mola Başlat Butonu (Asla kaymaz / sıkışmaz)
            Button(
                onClick = onStartBreak,
                enabled = canStartBreak,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = NearBlack
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = if (canStartBreak) {
                        String.format(s.breakStartButtonTemplate, selectedDurationMin)
                    } else {
                        s.breakLimitReached
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PlatformsGroupCard(
    isInstagramEnabled: Boolean,
    onInstagramToggle: (Boolean) -> Unit,
    isInstagramDmReelsAllowed: Boolean,
    onInstagramDmReelsToggle: (Boolean) -> Unit,
    isInstagramCommentsBlocked: Boolean,
    onInstagramCommentsToggle: (Boolean) -> Unit,
    isTikTokEnabled: Boolean,
    onTikTokToggle: (Boolean) -> Unit,
    isTikTokDmVideosAllowed: Boolean,
    onTikTokDmVideosToggle: (Boolean) -> Unit,
    isTikTokCommentsBlocked: Boolean,
    onTikTokCommentsToggle: (Boolean) -> Unit,
    isYouTubeEnabled: Boolean,
    onYouTubeToggle: (Boolean) -> Unit,
    isYouTubeCommentsBlocked: Boolean,
    onYouTubeCommentsToggle: (Boolean) -> Unit
) {
    val s = LanguageManager.LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        // 1. Instagram
        PlatformToggleRow(
            title = s.platformInstagramTitle,
            subtitle = s.platformInstagramDesc,
            checked = isInstagramEnabled,
            onCheckedChange = onInstagramToggle
        )

        AnimatedVisibility(visible = isInstagramEnabled) {
            DmSubSettingRow(
                title = s.platformInstagramDmTitle,
                subtitle = s.platformInstagramDmDesc,
                checked = isInstagramDmReelsAllowed,
                onCheckedChange = onInstagramDmReelsToggle
            )
        }

        DmSubSettingRow(
            title = s.platformInstagramCommentsTitle,
            subtitle = s.platformInstagramCommentsDesc,
            checked = isInstagramCommentsBlocked,
            onCheckedChange = onInstagramCommentsToggle
        )

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 2. TikTok
        PlatformToggleRow(
            title = s.platformTikTokTitle,
            subtitle = s.platformTikTokDesc,
            checked = isTikTokEnabled,
            onCheckedChange = onTikTokToggle
        )

        AnimatedVisibility(visible = isTikTokEnabled) {
            DmSubSettingRow(
                title = s.platformTikTokDmTitle,
                subtitle = s.platformTikTokDmDesc,
                checked = isTikTokDmVideosAllowed,
                onCheckedChange = onTikTokDmVideosToggle
            )
        }

        DmSubSettingRow(
            title = s.platformTikTokCommentsTitle,
            subtitle = s.platformTikTokCommentsDesc,
            checked = isTikTokCommentsBlocked,
            onCheckedChange = onTikTokCommentsToggle
        )

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 3. YouTube Shorts
        PlatformToggleRow(
            title = s.platformYouTubeTitle,
            subtitle = s.platformYouTubeDesc,
            checked = isYouTubeEnabled,
            onCheckedChange = onYouTubeToggle
        )

        DmSubSettingRow(
            title = s.platformYouTubeCommentsTitle,
            subtitle = s.platformYouTubeCommentsDesc,
            checked = isYouTubeCommentsBlocked,
            onCheckedChange = onYouTubeCommentsToggle
        )
    }
}

@Composable
private fun PlatformToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        MinimalSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DmSubSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NearBlack.copy(alpha = 0.55f))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(30.dp)
                    .background(if (checked) AccentPrimary else CardBorder)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (checked) TextPrimary else TextSecondary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        MinimalSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SystemSecurityCard(
    isPinEnabled: Boolean,
    onTogglePin: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    isServiceActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    versionName: String,
    versionCode: Int
) {
    val context = LocalContext.current
    val s = LanguageManager.LocalAppStrings.current
    val currentLang by LanguageManager.currentLanguage.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        // 1. PIN Kilidi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTogglePin(!isPinEnabled) }
                .padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = s.securityPinTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (isPinEnabled) {
                        Text(
                            text = s.securityChangePinBtn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CardBackgroundElevated)
                                .clickable { onChangePin() }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (isPinEnabled) s.securityPinDescOn else s.securityPinDescOff,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            MinimalSwitch(checked = isPinEnabled, onCheckedChange = onTogglePin)
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 2. Erişilebilirlik Kısayolu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAccessibilitySettings() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = s.securityAccessibilityTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = if (isServiceActive) s.securityAccessibilityDescRunning else s.securityAccessibilityDescStopped,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = s.securityOpenBtn,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackgroundElevated)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 3. Kullanım ve Kurulum Rehberi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReopenOnboarding() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = s.securityGuideTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = s.securityGuideDesc,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = s.securityGuideBtn,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackgroundElevated)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 3.5 Gizlilik Politikası ve Erişilebilirlik Beyanı (Google Play Zorunlu Alan)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPrivacyPolicy() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = s.securityPrivacyTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = s.securityPrivacyDesc,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = s.securityReadBtn,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackgroundElevated)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 3.8 Dil Seçimi (TR / EN)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = s.securityLanguageTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = s.securityLanguageDesc,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackgroundElevated)
                    .border(1.dp, HairlineDivider, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppLanguage.values().forEach { lang ->
                    val isSelected = currentLang == lang
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) AccentPrimary else Color.Transparent)
                            .clickable {
                                if (!isSelected) {
                                    LanguageManager.setLanguage(context, lang)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang.code.uppercase(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NearBlack else TextSecondary
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 4. Sürüm Bilgisi (Google Play uyumlu statik gösterim)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = s.securityVersionTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = s.securityVersionSub,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "v$versionName (Build $versionCode)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
    }
}

// ----------------- MİNİMAL ANAHTAR (SWITCH) -----------------

@Composable
private fun MinimalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackBg by animateColorAsState(
        targetValue = if (checked) AccentPrimary else CardBackgroundElevated,
        label = "trackBg"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) NearBlack else TextMuted,
        label = "thumbColor"
    )
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 18f else 2f,
        label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackBg)
            .border(1.dp, if (checked) AccentPrimary else HairlineDivider, RoundedCornerShape(13.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

// ----------------- PIN DİYALOGLARI -----------------

@Composable
private fun PinVerificationDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    val s = LanguageManager.LocalAppStrings.current
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.pinFieldPlaceholder, color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(s.pinDialogCancelBtn, color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (pin.length == 4) {
                            if (!onVerify(pin)) {
                                pin = ""
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = NearBlack
                    )
                ) {
                    Text(s.pinDialogConfirmBtn, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    val s = LanguageManager.LocalAppStrings.current
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = s.pinSetupNewHeader,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = s.pinSetupSubHeader,
                fontSize = 12.sp,
                color = TextSecondary
            )

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        pin = it
                        errorText = ""
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.pinFieldPlaceholder, color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPrimary
                )
            )

            OutlinedTextField(
                value = pinConfirm,
                onValueChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        pinConfirm = it
                        errorText = ""
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.pinConfirmFieldPlaceholder, color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPrimary
                )
            )

            if (errorText.isNotEmpty()) {
                Text(text = errorText, color = AccentSignal, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(s.pinDialogCancelBtn, color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (pin.length != 4) {
                            errorText = s.pinSetupMustBe4
                        } else if (pin != pinConfirm) {
                            errorText = s.pinSetupMismatch
                        } else {
                            onSavePin(pin)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = NearBlack
                    )
                ) {
                    Text(s.pinSetupSaveBtn, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PrivacyAndDisclosureDialog(
    isPromptBeforePermission: Boolean,
    onDismiss: () -> Unit,
    onAcceptAndOpenSettings: () -> Unit
) {
    val s = LanguageManager.LocalAppStrings.current
    Dialog(onDismissRequest = onDismiss) {
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
                text = s.privacyDialogTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = s.privacyDialogIntro,
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = s.privacySection1,
                    fontSize = 11.5.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
                Text(
                    text = s.privacySection2,
                    fontSize = 11.5.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
                Text(
                    text = s.privacySection3,
                    fontSize = 11.5.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
                Text(
                    text = s.privacySection4,
                    fontSize = 11.5.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(if (isPromptBeforePermission) s.privacyDialogDismiss else s.privacyDialogAcknowledge, color = TextSecondary)
                }
                if (isPromptBeforePermission) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onAcceptAndOpenSettings,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = NearBlack
                        )
                    ) {
                        Text(s.privacyDialogAcceptSettings, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

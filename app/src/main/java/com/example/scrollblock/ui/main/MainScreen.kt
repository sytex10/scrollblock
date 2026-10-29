package com.example.scrollblock.ui.main

import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.scrollblock.updater.UpdateManager
import com.example.scrollblock.util.AccessibilityUtils
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isYouTubeEnabled by viewModel.isYouTubeEnabled.collectAsStateWithLifecycle()
    val isInstagramEnabled by viewModel.isInstagramEnabled.collectAsStateWithLifecycle()
    val isTikTokEnabled by viewModel.isTikTokEnabled.collectAsStateWithLifecycle()
    val isInstagramDmReelsAllowed by viewModel.isInstagramDmReelsAllowed.collectAsStateWithLifecycle()
    val isTikTokDmVideosAllowed by viewModel.isTikTokDmVideosAllowed.collectAsStateWithLifecycle()

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
    var pinVerifyTitle by remember { mutableStateOf("PIN Doğrulaması") }
    var pinVerifySubtitle by remember { mutableStateOf("Devam etmek için 4 haneli PIN kodunuzu girin.") }
    var onPinVerifiedAction by remember { mutableStateOf<() -> Unit>({}) }

    var showPinSetupDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    var remoteUpdateInfo by remember { mutableStateOf<UpdateManager.RemoteVersionInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val currentVersionCode = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (_: Exception) {
            18
        }
    }

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.7"
        } catch (_: Exception) {
            "2.7"
        }
    }

    fun executeProtectedAction(
        title: String = "PIN Doğrulaması",
        subtitle: String = "Devam etmek için 4 haneli PIN kodunuzu girin.",
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

    LaunchedEffect(Unit) {
        val info = UpdateManager.checkRemoteVersion()
        if (info != null && info.versionCode > currentVersionCode) {
            remoteUpdateInfo = info
        }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    Toast.makeText(context, "Paket yükleyici açılıyor...", Toast.LENGTH_SHORT).show()
                    UpdateManager.installFromPickedUri(context, uri)
                } catch (e: Exception) {
                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
                        AccessibilityUtils.openAccessibilitySettings(context)
                    }
                }
            )

            // 2. Erişilebilirlik Uyarısı (Servis kapalıysa görünür)
            if (!isServiceActive) {
                PermissionSetupBanner(
                    onOpenAppDetails = { AccessibilityUtils.openAppDetailsSettings(context) },
                    onOpenAccessibilitySettings = { AccessibilityUtils.openAccessibilitySettings(context) },
                    onRefresh = { viewModel.refreshServiceStatus() }
                )
            }

            // 3. Gösterge Paneli: Engellenen Akış Sayacı
            StatsOverviewCard(
                totalCount = totalBlockedCount,
                lastPlatform = lastBlockedPlatform,
                lastTimestamp = lastBlockedTimestamp,
                onResetStats = {
                    executeProtectedAction(subtitle = "Sayacı sıfırlamak için PIN girin.") {
                        viewModel.resetStatistics()
                    }
                }
            )

            // 4. Mola / Cooldown Bölümü
            SectionHeader(title = "MOLA VE DİNLENME")
            BreakCooldownCard(
                isBreakActive = isBreakActive,
                remainingSeconds = breakRemainingSeconds,
                selectedDurationMin = breakDurationMinutes,
                onSelectDuration = { duration ->
                    executeProtectedAction(
                        title = "Mola Süresi",
                        subtitle = "Mola süresini değiştirmek için PIN girin."
                    ) {
                        viewModel.setBreakDurationMinutes(duration)
                    }
                },
                dailyLimit = breakDailyLimit,
                onSelectDailyLimit = { limit ->
                    executeProtectedAction(
                        title = "Günlük Limit",
                        subtitle = "Günlük mola limitini değiştirmek için PIN girin."
                    ) {
                        viewModel.setBreakDailyLimit(limit)
                    }
                },
                usedToday = breakUsedToday,
                canStartBreak = viewModel.canStartBreak(),
                onStartBreak = {
                    executeProtectedAction(
                        title = "Mola Başlat",
                        subtitle = "Geçici molayı başlatmak için PIN girin."
                    ) {
                        val started = viewModel.startBreak()
                        if (!started) {
                            Toast.makeText(context, "Bugünkü mola hakkınız doldu.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onEndBreak = {
                    viewModel.endBreak()
                    Toast.makeText(context, "Mola bitirildi. Koruma devrede.", Toast.LENGTH_SHORT).show()
                }
            )

            // 5. Koruma Platformları & DM İzinleri
            SectionHeader(title = "KORUMA PLATFORMLARI")
            PlatformsGroupCard(
                isInstagramEnabled = isInstagramEnabled,
                onInstagramToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = "Instagram engelini kapatmak için PIN girin.") {
                            viewModel.setInstagramEnabled(false)
                        }
                    } else {
                        viewModel.setInstagramEnabled(true)
                    }
                },
                isInstagramDmReelsAllowed = isInstagramDmReelsAllowed,
                onInstagramDmReelsToggle = { allowed ->
                    if (allowed) {
                        executeProtectedAction(subtitle = "Instagram DM video iznini açmak için PIN girin.") {
                            viewModel.setInstagramDmReelsAllowed(true)
                        }
                    } else {
                        viewModel.setInstagramDmReelsAllowed(false)
                    }
                },
                isTikTokEnabled = isTikTokEnabled,
                onTikTokToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = "TikTok engelini kapatmak için PIN girin.") {
                            viewModel.setTikTokEnabled(false)
                        }
                    } else {
                        viewModel.setTikTokEnabled(true)
                    }
                },
                isTikTokDmVideosAllowed = isTikTokDmVideosAllowed,
                onTikTokDmVideosToggle = { allowed ->
                    if (allowed) {
                        executeProtectedAction(subtitle = "TikTok DM video iznini açmak için PIN girin.") {
                            viewModel.setTikTokDmVideosAllowed(true)
                        }
                    } else {
                        viewModel.setTikTokDmVideosAllowed(false)
                    }
                },
                isYouTubeEnabled = isYouTubeEnabled,
                onYouTubeToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = "YouTube Shorts engelini kapatmak için PIN girin.") {
                            viewModel.setYouTubeEnabled(false)
                        }
                    } else {
                        viewModel.setYouTubeEnabled(true)
                    }
                }
            )

            // 6. Güvenlik & Sistem Ayarları
            SectionHeader(title = "GÜVENLİK VE SİSTEM")
            SystemSecurityCard(
                isPinEnabled = isPinEnabled,
                onTogglePin = { enable ->
                    if (enable) {
                        showPinSetupDialog = true
                    } else {
                        executeProtectedAction(
                            title = "PIN Kilidini Kaldır",
                            subtitle = "Kaldırmak için mevcut PIN kodunuzu girin."
                        ) {
                            viewModel.disablePin()
                            Toast.makeText(context, "PIN kilidi kaldırıldı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onChangePin = {
                    executeProtectedAction(
                        title = "PIN Değiştir",
                        subtitle = "Önce mevcut PIN kodunuzu doğrulayın."
                    ) {
                        showPinSetupDialog = true
                    }
                },
                isServiceActive = isServiceActive,
                onOpenAccessibilitySettings = {
                    AccessibilityUtils.openAccessibilitySettings(context)
                },
                onReopenOnboarding = {
                    viewModel.reopenOnboarding()
                },
                onPickLocalApk = {
                    apkPickerLauncher.launch("application/vnd.android.package-archive")
                },
                onCheckUpdate = {
                    isCheckingUpdate = true
                    coroutineScope.launch {
                        val info = UpdateManager.checkRemoteVersion()
                        isCheckingUpdate = false
                        if (info != null && info.versionCode > currentVersionCode) {
                            remoteUpdateInfo = info
                        } else {
                            Toast.makeText(context, "Sürüm güncel (v$currentVersionName)", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                isCheckingUpdate = isCheckingUpdate
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
                            Toast.makeText(context, "Hatalı PIN (${result.attemptsLeft} deneme kaldı)", Toast.LENGTH_SHORT).show()
                            false
                        }
                        is PinManager.VerifyResult.LockedOut -> {
                            val sec = result.remainingMs / 1000L
                            Toast.makeText(context, "Çok fazla hatalı deneme. $sec sn bekleyin.", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, "PIN kaydedildi.", Toast.LENGTH_SHORT).show()
                        showPinSetupDialog = false
                    } else {
                        Toast.makeText(context, "PIN 4 rakamdan oluşmalıdır.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Güncelleme Zorunlu Modalı
        if (remoteUpdateInfo != null && remoteUpdateInfo!!.versionCode > currentVersionCode) {
            val info = remoteUpdateInfo!!
            var isDownloadingMandatory by remember { mutableStateOf(false) }
            var mandatoryProgress by remember { mutableFloatStateOf(0f) }

            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
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
                        text = "Yeni Sürüm Hazır (v${info.versionName})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Text(
                        text = "ScrollBlock v${info.versionName} güncellemesi mevcut.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    if (info.changelog.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBackgroundElevated)
                                .border(1.dp, HairlineDivider, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = info.changelog,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    if (isDownloadingMandatory) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { mandatoryProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentPrimary,
                                trackColor = CardBackgroundElevated
                            )
                            Text(
                                text = "İndiriliyor: %${(mandatoryProgress * 100).toInt()}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isDownloadingMandatory = true
                                coroutineScope.launch {
                                    UpdateManager.downloadAndInstall(
                                        context = context,
                                        urlString = info.apkUrl,
                                        onProgress = { progressInt ->
                                            mandatoryProgress = progressInt / 100f
                                        },
                                        onError = { errorMsg ->
                                            isDownloadingMandatory = false
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPrimary,
                                contentColor = NearBlack
                            )
                        ) {
                            Text(
                                text = "İndir ve Güncelle",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "scrollblock",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = "v$versionName",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 3.dp)
            )
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
            isBreakActive -> "Mola"
            isServiceActive -> "Aktif"
            else -> "İzin Gerekli"
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
                    text = "Erişilebilirlik Servisi Kapalı",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Kısa videoları algılayıp yönlendirmek için servisi açın.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = "Yenile",
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
            text = "Android 13+ kullanıyorsanız önce Uygulama Bilgisi ekranında sağ üstteki (⋮) menüsünden 'Kısıtlı ayarlara izin ver' seçeneğini etkinleştirin.",
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenAppDetails,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(
                    text = "1. Uygulama Bilgisi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

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
                    text = "2. Erişilebilirliği Aç",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
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
                    text = "Toplam Engellenen Akış",
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
                    text = "Sıfırla",
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
            "Henüz hiçbir kısa video akışı engellenmedi"
        } else {
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                lastTimestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            "Son engelleme: $lastPlatform · $timeAgo"
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
                    text = "MOLA AKTİF — KALAN SÜRE",
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
                    text = "Engelleme geçici olarak durduruldu. Süre dolunca otomatik olarak tekrar devreye girecektir.",
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
                    text = "Molayı Bitir ve Korumaya Dön",
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
                        text = "Geçici Mola Modu",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Belirli bir süre için engellemeyi geçici olarak duraklatın.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                val limitBadge = if (dailyLimit == 0) "Sınırsız" else "$usedToday/$dailyLimit kullanıldı"
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
                    text = "Mola Süresi Seçin",
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
                                text = "$duration dk",
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
                    text = "Günlük Mola Hakkı Sınırı",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakPolicy.DAILY_LIMIT_OPTIONS.forEach { limit ->
                        val isSelected = limit == dailyLimit
                        val label = if (limit == 0) "Sınırsız" else "$limit kez"
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
                        "$selectedDurationMin Dakika Mola Başlat"
                    } else {
                        "Bugünkü Mola Hakkınız Doldu"
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
    isTikTokEnabled: Boolean,
    onTikTokToggle: (Boolean) -> Unit,
    isTikTokDmVideosAllowed: Boolean,
    onTikTokDmVideosToggle: (Boolean) -> Unit,
    isYouTubeEnabled: Boolean,
    onYouTubeToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        // 1. Instagram
        PlatformToggleRow(
            title = "Instagram",
            subtitle = "Reels sekmesini Ana Sayfa'ya yönlendirir (uygulamadan atmaz)",
            checked = isInstagramEnabled,
            onCheckedChange = onInstagramToggle
        )

        AnimatedVisibility(visible = isInstagramEnabled) {
            DmSubSettingRow(
                title = "DM'den gelen ilk Reels'a izin ver",
                subtitle = "Yalnızca sohbetten açılan ilk videoya izin verir; sonraki videoya kaydırdığınız an kapatır.",
                checked = isInstagramDmReelsAllowed,
                onCheckedChange = onInstagramDmReelsToggle
            )
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 2. TikTok
        PlatformToggleRow(
            title = "TikTok",
            subtitle = "Sizin İçin akışını Gelen Kutusu'na yönlendirir (uygulamadan atmaz)",
            checked = isTikTokEnabled,
            onCheckedChange = onTikTokToggle
        )

        AnimatedVisibility(visible = isTikTokEnabled) {
            DmSubSettingRow(
                title = "DM'den gelen ilk videoya izin ver",
                subtitle = "Sohbetten açılan ilk video izlenir; sonraki videoya kaydırınca sohbete döner.",
                checked = isTikTokDmVideosAllowed,
                onCheckedChange = onTikTokDmVideosToggle
            )
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // 3. YouTube Shorts
        PlatformToggleRow(
            title = "YouTube Shorts",
            subtitle = "Shorts oynatıcısını kapatır, normal uzun videolar çalışır",
            checked = isYouTubeEnabled,
            onCheckedChange = onYouTubeToggle
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
    onPickLocalApk: () -> Unit,
    onCheckUpdate: () -> Unit,
    isCheckingUpdate: Boolean
) {
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
                        text = "Ebeveyn / Güvenlik PIN Kilidi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (isPinEnabled) {
                        Text(
                            text = "Değiştir",
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
                    text = if (isPinEnabled) "Koruma anahtarları ve mola 4 haneli PIN ile korunuyor." else "Ayarları ve molayı 4 haneli şifreyle koruma altına alın.",
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
                    text = "Erişilebilirlik Servis Ayarları",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = if (isServiceActive) "Servis arka planda çalışıyor · Ayarları aç" else "Servis kapalı · İzin vermek için dokunun",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = "Aç",
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
                    text = "Kullanım ve Kurulum Rehberi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Uygulamanın nasıl çalıştığını ve izin adımlarını görüntüleyin",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = "Rehber",
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

        // 4. Uygulama Güncellemesi (Tam genişlikte iki dengeli buton - kayma yapmaz)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Uygulama Güncellemesi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "GitHub üzerinden yeni sürümü denetleyin veya indirdiğiniz APK'yı kurun",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackgroundElevated)
                        .border(1.dp, HairlineDivider, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isCheckingUpdate) { onCheckUpdate() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCheckingUpdate) "Denetleniyor..." else "Güncellemeyi Kontrol Et",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackgroundElevated)
                        .border(1.dp, HairlineDivider, RoundedCornerShape(8.dp))
                        .clickable { onPickLocalApk() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yerel APK Seç",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
                placeholder = { Text("4 haneli PIN", color = TextMuted) },
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
                    Text("İptal", color = TextSecondary)
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
                    Text("Onayla", fontWeight = FontWeight.SemiBold)
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
                text = "Yeni PIN Belirleyin",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Ayarları ve molayı korumak için 4 haneli bir şifre girin.",
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
                placeholder = { Text("4 haneli PIN", color = TextMuted) },
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
                placeholder = { Text("PIN'i tekrar girin", color = TextMuted) },
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
                    Text("İptal", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (pin.length != 4) {
                            errorText = "PIN 4 haneli olmalıdır."
                        } else if (pin != pinConfirm) {
                            errorText = "PIN kodları eşleşmiyor."
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
                    Text("Kaydet", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

package com.example.scrollblock.ui.main

import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scrollblock.preferences.BreakPolicy
import com.example.scrollblock.security.PinHasher
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
            16
        }
    }

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.5"
        } catch (_: Exception) {
            "2.5"
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
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Üst Başlık ve Durum Göstergesi
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

            // 2. Erişilebilirlik Kurulum Kartı (Yalnızca servis kapalıysa veya kullanıcı açmak istediğinde)
            if (!isServiceActive) {
                PermissionSetupBanner(
                    onOpenAppDetails = { AccessibilityUtils.openAppDetailsSettings(context) },
                    onOpenAccessibilitySettings = { AccessibilityUtils.openAccessibilitySettings(context) },
                    onRefresh = { viewModel.refreshServiceStatus() }
                )
            }

            // 3. Sayaç & Mola Kontrolü
            OverviewAndBreakSection(
                totalCount = totalBlockedCount,
                lastPlatform = lastBlockedPlatform,
                lastTimestamp = lastBlockedTimestamp,
                onResetStats = {
                    executeProtectedAction(subtitle = "Sayacı sıfırlamak için PIN girin.") {
                        viewModel.resetStatistics()
                    }
                },
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
                    Toast.makeText(context, "Mola bitirildi.", Toast.LENGTH_SHORT).show()
                }
            )

            // 4. Platformlar ve DM Tek Video İzinleri
            PlatformsGroupSection(
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

            // 5. Güvenlik & Güncelleme
            SystemAndSecuritySection(
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

            Spacer(modifier = Modifier.height(12.dp))
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

        // Uzaktan Güncelleme Diyaloğu
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
                        text = "Devam etmek için uygulamayı güncelleyin.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    if (info.changelog.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NearBlack)
                                .border(1.dp, HairlineDivider, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = info.changelog,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    if (isDownloadingMandatory) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { mandatoryProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = AccentPrimary,
                                trackColor = CardBackgroundElevated
                            )
                            Text(
                                text = "%${(mandatoryProgress * 100).toInt()} indirildi",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!UpdateManager.canRequestPackageInstalls(context)) {
                                    Toast.makeText(context, "Bilinmeyen kaynaklardan yükleme izni verin", Toast.LENGTH_SHORT).show()
                                    UpdateManager.openInstallPermissionSettings(context)
                                } else {
                                    isDownloadingMandatory = true
                                    coroutineScope.launch {
                                        UpdateManager.downloadAndInstall(
                                            context = context,
                                            urlString = info.apkUrl,
                                            onProgress = { p -> mandatoryProgress = p / 100f },
                                            onError = { err ->
                                                isDownloadingMandatory = false
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
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
                                text = "İndir ve Kur",
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

// ---------------- BÖLÜMLER ----------------

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
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "scrollblock",
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Erişilebilirlik servisi kapalı",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Kısa video akışlarını algılamak için servisi açın.",
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

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Android 13+ kısıtlı ayar kilidi varsa önce Uygulama Bilgisi ekranında sağ üstteki ⋮ menüsünden \"Kısıtlı ayarlara izin ver\" seçeneğini açın.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
        }

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
                    fontWeight = FontWeight.Medium
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
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OverviewAndBreakSection(
    totalCount: Int,
    lastPlatform: String,
    lastTimestamp: Long,
    onResetStats: () -> Unit,
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
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
    ) {
        // Üst Kısım: Engellenen Akış Sayacı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Engellenen akış",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "$totalCount",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = (-1).sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (lastPlatform.isNotEmpty() && lastTimestamp > 0L) {
                    val relativeTime = DateUtils.getRelativeTimeSpanString(
                        lastTimestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    Text(
                        text = "$lastPlatform · $relativeTime",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                if (totalCount > 0) {
                    Text(
                        text = "Sıfırla",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onResetStats() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

        // Alt Kısım: Mola / Cooldown Kontrolü
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isBreakActive) "Mola aktif" else "Geçici mola",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    val usageText = if (dailyLimit == 0) {
                        "Bugün $usedToday kez kullanıldı · Sınırsız"
                    } else {
                        val left = (dailyLimit - usedToday).coerceAtLeast(0)
                        "Bugün kalan hak: $left / $dailyLimit"
                    }
                    Text(
                        text = usageText,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Günlük limit seçici (kompakt)
                if (!isBreakActive) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NearBlack)
                            .border(1.dp, HairlineDivider, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BreakPolicy.DAILY_LIMIT_OPTIONS.forEach { limit ->
                            val selected = limit == dailyLimit
                            val text = if (limit == 0) "∞" else "${limit}x"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) CardBackgroundElevated else Color.Transparent)
                                    .clickable { onSelectDailyLimit(limit) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) TextPrimary else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            if (isBreakActive) {
                val min = remainingSeconds / 60
                val sec = remainingSeconds % 60
                val timeString = String.format("%02d:%02d", min, sec)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NearBlack)
                        .border(1.dp, AccentSignal.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kalan süre",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = timeString,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = onEndBreak,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentSignal,
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Bitir",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Süre seçimi
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NearBlack)
                            .border(1.dp, HairlineDivider, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BreakPolicy.DURATION_OPTIONS_MIN.forEach { duration ->
                            val isSelected = duration == selectedDurationMin
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (isSelected) CardBackgroundElevated else Color.Transparent)
                                    .clickable { onSelectDuration(duration) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${duration}dk",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextMuted
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onStartBreak,
                        enabled = canStartBreak,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = NearBlack,
                            disabledContainerColor = CardBackgroundElevated,
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Text(
                            text = if (canStartBreak) "Başlat" else "Limit Doldu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformsGroupSection(
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Platformlar",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 2.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        ) {
            // 1. Instagram
            PlatformToggleRow(
                title = "Instagram Reels",
                subtitle = "Reels sekmesini ve keşfet videolarını engeller",
                checked = isInstagramEnabled,
                onCheckedChange = onInstagramToggle
            )

            AnimatedVisibility(visible = isInstagramEnabled) {
                DmAllowanceSubRow(
                    title = "DM'den gelen tek videoya izin ver",
                    subtitle = "Sohbetten açılan ilk Reel izlenir, aşağı kaydırınca kapanır",
                    checked = isInstagramDmReelsAllowed,
                    onCheckedChange = onInstagramDmReelsToggle
                )
            }

            HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

            // 2. TikTok
            PlatformToggleRow(
                title = "TikTok",
                subtitle = "Sizin İçin akışını engeller, Gelen Kutusu açık kalır",
                checked = isTikTokEnabled,
                onCheckedChange = onTikTokToggle
            )

            AnimatedVisibility(visible = isTikTokEnabled) {
                DmAllowanceSubRow(
                    title = "DM'den gelen tek videoya izin ver",
                    subtitle = "Mesajlardan açılan ilk video izlenir, kaydırınca sohbete döner",
                    checked = isTikTokDmVideosAllowed,
                    onCheckedChange = onTikTokDmVideosToggle
                )
            }

            HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

            // 3. YouTube Shorts
            PlatformToggleRow(
                title = "YouTube Shorts",
                subtitle = "Yalnızca Shorts oynatıcısını kapatır, normal videolar çalışır",
                checked = isYouTubeEnabled,
                onCheckedChange = onYouTubeToggle
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        MinimalSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DmAllowanceSubRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NearBlack.copy(alpha = 0.45f))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 26.dp, end = 18.dp, top = 11.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
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

        MinimalSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SystemAndSecuritySection(
    isPinEnabled: Boolean,
    onTogglePin: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    isServiceActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onPickLocalApk: () -> Unit,
    onCheckUpdate: () -> Unit,
    isCheckingUpdate: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAdvancedUpdater by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Güvenlik ve Sistem",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 2.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        ) {
            // PIN Kilidi Satırı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTogglePin(!isPinEnabled) }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PIN kilidi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        if (isPinEnabled) {
                            Text(
                                text = "Değiştir",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CardBackgroundElevated)
                                    .clickable { onChangePin() }
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isPinEnabled) "Ayarlar ve mola 4 haneli kodla korunuyor" else "Ayarları değiştirmek için kod iste",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                MinimalSwitch(
                    checked = isPinEnabled,
                    onCheckedChange = onTogglePin
                )
            }

            HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

            // Erişilebilirlik Ayarları Kısayolu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAccessibilitySettings() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Erişilebilirlik ayarları",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isServiceActive) "Servis çalışıyor · Sistem ayarlarını aç" else "Servis kapalı · İzin vermek için dokunun",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "Aç",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = HairlineDivider, thickness = 1.dp)

            // Güncelleme Bölümü
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uygulama güncelleme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (showAdvancedUpdater) "Gizle" else "Diğer seçenekler",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showAdvancedUpdater = !showAdvancedUpdater }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCheckUpdate,
                        enabled = !isCheckingUpdate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(vertical = 9.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text(
                            text = if (isCheckingUpdate) "Bakılıyor..." else "Güncelleme Ara",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = onPickLocalApk,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(vertical = 9.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text(
                            text = "APK Dosyası Seç",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                AnimatedVisibility(visible = showAdvancedUpdater) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isDownloading = true
                                coroutineScope.launch {
                                    UpdateManager.downloadAndInstall(
                                        context = context,
                                        urlString = UpdateManager.DEFAULT_GITHUB_APK_URL,
                                        onProgress = { p -> downloadProgress = p / 100f },
                                        onError = { err ->
                                            isDownloading = false
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                    isDownloading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(9.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text(
                                text = "GitHub'dan Son Sürümü İndir",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = downloadUrl,
                                onValueChange = { downloadUrl = it },
                                placeholder = { Text("APK bağlantısı...", fontSize = 11.sp, color = TextMuted) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = AccentPrimary,
                                    unfocusedBorderColor = CardBorder
                                )
                            )

                            Button(
                                onClick = {
                                    if (downloadUrl.isNotBlank()) {
                                        isDownloading = true
                                        coroutineScope.launch {
                                            UpdateManager.downloadAndInstall(
                                                context = context,
                                                urlString = downloadUrl.trim(),
                                                onProgress = { p -> downloadProgress = p / 100f },
                                                onError = { err ->
                                                    isDownloading = false
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                            isDownloading = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPrimary,
                                    contentColor = NearBlack
                                )
                            ) {
                                Text("Kur", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }

                        if (isDownloading) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = AccentPrimary,
                                trackColor = CardBackgroundElevated
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = NearBlack,
            checkedTrackColor = AccentPrimary,
            checkedBorderColor = AccentPrimary,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = NearBlack,
            uncheckedBorderColor = CardBorder
        )
    )
}

// ---------------- PIN DİYALOGLARI ----------------

@Composable
private fun PinVerificationDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var enteredPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = enteredPin,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) enteredPin = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "••••",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier.width(130.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(text = "İptal", color = TextSecondary, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (enteredPin.length == 4) {
                            onVerify(enteredPin)
                        }
                    },
                    enabled = enteredPin.length == 4,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = NearBlack,
                        disabledContainerColor = CardBackgroundElevated,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Text(text = "Onayla", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "4 Haneli PIN Belirle",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Text(
                text = "Korumayı kapatırken veya mola başlatırken sorulur.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it
                    errorMessage = null
                },
                label = { Text("PIN kodu", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder
                )
            )

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it
                    errorMessage = null
                },
                label = { Text("PIN tekrar", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = AccentSignal,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(text = "İptal", color = TextSecondary, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (!PinHasher.isValidFormat(pin)) {
                            errorMessage = "PIN 4 rakamdan oluşmalıdır."
                        } else if (pin != confirmPin) {
                            errorMessage = "PIN kodları eşleşmiyor."
                        } else {
                            onSavePin(pin)
                        }
                    },
                    enabled = pin.length == 4 && confirmPin.length == 4,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = NearBlack,
                        disabledContainerColor = CardBackgroundElevated,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Text(text = "Kaydet", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

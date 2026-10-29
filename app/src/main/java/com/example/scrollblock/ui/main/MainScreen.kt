package com.example.scrollblock.ui.main

import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.scrollblock.theme.ActiveGreen
import com.example.scrollblock.theme.ActiveGreenContainer
import com.example.scrollblock.theme.ActiveGreenText
import com.example.scrollblock.theme.CardBackground
import com.example.scrollblock.theme.CardBackgroundElevated
import com.example.scrollblock.theme.CardBorder
import com.example.scrollblock.theme.ElectricBlue
import com.example.scrollblock.theme.ElectricBlueGlow
import com.example.scrollblock.theme.ElectricBlueSubtle
import com.example.scrollblock.theme.ElectricBlueVariant
import com.example.scrollblock.theme.InactiveAmber
import com.example.scrollblock.theme.InactiveAmberContainer
import com.example.scrollblock.theme.InactiveAmberText
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

    val totalBlockedCount by viewModel.totalBlockedCount.collectAsStateWithLifecycle()
    val lastBlockedPlatform by viewModel.lastBlockedPlatform.collectAsStateWithLifecycle()
    val lastBlockedTimestamp by viewModel.lastBlockedTimestamp.collectAsStateWithLifecycle()

    // Mola / Cooldown State
    val isBreakActive by viewModel.isBreakActive.collectAsStateWithLifecycle()
    val breakRemainingSeconds by viewModel.breakRemainingSeconds.collectAsStateWithLifecycle()
    val breakDurationMinutes by viewModel.breakDurationMinutes.collectAsStateWithLifecycle()
    val breakDailyLimit by viewModel.breakDailyLimit.collectAsStateWithLifecycle()
    val breakUsedToday by viewModel.breakUsedToday.collectAsStateWithLifecycle()

    // PIN State
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()

    // PIN Kontrol Diyalogları State
    var showPinVerifyDialog by remember { mutableStateOf(false) }
    var pinVerifyTitle by remember { mutableStateOf("PIN Doğrulaması Gerekli") }
    var pinVerifySubtitle by remember { mutableStateOf("Bu işlemi gerçekleştirmek için 4 haneli PIN kodunuzu girin.") }
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
            14
        }
    }

    // PIN korumalı aksiyon çalıştırma yardımcısı
    fun executeProtectedAction(
        title: String = "PIN Doğrulaması Gerekli",
        subtitle: String = "Bu işlemi gerçekleştirmek için PIN kodunuzu girin.",
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
                    Toast.makeText(context, "APK yükleniyor...", Toast.LENGTH_SHORT).show()
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Logo & Başlık Alanı
            HeaderSection(
                isServiceActive = isServiceActive,
                isBreakActive = isBreakActive
            )

            // 2. Mola / Cooldown Kartı (Yeni Özellik 1)
            BreakCooldownCard(
                isBreakActive = isBreakActive,
                remainingSeconds = breakRemainingSeconds,
                selectedDurationMin = breakDurationMinutes,
                onSelectDuration = { duration ->
                    executeProtectedAction(
                        title = "Mola Süresini Değiştir",
                        subtitle = "Mola varsayılan süresini değiştirmek için PIN girin."
                    ) {
                        viewModel.setBreakDurationMinutes(duration)
                    }
                },
                dailyLimit = breakDailyLimit,
                onSelectDailyLimit = { limit ->
                    executeProtectedAction(
                        title = "Günlük Mola Sınırını Değiştir",
                        subtitle = "Günlük mola sınırını değiştirmek için PIN girin."
                    ) {
                        viewModel.setBreakDailyLimit(limit)
                    }
                },
                usedToday = breakUsedToday,
                canStartBreak = viewModel.canStartBreak(),
                onStartBreak = {
                    executeProtectedAction(
                        title = "Mola Başlat",
                        subtitle = "Geçici mola modunu başlatmak için PIN kodunu girin."
                    ) {
                        val started = viewModel.startBreak()
                        if (!started) {
                            Toast.makeText(context, "Bugünkü mola hakkınız doldu!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onEndBreak = {
                    viewModel.endBreak()
                    Toast.makeText(context, "Mola sonlandırıldı, koruma tekrar devrede.", Toast.LENGTH_SHORT).show()
                }
            )

            // 3. Koruma Durumu ve İzin Rehberi Kartı
            ServiceStatusCard(
                isServiceActive = isServiceActive,
                onOpenAccessibilitySettings = {
                    AccessibilityUtils.openAccessibilitySettings(context)
                },
                onOpenAppDetails = {
                    AccessibilityUtils.openAppDetailsSettings(context)
                },
                onRefresh = {
                    viewModel.refreshServiceStatus()
                }
            )

            // 4. Platform Engelleme Anahtarları & DM Reels İzni (Yeni Özellik 3)
            PlatformTogglesSection(
                isYouTubeEnabled = isYouTubeEnabled,
                onYouTubeToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = "YouTube korumasını kapatmak için PIN girin.") {
                            viewModel.setYouTubeEnabled(false)
                        }
                    } else {
                        viewModel.setYouTubeEnabled(true)
                    }
                },
                isInstagramEnabled = isInstagramEnabled,
                onInstagramToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = "Instagram korumasını kapatmak için PIN girin.") {
                            viewModel.setInstagramEnabled(false)
                        }
                    } else {
                        viewModel.setInstagramEnabled(true)
                    }
                },
                isTikTokEnabled = isTikTokEnabled,
                onTikTokToggle = { enabled ->
                    if (!enabled) {
                        executeProtectedAction(subtitle = "TikTok korumasını kapatmak için PIN girin.") {
                            viewModel.setTikTokEnabled(false)
                        }
                    } else {
                        viewModel.setTikTokEnabled(true)
                    }
                },
                isInstagramDmReelsAllowed = isInstagramDmReelsAllowed,
                onInstagramDmReelsToggle = { allowed ->
                    viewModel.setInstagramDmReelsAllowed(allowed)
                }
            )

            // 5. Ebeveyn / Güvenlik PIN Kilidi Kartı (Yeni Özellik 2)
            PinSecurityCard(
                isPinEnabled = isPinEnabled,
                onTogglePin = { enable ->
                    if (enable) {
                        showPinSetupDialog = true
                    } else {
                        executeProtectedAction(
                            title = "PIN Kilidini Kapat",
                            subtitle = "PIN kilidini devre dışı bırakmak için mevcut PIN'inizi girin."
                        ) {
                            viewModel.disablePin()
                            Toast.makeText(context, "PIN kilidi devre dışı bırakıldı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onChangePin = {
                    executeProtectedAction(
                        title = "PIN Kodunu Değiştir",
                        subtitle = "Yeni bir PIN oluşturmadan önce mevcut PIN kodunuzu doğrulayın."
                    ) {
                        showPinSetupDialog = true
                    }
                }
            )

            // 6. Canlı İstatistikler ve Engelleme Özeti
            StatisticsCard(
                totalCount = totalBlockedCount,
                lastPlatform = lastBlockedPlatform,
                lastTimestamp = lastBlockedTimestamp,
                onResetStats = {
                    executeProtectedAction(subtitle = "İstatistikleri sıfırlamak için PIN girin.") {
                        viewModel.resetStatistics()
                    }
                }
            )

            // 7. Otomatik Güncelleyici
            AutoUpdateCard(
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
                            Toast.makeText(context, "Uygulamanız güncel", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                isCheckingUpdate = isCheckingUpdate
            )

            // 8. Test ve Sorun Giderme Rehberi
            TestingAndTroubleshootingCard()

            Spacer(modifier = Modifier.height(16.dp))
        }

        // PIN Doğrulama Diyaloğu
        if (showPinVerifyDialog) {
            PinVerificationDialog(
                title = pinVerifyTitle,
                subtitle = pinVerifySubtitle,
                onDismiss = { showPinVerifyDialog = false },
                onVerify = { pin ->
                    val result = viewModel.verifyPin(pin)
                    when (result) {
                        is PinManager.VerifyResult.Success -> {
                            showPinVerifyDialog = false
                            onPinVerifiedAction()
                            true
                        }
                        is PinManager.VerifyResult.Wrong -> {
                            Toast.makeText(context, "Hatalı PIN! (${result.attemptsLeft} hak kaldı)", Toast.LENGTH_SHORT).show()
                            false
                        }
                        is PinManager.VerifyResult.LockedOut -> {
                            val sec = result.remainingMs / 1000L
                            Toast.makeText(context, "Çok fazla deneme! $sec sn kilitlendi.", Toast.LENGTH_LONG).show()
                            false
                        }
                    }
                }
            )
        }

        // PIN Belirleme / Değiştirme Diyaloğu
        if (showPinSetupDialog) {
            PinSetupDialog(
                onDismiss = { showPinSetupDialog = false },
                onSavePin = { pin ->
                    val success = viewModel.setPin(pin)
                    if (success) {
                        Toast.makeText(context, "PIN kilidi başarıyla aktif edildi.", Toast.LENGTH_SHORT).show()
                        showPinSetupDialog = false
                    } else {
                        Toast.makeText(context, "PIN 4 haneli rakamlardan oluşmalıdır.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // ZORUNLU GÜNCELLEME EKRANI
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(ElectricBlue, ElectricBlueVariant))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ElectricBlueSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Zorunlu Güncelleme Mevcut",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "ScrollBlock'un kısa videoları eksiksiz engellemeye devam edebilmesi için yeni sürüme (v${info.versionName}) güncellenmesi zorunludur.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        if (info.changelog.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NearBlack)
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "YENİLİKLER:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlueGlow
                                    )
                                    Text(
                                        text = info.changelog,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        if (isDownloadingMandatory) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { mandatoryProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ElectricBlue
                                )
                                Text(
                                    text = "İndiriliyor: %${(mandatoryProgress * 100).toInt()}",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!UpdateManager.canRequestPackageInstalls(context)) {
                                        Toast.makeText(context, "Lütfen bilinmeyen uygulama yükleme izni verin", Toast.LENGTH_SHORT).show()
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
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricBlue,
                                    contentColor = NearBlack
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Şimdi Güncelle ve Kur",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- UI BİLEŞENLERİ ----------------

@Composable
private fun HeaderSection(
    isServiceActive: Boolean,
    isBreakActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppLogoBadge()

            Column {
                Text(
                    text = "ScrollBlock",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Kısa Video Engelleyici • v2.4",
                    fontSize = 12.sp,
                    color = ElectricBlueGlow
                )
            }
        }

        val badgeBackground by animateColorAsState(
            targetValue = when {
                isBreakActive -> InactiveAmberContainer
                isServiceActive -> ActiveGreenContainer
                else -> InactiveAmberContainer
            },
            label = "BadgeBg"
        )
        val badgeText = when {
            isBreakActive -> InactiveAmberText
            isServiceActive -> ActiveGreenText
            else -> InactiveAmberText
        }
        val statusLabel = when {
            isBreakActive -> "MOLA"
            isServiceActive -> "AKTİF"
            else -> "DEVRE DIŞI"
        }
        val statusDotColor = when {
            isBreakActive -> InactiveAmber
            isServiceActive -> ActiveGreen
            else -> InactiveAmber
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(badgeBackground)
                .border(
                    width = 1.dp,
                    color = statusDotColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Text(
                    text = statusLabel,
                    color = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * 1. MOLA / COOLDOWN MODU KARTI (Yeni Özellik 1)
 */
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isBreakActive) {
                    listOf(InactiveAmber.copy(alpha = 0.8f), ElectricBlue.copy(alpha = 0.4f))
                } else {
                    listOf(ElectricBlue.copy(alpha = 0.35f), CardBorder)
                }
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isBreakActive) InactiveAmber.copy(alpha = 0.2f) else ElectricBlue.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBreakActive) Icons.Default.Pause else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isBreakActive) InactiveAmber else ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isBreakActive) "Mola Devam Ediyor" else "Mola / Duraklatma Modu",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBreakActive) "Engelleme geçici olarak kapalı" else "Geçici süre engellemeyi kapatın",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (isBreakActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(InactiveAmberContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "GEÇİCİ SERBEST",
                            color = InactiveAmberText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isBreakActive) {
                // Aktif Geri Sayım Ekranı
                val min = remainingSeconds / 60
                val sec = remainingSeconds % 60
                val timeString = String.format("%02d:%02d", min, sec)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NearBlack)
                        .border(1.dp, InactiveAmber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "KALAN SÜRE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = InactiveAmberText,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = timeString,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = "Süre bittiğinde koruma otomatik olarak tekrar açılır.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onEndBreak,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InactiveAmber,
                        contentColor = NearBlack
                    )
                ) {
                    Text(
                        text = "Molayı Şimdi Bitir (Korumayı Aç)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Mola Başlatma Paneli
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "MOLA SÜRESİ:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlueGlow
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
                                    .background(if (isSelected) ElectricBlue else CardBackgroundElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ElectricBlue else CardBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectDuration(duration) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$duration dk",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NearBlack else TextPrimary
                                )
                            }
                        }
                    }

                    // Günlük Limit Bilgisi ve Seçimi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GÜNLÜK SINIR:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlueGlow
                        )
                        val limitLabel = if (dailyLimit == 0) "Sınırsız" else "Günde $dailyLimit kez"
                        Text(
                            text = if (dailyLimit == 0) "$limitLabel (Kullanılan: $usedToday)" else "$limitLabel (Kalan: ${(dailyLimit - usedToday).coerceAtLeast(0)})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

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
                                    .background(if (isSelected) ElectricBlueVariant.copy(alpha = 0.3f) else CardBackgroundElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ElectricBlue else CardBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectDailyLimit(limit) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ElectricBlue else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onStartBreak,
                        enabled = canStartBreak,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = NearBlack,
                            disabledContainerColor = CardBackgroundElevated,
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (canStartBreak) "$selectedDurationMin Dakika Mola Başlat" else "Bugünkü Mola Hakkı Doldu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. EBEVEYN / GÜVENLİK PIN KİLİDİ KARTI (Yeni Özellik 2)
 */
@Composable
private fun PinSecurityCard(
    isPinEnabled: Boolean,
    onTogglePin: (Boolean) -> Unit,
    onChangePin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isPinEnabled) {
                    listOf(ActiveGreen.copy(alpha = 0.4f), CardBorder)
                } else {
                    listOf(CardBorder, CardBorder)
                }
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isPinEnabled) ActiveGreen.copy(alpha = 0.2f) else CardBackgroundElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPinEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPinEnabled) ActiveGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = "Ebeveyn / Güvenlik PIN Kilidi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isPinEnabled) "Korumayı kapatma ve mola PIN ile kilitli" else "Korumayı ve ayarları 4 haneli PIN ile kilitleyin",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = isPinEnabled,
                    onCheckedChange = onTogglePin,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NearBlack,
                        checkedTrackColor = ActiveGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardBackgroundElevated
                    )
                )
            }

            AnimatedVisibility(visible = isPinEnabled) {
                OutlinedButton(
                    onClick = onChangePin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlueGlow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "PIN Kodunu Değiştir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * 3. PLATFORM AYARLARI VE DM REELS İZNİ (Yeni Özellik 3)
 */
@Composable
private fun PlatformTogglesSection(
    isYouTubeEnabled: Boolean,
    onYouTubeToggle: (Boolean) -> Unit,
    isInstagramEnabled: Boolean,
    onInstagramToggle: (Boolean) -> Unit,
    isTikTokEnabled: Boolean,
    onTikTokToggle: (Boolean) -> Unit,
    isInstagramDmReelsAllowed: Boolean,
    onInstagramDmReelsToggle: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PLATFORM KORUMA AYARLARI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricBlueGlow,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        PlatformItem(
            name = "YouTube Shorts",
            subtitle = "Shorts açıldığı an geri çıkar. Normal YouTube videoları, arama ve yorumlar aynen çalışır.",
            icon = Icons.Default.PlayArrow,
            iconTint = Color(0xFFFF4444),
            isEnabled = isYouTubeEnabled,
            onToggle = onYouTubeToggle
        )

        // Instagram Reels Kartı + DM Reels İzni Alt Öğesi
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    if (isInstagramEnabled) {
                        listOf(ElectricBlueVariant.copy(alpha = 0.35f), CardBorder)
                    } else {
                        listOf(CardBorder, CardBorder)
                    }
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE1306C).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Instagram Reels",
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = "Instagram Reels",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Reels sekmesini ve keşfet videolarını kapatır. Ana akış, hikayeler ve DM serbest kalır.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Switch(
                        checked = isInstagramEnabled,
                        onCheckedChange = onInstagramToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NearBlack,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CardBackgroundElevated
                        )
                    )
                }

                // DM'den Gelen Reels İzni (Yeni Özellik 3)
                AnimatedVisibility(visible = isInstagramEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBackgroundElevated)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "DM'den Gelen Reels İzni (Tek Video)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isInstagramDmReelsAllowed) ElectricBlueGlow else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Arkadaşınızın attığı İLK videoyu izlemenize izin verilir. Aşağı kaydırıp (scroll) ikinci videoya geçmeye çalıştığınız an kapatılır.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }

                            Switch(
                                checked = isInstagramDmReelsAllowed,
                                onCheckedChange = onInstagramDmReelsToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NearBlack,
                                    checkedTrackColor = ElectricBlue,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = NearBlack
                                )
                            )
                        }
                    }
                }
            }
        }

        PlatformItem(
            name = "TikTok Akışı",
            subtitle = "'Sizin İçin' ve ana video akışını durdurur. Gelen Kutusu, Mesajlar ve Profil açık kalır.",
            icon = Icons.Default.PlayArrow,
            iconTint = Color(0xFF00F2FE),
            isEnabled = isTikTokEnabled,
            onToggle = onTikTokToggle
        )
    }
}

@Composable
private fun PlatformItem(
    name: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isEnabled) {
                    listOf(ElectricBlueVariant.copy(alpha = 0.35f), CardBorder)
                } else {
                    listOf(CardBorder, CardBorder)
                }
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NearBlack,
                    checkedTrackColor = ElectricBlue,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CardBackgroundElevated
                )
            )
        }
    }
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundElevated),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(ElectricBlue, ElectricBlueVariant))
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElectricBlueSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
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
                    placeholder = { Text("••••", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    modifier = Modifier.width(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = CardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "İptal", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (enteredPin.length == 4) {
                                onVerify(enteredPin)
                            }
                        },
                        enabled = enteredPin.length == 4,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = NearBlack
                        )
                    ) {
                        Text(text = "Onayla", fontWeight = FontWeight.Bold)
                    }
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundElevated),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(ActiveGreen, ElectricBlue))
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ActiveGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ActiveGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Yeni 4 Haneli PIN Belirleyin",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Bu PIN, korumayı kapatırken ve mola başlatırken istenecektir.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it
                        errorMessage = null
                    },
                    label = { Text("4 Haneli PIN", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = CardBorder
                    )
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it
                        errorMessage = null
                    },
                    label = { Text("PIN Kodunu Tekrar Girin", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = CardBorder
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "İptal", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (!PinHasher.isValidFormat(pin)) {
                                errorMessage = "PIN tam 4 haneli rakam olmalıdır."
                            } else if (pin != confirmPin) {
                                errorMessage = "PIN kodları birbiriyle eşleşmiyor!"
                            } else {
                                onSavePin(pin)
                            }
                        },
                        enabled = pin.length == 4 && confirmPin.length == 4,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ActiveGreen,
                            contentColor = NearBlack
                        )
                    ) {
                        Text(text = "Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- DİĞER KARTLAR ----------------

@Composable
private fun AppLogoBadge() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(ElectricBlueSubtle, CardBackgroundElevated)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(ElectricBlue, ElectricBlueVariant)),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val w = size.width
            val h = size.height

            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.1f)
                lineTo(w * 0.88f, h * 0.28f)
                cubicTo(w * 0.88f, h * 0.65f, w * 0.5f, h * 0.92f, w * 0.5f, h * 0.92f)
                cubicTo(w * 0.5f, h * 0.92f, w * 0.12f, h * 0.65f, w * 0.12f, h * 0.28f)
                close()
            }

            drawPath(
                path = shieldPath,
                color = ElectricBlue.copy(alpha = 0.25f)
            )
            drawPath(
                path = shieldPath,
                color = ElectricBlue,
                style = Stroke(width = 3.dp.toPx())
            )

            drawLine(
                color = Color.White,
                start = Offset(w * 0.32f, h * 0.70f),
                end = Offset(w * 0.68f, h * 0.32f),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

@Composable
private fun ServiceStatusCard(
    isServiceActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isServiceActive) {
                    listOf(ActiveGreen.copy(alpha = 0.4f), CardBorder)
                } else {
                    listOf(InactiveAmber.copy(alpha = 0.8f), CardBorder)
                }
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isServiceActive) ActiveGreen else InactiveAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isServiceActive) "Koruma Çalışıyor" else "Erişilebilirlik İzni Gerekli",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onRefresh,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlueGlow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Yenile",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Kontrol Et", fontSize = 12.sp)
                }
            }

            if (!isServiceActive) {
                Text(
                    text = "ScrollBlock'un kısa videoları (Shorts, Reels, TikTok) anında algılayıp geri çıkabilmesi için Android Erişilebilirlik Servisi iznine ihtiyacı vardır.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(InactiveAmberContainer.copy(alpha = 0.4f))
                        .border(1.dp, InactiveAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "⚠️ \"Erişim Reddedildi / Kısıtlı Ayar\" Çözümü:",
                            color = InactiveAmberText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android 13+ güvenliği için dışarıdan kurulan uygulamalarda izinleri kilitler:\n" +
                                "1. Aşağıdaki 'Kısıtlı Ayar Kilidini Aç' butonuna bas.\n" +
                                "2. Açılan sayfada sağ üstteki 3 noktaya (⋮) dokun.\n" +
                                "3. 'Kısıtlı ayarlara izin ver' seçeneğini onayla.\n" +
                                "4. Ardından Erişilebilirlik servisini kolayca açabilirsin.",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onOpenAppDetails,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InactiveAmber,
                                contentColor = NearBlack
                            )
                        ) {
                            Text(
                                text = "1. Adım: Kısıtlı Ayar Kilidini Aç (Uygulama Bilgisi)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = NearBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. Adım: Erişilebilirlik Ayarlarına Git",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                Text(
                    text = "ScrollBlock arka planda koruma sağlıyor. Kısa video akışına girdiğinizde hiçbir uyarı çıkarmadan sizi anında önceki ekrana geri fırlatır.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatisticsCard(
    totalCount: Int,
    lastPlatform: String,
    lastTimestamp: Long,
    onResetStats: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CardBorder, CardBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ENGELLEME ETKİNLİĞİ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlueGlow,
                    letterSpacing = 1.sp
                )

                if (totalCount > 0) {
                    OutlinedButton(
                        onClick = onResetStats,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Text(text = "Sıfırla", fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$totalCount",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricBlue,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Kısa Video Akışı Durduruldu",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (lastPlatform.isNotEmpty() && lastTimestamp > 0L) {
                    val relativeTime = DateUtils.getRelativeTimeSpanString(
                        lastTimestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBackgroundElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Son: $lastPlatform",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = relativeTime.toString(),
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoUpdateCard(
    onPickLocalApk: () -> Unit,
    onCheckUpdate: () -> Unit,
    isCheckingUpdate: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var downloadUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(ElectricBlue.copy(alpha = 0.3f), CardBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Uygulama Güncelleyici",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Text(
                text = "Drive veya tarayıcı karmaşası olmadan, yeni çıkan APK'yı doğrudan bu uygulama içinden tek tıkla kurabilirsiniz.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            OutlinedButton(
                onClick = onCheckUpdate,
                enabled = !isCheckingUpdate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlueGlow)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCheckingUpdate) "Kontrol Ediliyor..." else "Güncellemeleri Şimdi Kontrol Et",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }

            Button(
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
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = NearBlack
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GitHub'dan En Son Sürümü İndir ve Güncelle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = onPickLocalApk,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlueGlow)
            ) {
                Text(
                    text = "Veya İndirilen APK Dosyasını Seçip Kur",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
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
                    placeholder = { Text("Doğrudan APK indirme linki...", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue,
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
                        } else {
                            Toast.makeText(context, "Lütfen bir link girin", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = NearBlack
                    )
                ) {
                    Text("İndir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (isDownloading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = ElectricBlue,
                        trackColor = CardBackgroundElevated
                    )
                    Text(
                        text = "İndiriliyor: %${(downloadProgress * 100).toInt()}",
                        fontSize = 11.sp,
                        color = ElectricBlueGlow
                    )
                }
            }
        }
    }
}

@Composable
private fun TestingAndTroubleshootingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CardBorder, CardBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ElectricBlueGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Test ve Güvenlik Bilgileri",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Text(
                text = "• Mola Modu: Geçici serbestlik sağlar. Süre dolduğunda hiçbir müdahaleye gerek kalmadan engelleme otomatik olarak tekrar devreye girer.\n" +
                    "• DM Reels İzni: Arkadaşınızın gönderdiği videoyu izlemenize izin verir; bir sonraki videoya kaydırdığınız an algılanıp geri fırlatılır.\n" +
                    "• Ebeveyn PIN Kilidi: Korumayı kapatmaya veya mola başlatmaya çalışırken 4 haneli PIN sorulur.\n" +
                    "• Gizlilik ve Güvenlik: PIN kodu tuzlanmış (salted) PBKDF2 ile saklanır. Mesajlarınız veya kişisel verileriniz asla kaydedilmez.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

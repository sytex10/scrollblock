package com.example.scrollblock.ui.main

import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val totalBlockedCount by viewModel.totalBlockedCount.collectAsStateWithLifecycle()
    val lastBlockedPlatform by viewModel.lastBlockedPlatform.collectAsStateWithLifecycle()
    val lastBlockedTimestamp by viewModel.lastBlockedTimestamp.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // APK Dosya Seçici (İndirilen APK ile Doğrudan Güncelleme)
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
            HeaderSection(isServiceActive = isServiceActive)

            // 2. Koruma Durumu ve İzin Rehberi Kartı
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

            // 3. Platform Engelleme Anahtarları
            PlatformTogglesSection(
                isYouTubeEnabled = isYouTubeEnabled,
                onYouTubeToggle = { viewModel.setYouTubeEnabled(it) },
                isInstagramEnabled = isInstagramEnabled,
                onInstagramToggle = { viewModel.setInstagramEnabled(it) },
                isTikTokEnabled = isTikTokEnabled,
                onTikTokToggle = { viewModel.setTikTokEnabled(it) }
            )

            // 4. Canlı İstatistikler ve Engelleme Özeti
            StatisticsCard(
                totalCount = totalBlockedCount,
                lastPlatform = lastBlockedPlatform,
                lastTimestamp = lastBlockedTimestamp,
                onResetStats = { viewModel.resetStatistics() }
            )

            // 5. Otomatik Güncelleyici (Yerel APK veya Link)
            AutoUpdateCard(
                onPickLocalApk = {
                    apkPickerLauncher.launch("application/vnd.android.package-archive")
                }
            )

            // 6. Test ve Sorun Giderme Rehberi
            TestingAndTroubleshootingCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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
private fun HeaderSection(isServiceActive: Boolean) {
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
                    text = "Kısa Video Engelleyici • v1.5",
                    fontSize = 12.sp,
                    color = ElectricBlueGlow
                )
            }
        }

        val badgeBackground by animateColorAsState(
            targetValue = if (isServiceActive) ActiveGreenContainer else InactiveAmberContainer,
            label = "BadgeBg"
        )
        val badgeText = if (isServiceActive) ActiveGreenText else InactiveAmberText
        val statusLabel = if (isServiceActive) "AKTİF" else "DEVRE DIŞI"

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(badgeBackground)
                .border(
                    width = 1.dp,
                    color = if (isServiceActive) ActiveGreen.copy(alpha = 0.5f) else InactiveAmber.copy(alpha = 0.5f),
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
                        .background(if (isServiceActive) ActiveGreen else InactiveAmber)
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
private fun PlatformTogglesSection(
    isYouTubeEnabled: Boolean,
    onYouTubeToggle: (Boolean) -> Unit,
    isInstagramEnabled: Boolean,
    onInstagramToggle: (Boolean) -> Unit,
    isTikTokEnabled: Boolean,
    onTikTokToggle: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PLATFORM AYARLARI",
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

        PlatformItem(
            name = "Instagram Reels",
            subtitle = "Reels izleyicisi açıldığı an geri çıkar. Ana sayfa akışı, hikayeler, DM ve profiller etkilenmez.",
            icon = Icons.Default.PlayArrow,
            iconTint = Color(0xFFE1306C),
            isEnabled = isInstagramEnabled,
            onToggle = onInstagramToggle
        )

        PlatformItem(
            name = "TikTok Akışı",
            subtitle = "'Sizin İçin' ve ana video akışını kapatır. Gelen Kutusu (mesajlar/sohbet) ve Profil ekranı açık kalır.",
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
    onPickLocalApk: () -> Unit
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

            // Yöntem 1: GitHub'dan Doğrudan Tek Tıkla Güncelle
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

            // Yöntem 2: İndirilen APK ile Güncelle
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

            // Yöntem 2: Doğrudan URL ile İndirip Güncelle
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
                text = "• Nasıl Test Edilir: Yukarıdaki durum 'AKTİF' iken YouTube Shorts, Instagram Reels veya TikTok ana akışını açın. Ekranda hiçbir uyarı çıkmadan anında bir önceki sayfaya atacaktır.\n" +
                    "• TikTok Gelen Kutusu Koruması: TikTok'ta Gelen Kutusu (sohbet/mesajlar) ve Profil ekranı engellenmez, yalnızca 'Sizin İçin' video akışı durdurulur.\n" +
                    "• Akıllı Döngü Koruması: Sayfadan çıkış esnasında ekrana peş peşe basılmasını ve takılmaları önlemek için 650 ms koruma filtresi uygulanır.\n" +
                    "• Gizlilik: Ekran görüntüsü veya mesajlarınız asla kaydedilmez.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

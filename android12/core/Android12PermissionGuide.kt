package com.example.scrollblock.android12.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android 12 ve Altı (Build.VERSION.SDK_INT <= 32) İçin İzole İzin ve Kurulum Rehberi.
 *
 * Android 12 ve altında Android 13'ün "Kısıtlı Ayarlar (Restricted Settings)" kilidi
 * bulunmadığından kullanıcıya "Uygulama Bilgisi -> Sağ üst ⋮ -> Kısıtlı ayarlara izin ver"
 * adımları hiç gösterilmez; doğrudan tek butonla Erişilebilirlik Ayarları açılır.
 */
object Android12PermissionGuide {

    fun isAndroid12OrBelow(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 // API 32 (Android 12L) ve altı
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    @Composable
    fun MainScreenWarningCard(onOpenAccessibility: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E130F)),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Erişilebilirlik Servisi Kapalı",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Korumayı başlatmak için tek adımda aktif edin.",
                            color = Color(0xFFFCD34D),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "1. Aşağıdaki butona dokunun.\n" +
                        "2. İndirilen Uygulamalar / Hizmetler listesinden ShortBan'ı seçip anahtarı açın.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onOpenAccessibility,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = Color(0xFF0A0D12),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Erişilebilirlik Ayarlarını Aç",
                        color = Color(0xFF0A0D12),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    @Composable
    fun OnboardingStepsCard(
        isServiceEnabled: Boolean,
        onOpenAccessibility: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Hızlı Kurulum (Android 12 ve Altı)",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cihazınızda ek kısıtlı ayar kilidi bulunmamaktadır. Doğrudan Erişilebilirlik menüsünden ShortBan'ı açabilirsiniz.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                StepItem(
                    number = "1",
                    title = "Erişilebilirlik Menüsünü Açın",
                    description = "Aşağıdaki butona basarak telefonunuzun Erişilebilirlik ayarlarına gidin."
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepItem(
                    number = "2",
                    title = "ShortBan'ı Aktif Edin",
                    description = "'İndirilen Uygulamalar' veya 'Yüklü Hizmetler' altından ShortBan'ı bulup anahtarı açık konuma getirin."
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onOpenAccessibility,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceEnabled) Color(0xFF10B981) else Color(0xFFE11D48)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceEnabled) Icons.Default.CheckCircle else Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceEnabled) "Erişilebilirlik Aktif" else "Erişilebilirlik Ayarlarını Aç",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    @Composable
    private fun StepItem(number: String, title: String, description: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFE11D48).copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = Color(0xFFFB7185),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}


package com.example.scrollblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scrollblock.theme.NearBlack
import com.example.scrollblock.theme.ScrollBlockTheme
import com.example.scrollblock.ui.main.MainScreen
import com.example.scrollblock.ui.main.MainScreenViewModel
import com.example.scrollblock.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Üst durum çubuğu (saat, şarj) ve alt çubuk simgelerini açık renk (beyaz) yap
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            ScrollBlockTheme {
                val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
                val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()

                // Tüm ekranı (status bar ve navigation bar dahil) NearBlack (#0A0A0B) ile doldurur
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NearBlack
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        Crossfade(
                            targetState = hasCompletedOnboarding,
                            label = "screen_crossfade"
                        ) { completed ->
                            if (!completed) {
                                OnboardingScreen(
                                    isServiceActive = isServiceActive,
                                    onComplete = { viewModel.completeOnboarding() }
                                )
                            } else {
                                MainScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check accessibility service status when user returns from Android Settings
        viewModel.refreshServiceStatus()
    }
}

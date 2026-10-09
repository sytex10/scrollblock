package com.example.scrollblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.scrollblock.theme.NearBlack
import com.example.scrollblock.theme.ScrollBlockTheme
import com.example.scrollblock.ui.main.MainScreen
import com.example.scrollblock.ui.main.MainScreenViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScrollBlockTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = NearBlack
                ) {
                    MainScreen(viewModel = viewModel)
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

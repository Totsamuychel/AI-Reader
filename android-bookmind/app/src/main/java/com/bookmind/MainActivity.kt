package com.bookmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmind.ui.BookMindNavHost
import com.bookmind.ui.screens.SplashOverlay
import com.bookmind.ui.settings.SettingsViewModel
import com.bookmind.ui.theme.AppTheme
import com.bookmind.ui.theme.BookMindTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ system splash; we hand off immediately to an animated
        // in-app reveal so branding looks consistent across versions.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            var showSplash by remember { mutableStateOf(true) }

            BookMindTheme(
                appTheme = settings.theme,
                dynamicColor = settings.dynamicColor && settings.theme == AppTheme.SYSTEM
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BookMindNavHost(settingsViewModel = settingsViewModel)
                    if (showSplash) {
                        SplashOverlay(onFinished = { showSplash = false })
                    }
                }
            }
        }
    }
}

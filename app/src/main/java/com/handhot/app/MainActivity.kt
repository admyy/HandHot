package com.handhot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.handhot.app.ui.main.MainViewModel
import com.handhot.app.ui.navigation.HandHotNavHost
import com.handhot.app.ui.settings.CleanupWorker
import com.handhot.app.ui.settings.SettingsViewModel
import com.handhot.app.ui.theme.HandHotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic cleanup
        CleanupWorker.schedule(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val mainViewModel: MainViewModel = hiltViewModel()

            HandHotTheme(
                darkThemeOverride = settingsViewModel.settingsState.value.darkMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    HandHotNavHost(
                        navController = navController,
                        mainViewModel = mainViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

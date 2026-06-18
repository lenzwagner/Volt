package com.lenz.tennisapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lenz.tennisapp.ui.navigation.AppNavigation
import com.lenz.tennisapp.ui.startup.StartupViewModel
import com.lenz.tennisapp.ui.theme.TennisTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val startupViewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Hold the splash screen until today's matches are refreshed
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !startupViewModel.isReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateTo = intent.getStringExtra("navigate_to")
        val matchId = intent.getStringExtra("matchId")

        setContent {
            TennisTheme {
                AppNavigation(navigateTo, matchId)
            }
        }
    }
}

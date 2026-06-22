package com.lenz.tennisapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lenz.tennisapp.ui.navigation.AppNavigation
import com.lenz.tennisapp.ui.theme.TennisTheme
import com.lenz.tennisapp.ui.update.UpdateGate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateTo = intent.getStringExtra("navigate_to")
        val matchId = intent.getStringExtra("matchId")

        setContent {
            TennisTheme {
                AppNavigation(navigateTo, matchId)
                UpdateGate()
            }
        }
    }
}

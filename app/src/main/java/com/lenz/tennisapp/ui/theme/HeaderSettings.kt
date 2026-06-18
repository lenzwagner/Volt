package com.lenz.tennisapp.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object HeaderSettings {
    private const val PREFS_NAME = "TennisAppSettings"
    private const val BLUR_RADIUS_KEY = "blur_radius"
    private const val GRADIENT_INTENSITY_KEY = "gradient_intensity"
    private const val GRADIENT_HEIGHT_KEY = "gradient_height"

    fun getBlurRadius(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(BLUR_RADIUS_KEY, 20f)
    }

    fun getGradientIntensity(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(GRADIENT_INTENSITY_KEY, 0.6f)
    }

    fun getGradientHeight(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(GRADIENT_HEIGHT_KEY, 0.25f)
    }
}

@Composable
fun rememberHeaderSettings(): Triple<Float, Float, Float> {
    val context = LocalContext.current
    return remember {
        Triple(
            HeaderSettings.getBlurRadius(context),
            HeaderSettings.getGradientIntensity(context),
            HeaderSettings.getGradientHeight(context)
        )
    }
}

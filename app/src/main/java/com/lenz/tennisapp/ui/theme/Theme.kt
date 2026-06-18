package com.lenz.tennisapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary                = TennisGreen,
    onPrimary              = Color.White,
    primaryContainer       = TennisGreenContainer,
    onPrimaryContainer     = OnTennisGreenContainer,
    secondary              = TennisOrange,
    onSecondary            = Color.White,
    secondaryContainer     = TennisOrangeContainer,
    onSecondaryContainer   = OnTennisOrangeContainer,
    tertiary               = TertiaryBlue,
    tertiaryContainer      = TertiaryBlueContainer,
    error                  = ErrorRed,
    errorContainer         = ErrorRedContainer,
    surface                = SurfaceLight,
    surfaceVariant         = SurfaceVariantLight,
    background             = BackgroundLight,
    outline                = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary                = TennisGreenLight,
    onPrimary              = TennisGreenDark,
    primaryContainer       = TennisGreen,
    onPrimaryContainer     = TennisGreenContainer,
    secondary              = TennisOrangeLight,
    onSecondary            = OnTennisOrangeContainer,
    secondaryContainer     = TennisOrange,
    surface                = SurfaceDark,
    surfaceVariant         = SurfaceVariantDark,
    background             = BackgroundDark,
)

@Composable
fun TennisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color — we want our tennis-green brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // never reached with default dynamicColor = false
            if (darkTheme) dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
            else dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = TennisTypography,
        shapes      = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small      = RoundedCornerShape(12.dp),
            medium     = RoundedCornerShape(20.dp),
            large      = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        ),
        content = content
    )
}

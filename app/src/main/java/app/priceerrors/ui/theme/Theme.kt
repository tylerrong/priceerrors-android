package app.priceerrors.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun PriceErrorsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: PaletteOption = PaletteOption.MINT,
    content: @Composable () -> Unit,
) {
    val lightColorScheme = lightColorScheme(
        primary = palette.primaryColor,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = palette.tintColor,
        onPrimaryContainer = AppDark,
        secondary = palette.secondaryColor,
        onSecondary = AppDark,
        tertiary = palette.accentColor,
        background = BackgroundLight,
        onBackground = AppDark,
        surface = SurfaceLight,
        onSurface = AppDark,
        error = NotWorkingRed,
    )
    val darkColorScheme = darkColorScheme(
        primary = palette.primaryColor,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = DarkMintContainer,
        onPrimaryContainer = SurfaceLight,
        secondary = palette.secondaryColor,
        tertiary = palette.accentColor,
        background = BackgroundDark,
        onBackground = SurfaceLight,
        surface = SurfaceDark,
        onSurface = SurfaceLight,
        error = NotWorkingRed,
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme else lightColorScheme,
        typography = PriceErrorsTypography,
        content = content,
    )
}

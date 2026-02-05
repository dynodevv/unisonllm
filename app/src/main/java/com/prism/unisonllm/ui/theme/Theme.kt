package com.prism.unisonllm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.serialization.Serializable

/**
 * Theme settings data class to control theme preferences.
 * Supports Material You dynamic colors, theme mode, and contrast settings.
 */
@Serializable
data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val useHighContrast: Boolean = false
)

/**
 * Enum representing available theme modes.
 */
@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Light color scheme with Material 3 Expressive colors
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Purple100,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    secondary = Secondary40,
    onSecondary = Secondary100,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = Tertiary100,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    error = Error40,
    onError = Error100,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Purple80,
    surfaceTint = Purple40,
    scrim = Color.Black.copy(alpha = 0.32f)
)

/**
 * Dark color scheme with Material 3 Expressive colors
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Purple20,
    primaryContainer = Purple30,
    onPrimaryContainer = Purple90,
    secondary = Secondary80,
    onSecondary = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary20,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Purple40,
    surfaceTint = Purple80,
    scrim = Color.Black.copy(alpha = 0.32f)
)

/**
 * High contrast light color scheme for better accessibility
 */
private val HighContrastLightColorScheme = lightColorScheme(
    primary = Purple30,
    onPrimary = Purple100,
    primaryContainer = Purple80,
    onPrimaryContainer = Purple10,
    secondary = Secondary30,
    onSecondary = Secondary100,
    secondaryContainer = Secondary80,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary30,
    onTertiary = Tertiary100,
    tertiaryContainer = Tertiary80,
    onTertiaryContainer = Tertiary10,
    error = Error30,
    onError = Error100,
    errorContainer = Error80,
    onErrorContainer = Error10,
    background = Neutral100,
    onBackground = Neutral10,
    surface = Neutral100,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant20,
    outline = NeutralVariant40,
    outlineVariant = NeutralVariant70,
    inverseSurface = Neutral10,
    inverseOnSurface = Neutral99,
    inversePrimary = Purple90,
    surfaceTint = Purple30,
    scrim = Color.Black.copy(alpha = 0.5f)
)

/**
 * High contrast dark color scheme for better accessibility
 */
private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Purple90,
    onPrimary = Purple10,
    primaryContainer = Purple40,
    onPrimaryContainer = Purple99,
    secondary = Secondary90,
    onSecondary = Secondary10,
    secondaryContainer = Secondary40,
    onSecondaryContainer = Secondary99,
    tertiary = Tertiary90,
    onTertiary = Tertiary10,
    tertiaryContainer = Tertiary40,
    onTertiaryContainer = Tertiary99,
    error = Error90,
    onError = Error10,
    errorContainer = Error40,
    onErrorContainer = Error99,
    background = Neutral10,
    onBackground = Neutral99,
    surface = Neutral10,
    onSurface = Neutral99,
    surfaceVariant = NeutralVariant20,
    onSurfaceVariant = NeutralVariant90,
    outline = NeutralVariant70,
    outlineVariant = NeutralVariant40,
    inverseSurface = Neutral99,
    inverseOnSurface = Neutral10,
    inversePrimary = Purple30,
    surfaceTint = Purple90,
    scrim = Color.Black.copy(alpha = 0.5f)
)

/**
 * Material 3 Expressive Typography with high-contrast headlines
 */
val ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Material 3 Expressive Shapes with extra-rounded corners
 * Following M3 Expressive guidelines with up to 28dp for cards
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Extended color properties for provider-specific and semantic colors
 */
@Stable
data class ExtendedColors(
    val providerOpenAI: Color = ProviderColors.OpenAI,
    val providerAnthropic: Color = ProviderColors.Anthropic,
    val providerGoogle: Color = ProviderColors.Google,
    val providerOpenRouter: Color = ProviderColors.OpenRouter,
    val providerCustom: Color = ProviderColors.Custom,
    val roleUser: Color = RoleColors.User,
    val roleAssistant: Color = RoleColors.Assistant,
    val roleSystem: Color = RoleColors.System,
    val statusSuccess: Color = StatusColors.Success,
    val statusWarning: Color = StatusColors.Warning,
    val statusError: Color = StatusColors.Error,
    val statusInfo: Color = StatusColors.Info
)

/**
 * CompositionLocal for accessing extended colors
 */
val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

/**
 * Main theme composable for UnisonLLM
 *
 * @param themeSettings The current theme settings
 * @param content The composable content to be themed
 */
@Composable
fun UnisonLLMTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = getColorScheme(
        isDarkTheme = isDarkTheme,
        useDynamicColor = themeSettings.useDynamicColor,
        useHighContrast = themeSettings.useHighContrast
    )

    // Update system UI colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}

/**
 * Get the appropriate color scheme based on settings
 */
@Composable
private fun getColorScheme(
    isDarkTheme: Boolean,
    useDynamicColor: Boolean,
    useHighContrast: Boolean
): ColorScheme {
    val context = LocalContext.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    return when {
        // Dynamic color (Material You) on supported devices
        useDynamicColor && supportsDynamicColor -> {
            if (isDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // High contrast mode
        useHighContrast -> {
            if (isDarkTheme) {
                HighContrastDarkColorScheme
            } else {
                HighContrastLightColorScheme
            }
        }
        // Default schemes
        else -> {
            if (isDarkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }
}

/**
 * Helper extension to access extended colors
 */
object UnisonLLMTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

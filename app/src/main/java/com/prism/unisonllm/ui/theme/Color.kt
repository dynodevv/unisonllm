package com.prism.unisonllm.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * UnisonLLM Color Palette
 *
 * Material 3 Expressive Design color tokens.
 * These colors provide both light and dark theme variants with high contrast
 * support for better accessibility and visual impact.
 */

// Primary tones - Deep Purple for brand identity
val Purple10 = Color(0xFF21005D)
val Purple20 = Color(0xFF381E72)
val Purple30 = Color(0xFF4F378B)
val Purple40 = Color(0xFF6750A4)
val Purple50 = Color(0xFF7F67BE)
val Purple60 = Color(0xFF9A82DB)
val Purple70 = Color(0xFFB69DF8)
val Purple80 = Color(0xFFD0BCFF)
val Purple90 = Color(0xFFEADDFF)
val Purple95 = Color(0xFFF6EDFF)
val Purple99 = Color(0xFFFFFBFE)
val Purple100 = Color(0xFFFFFFFF)

// Secondary tones - Complementary purple shades
val Secondary10 = Color(0xFF1D192B)
val Secondary20 = Color(0xFF332D41)
val Secondary30 = Color(0xFF4A4458)
val Secondary40 = Color(0xFF625B71)
val Secondary50 = Color(0xFF7B7589)
val Secondary60 = Color(0xFF958FA3)
val Secondary70 = Color(0xFFB0A9BD)
val Secondary80 = Color(0xFFCCC2DC)
val Secondary90 = Color(0xFFE8DEF8)
val Secondary95 = Color(0xFFF6EDFF)
val Secondary99 = Color(0xFFFFFBFE)
val Secondary100 = Color(0xFFFFFFFF)

// Tertiary tones - Pink accents for expressive highlights
val Tertiary10 = Color(0xFF31111D)
val Tertiary20 = Color(0xFF492532)
val Tertiary30 = Color(0xFF633B48)
val Tertiary40 = Color(0xFF7D5260)
val Tertiary50 = Color(0xFF986977)
val Tertiary60 = Color(0xFFB58392)
val Tertiary70 = Color(0xFFD29DAC)
val Tertiary80 = Color(0xFFEFB8C8)
val Tertiary90 = Color(0xFFFFD8E4)
val Tertiary95 = Color(0xFFFFECF1)
val Tertiary99 = Color(0xFFFFFBFA)
val Tertiary100 = Color(0xFFFFFFFF)

// Error tones
val Error10 = Color(0xFF410E0B)
val Error20 = Color(0xFF601410)
val Error30 = Color(0xFF8C1D18)
val Error40 = Color(0xFFB3261E)
val Error50 = Color(0xFFDC362E)
val Error60 = Color(0xFFE46962)
val Error70 = Color(0xFFEC928E)
val Error80 = Color(0xFFF2B8B5)
val Error90 = Color(0xFFF9DEDC)
val Error95 = Color(0xFFFCEEEE)
val Error99 = Color(0xFFFFFBF9)
val Error100 = Color(0xFFFFFFFF)

// Neutral tones - Surface and background
val Neutral10 = Color(0xFF1C1B1F)
val Neutral20 = Color(0xFF313033)
val Neutral30 = Color(0xFF48464C)
val Neutral40 = Color(0xFF605D64)
val Neutral50 = Color(0xFF79767D)
val Neutral60 = Color(0xFF938F96)
val Neutral70 = Color(0xFFAEA9B1)
val Neutral80 = Color(0xFFCAC4CF)
val Neutral90 = Color(0xFFE6E1E5)
val Neutral95 = Color(0xFFF4EFF4)
val Neutral99 = Color(0xFFFFFBFE)
val Neutral100 = Color(0xFFFFFFFF)

// Neutral Variant tones - Surface variants and outlines
val NeutralVariant10 = Color(0xFF1D1A22)
val NeutralVariant20 = Color(0xFF322F37)
val NeutralVariant30 = Color(0xFF49454F)
val NeutralVariant40 = Color(0xFF605D66)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant60 = Color(0xFF938F99)
val NeutralVariant70 = Color(0xFFAEA9B4)
val NeutralVariant80 = Color(0xFFCAC4D0)
val NeutralVariant90 = Color(0xFFE7E0EC)
val NeutralVariant95 = Color(0xFFF5EEFA)
val NeutralVariant99 = Color(0xFFFFFBFE)
val NeutralVariant100 = Color(0xFFFFFFFF)

// Provider-specific accent colors for visual differentiation
object ProviderColors {
    val OpenAI = Color(0xFF10A37F)        // OpenAI Green
    val Anthropic = Color(0xFFD4A574)     // Anthropic Tan/Brown
    val Google = Color(0xFF4285F4)         // Google Blue
    val OpenRouter = Color(0xFF6366F1)     // OpenRouter Indigo
    val Custom = Color(0xFF64748B)         // Slate for custom providers
}

// Semantic colors for chat roles
object RoleColors {
    val User = Color(0xFF1E88E5)           // Blue for user messages
    val Assistant = Color(0xFF43A047)       // Green for assistant messages
    val System = Color(0xFFF57C00)          // Orange for system messages
}

// Status colors
object StatusColors {
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
}

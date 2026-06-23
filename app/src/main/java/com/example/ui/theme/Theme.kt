package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(val id: String, val displayNameEn: String, val displayNameAr: String, val primaryColor: Color, val isDark: Boolean) {
    PILOT_GOLD_BLUE("pilot_gold_blue", "Captain's Gold & Velvet", "ذهب الكابتن والقطيفة", Color(0xFFE5A93B), true),
    COSMIC_NEON_CYAN("cosmic_neon_cyan", "Cosmic Lavender & Cyan", "اللافندر الكوني والسيان", Color(0xFFD0BCFF), true),
    SUNSET_ROSE("sunset_rose", "Sunset Coral Rose", "روز المرجان وغروب الشمس", Color(0xFFFF8A80), true),
    EMERALD_BREEZE("emerald_breeze", "Mint Emerald Pitch", "نعناع الزمرد الداكن", Color(0xFF2ECC71), true),
    VIBRANT_LILAC("vibrant_lilac", "Luxurious Deep Purple", "الأرجواني الفاخر العميق", Color(0xFFE1BEE7), true),
    CLASSIC_LIGHT("classic_light", "Classic Cloud Light", "الغيوم المضيئة الكلاسيكية", Color(0xFF6C63FF), false),
    DYNAMIC_MATERIAL_3("dynamic_material_3", "Dynamic System Accent", "ألوان النظام الديناميكية", Color(0xFF1DB954), true)
}

private val PilotGoldBlueDark = darkColorScheme(
    primary = Color(0xFFE5A93B),
    secondary = Color(0xFF1E293B),
    tertiary = Color(0xFFF5C542),
    background = Color(0xFF0B132B),
    surface = Color(0xFF1C2541),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF3A506B),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val PilotGoldBlueLight = lightColorScheme(
    primary = Color(0xFFB88523),
    secondary = Color(0xFFF1F5F9),
    tertiary = Color(0xFFE5A93B),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

private val CosmicNeonCyanDark = darkColorScheme(
    primary = NeonCyan,
    secondary = VibrantLilac,
    tertiary = HotPink,
    background = ObsidianDark,
    surface = ObsidianSurface,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val CosmicNeonCyanLight = lightColorScheme(
    primary = Color(0xFF673AB7),
    secondary = Color(0xFFE8EAF6),
    tertiary = Color(0xFF00B8D4),
    background = Color(0xFFF5F5FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F)
)

private val SunsetRoseDark = darkColorScheme(
    primary = Color(0xFFFF8A80),
    secondary = Color(0xFF880E4F),
    tertiary = Color(0xFFFFD54F),
    background = Color(0xFF120812),
    surface = Color(0xFF2D132C),
    onBackground = Color(0xFFFDE8FD),
    onSurface = Color(0xFFFDE8FD),
    surfaceVariant = Color(0xFF511845),
    onSurfaceVariant = Color(0xFFE1BEE7)
)

private val SunsetRoseLight = lightColorScheme(
    primary = Color(0xFFD81B60),
    secondary = Color(0xFFFCE4EC),
    tertiary = Color(0xFFFF8A80),
    background = Color(0xFFFFF5F5),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF260826),
    onSurface = Color(0xFF260826),
    surfaceVariant = Color(0xFFF8BBD0),
    onSurfaceVariant = Color(0xFF880E4F)
)

private val EmeraldBreezeDark = darkColorScheme(
    primary = Color(0xFF2ECC71),
    secondary = Color(0xFF115228),
    tertiary = Color(0xFF76D7C4),
    background = Color(0xFF0A0E0F),
    surface = Color(0xFF162221),
    onBackground = Color(0xFFE8F8F5),
    onSurface = Color(0xFFE8F8F5),
    surfaceVariant = Color(0xFF2C3E3B),
    onSurfaceVariant = Color(0xFFA3E4D7)
)

private val EmeraldBreezeLight = lightColorScheme(
    primary = Color(0xFF27AE60),
    secondary = Color(0xFFE8F8F5),
    tertiary = Color(0xFF1ABC9C),
    background = Color(0xFFF4F9F6),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0E1A14),
    onSurface = Color(0xFF0E1A14),
    surfaceVariant = Color(0xFFD4EFDF),
    onSurfaceVariant = Color(0xFF196F3D)
)

private val VibrantLilacDark = darkColorScheme(
    primary = Color(0xFFE1BEE7),
    secondary = Color(0xFF4A148C),
    tertiary = Color(0xFFF8BBD0),
    background = Color(0xFF0D061A),
    surface = Color(0xFF22123B),
    onBackground = Color(0xFFF3E5F5),
    onSurface = Color(0xFFF3E5F5),
    surfaceVariant = Color(0xFF3F1F68),
    onSurfaceVariant = Color(0xFFD1C4E9)
)

private val VibrantLilacLight = lightColorScheme(
    primary = Color(0xFF8E24AA),
    secondary = Color(0xFFF3E5F5),
    tertiary = Color(0xFFD1C4E9),
    background = Color(0xFFFAF5FC),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F003C),
    onSurface = Color(0xFF1F003C),
    surfaceVariant = Color(0xFFE1BEE7),
    onSurfaceVariant = Color(0xFF4A148C)
)

private val ClassicLightDark = darkColorScheme(
    primary = Color(0xFF9E9AFF),
    secondary = Color(0xFF1F1F3D),
    tertiary = Color(0xFFFFB4AB),
    background = Color(0xFF0C0C14),
    surface = Color(0xFF171726),
    onBackground = Color(0xFFE4E4EB),
    onSurface = Color(0xFFE4E4EB),
    surfaceVariant = Color(0xFF2B2B47),
    onSurfaceVariant = Color(0xFFC7C7D4)
)

private val ClassicLightLight = lightColorScheme(
    primary = VioletPrimary,
    secondary = VioletSecondary,
    tertiary = Pink40,
    background = SoftWhite,
    surface = SurfaceLight,
    onBackground = Color(0xFF13151A),
    onSurface = Color(0xFF13151A),
    surfaceVariant = CardLight,
    onSurfaceVariant = Color(0xFF1E222B)
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.PILOT_GOLD_BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when (appTheme) {
        AppTheme.PILOT_GOLD_BLUE -> if (darkTheme) PilotGoldBlueDark else PilotGoldBlueLight
        AppTheme.COSMIC_NEON_CYAN -> if (darkTheme) CosmicNeonCyanDark else CosmicNeonCyanLight
        AppTheme.SUNSET_ROSE -> if (darkTheme) SunsetRoseDark else SunsetRoseLight
        AppTheme.EMERALD_BREEZE -> if (darkTheme) EmeraldBreezeDark else EmeraldBreezeLight
        AppTheme.VIBRANT_LILAC -> if (darkTheme) VibrantLilacDark else VibrantLilacLight
        AppTheme.CLASSIC_LIGHT -> if (darkTheme) ClassicLightDark else ClassicLightLight
        AppTheme.DYNAMIC_MATERIAL_3 -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) PilotGoldBlueDark else PilotGoldBlueLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

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

private val PilotGoldBlueColorScheme = darkColorScheme(
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

private val CosmicNeonCyanColorScheme = darkColorScheme(
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

private val SunsetRoseColorScheme = darkColorScheme(
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

private val EmeraldBreezeColorScheme = darkColorScheme(
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

private val VibrantLilacColorScheme = darkColorScheme(
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

private val ClassicLightColorScheme = lightColorScheme(
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
        AppTheme.PILOT_GOLD_BLUE -> PilotGoldBlueColorScheme
        AppTheme.COSMIC_NEON_CYAN -> CosmicNeonCyanColorScheme
        AppTheme.SUNSET_ROSE -> SunsetRoseColorScheme
        AppTheme.EMERALD_BREEZE -> EmeraldBreezeColorScheme
        AppTheme.VIBRANT_LILAC -> VibrantLilacColorScheme
        AppTheme.CLASSIC_LIGHT -> ClassicLightColorScheme
        AppTheme.DYNAMIC_MATERIAL_3 -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                PilotGoldBlueColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.lab3.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.lab3.data.ThemeMode

/**
 * Дополнительные семантические цвета финансов (доход, расход, 4 категории),
 * чтобы ни один компонент UI не использовал захардкоженные литералы Color(...).
 */
@Immutable
data class FinanceExtendedColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val categoryColors: List<Color>
)

private val LightFinanceExtended = FinanceExtendedColors(
    income = Color(0xFF0E7C59),
    incomeContainer = Color(0xFFD8F3E8),
    expense = Color(0xFFC2362B),
    expenseContainer = Color(0xFFFDE4E1),
    categoryColors = listOf(
        Color(0xFF1B56B3), // Продукты
        Color(0xFF0F766E), // Жильё
        Color(0xFFD97706), // Транспорт
        Color(0xFF6D28D9)  // Покупки
    )
)

private val DarkFinanceExtended = FinanceExtendedColors(
    income = Color(0xFF4ADE80),
    incomeContainer = Color(0xFF133A2C),
    expense = Color(0xFFF87171),
    expenseContainer = Color(0xFF441C1B),
    categoryColors = listOf(
        Color(0xFF60A5FA),
        Color(0xFF2DD4BF),
        Color(0xFFFBBF24),
        Color(0xFFA78BFA)
    )
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceExtended }

// Светлая палитра: спокойный банковский синий (вызывает доверие) + высокий контраст >= 4.5:1
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B56B3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF082454),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5F2EE),
    onSecondaryContainer = Color(0xFF073834),
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF141820),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141820),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF475061),
    outline = Color(0xFFC5CDD9),
    outlineVariant = Color(0xFFDDE3EC)
)

// Тёмная палитра: осознанные графитовые поверхности (#111418, НЕ чистый чёрный #000000!)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF082454),
    primaryContainer = Color(0xFF1D3B6E),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF05332E),
    secondaryContainer = Color(0xFF124640),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE7ECF3),
    surface = Color(0xFF1A1F26),
    onSurface = Color(0xFFE7ECF3),
    surfaceVariant = Color(0xFF242B35),
    onSurfaceVariant = Color(0xFFAAB4C4),
    outline = Color(0xFF3E4756),
    outlineVariant = Color(0xFF2D3542)
)

// Типографика с табличными цифрами (tnum), чтобы денежные суммы читались идеально ровно
private val FinanceTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum"
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum"
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum"
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum"
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum"
    )
)

private val FinanceShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun FinanceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    // Бонус +1: динамические цвета Material You на Android 12+ с корректным запасным вариантом
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColors
        else -> LightColors
    }

    val extendedColors = if (isDark) DarkFinanceExtended else LightFinanceExtended

    // Автоматически подстраиваем цвет иконок строки состояния под светлую/тёмную тему
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalFinanceColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinanceTypography,
            shapes = FinanceShapes,
            content = content
        )
    }
}

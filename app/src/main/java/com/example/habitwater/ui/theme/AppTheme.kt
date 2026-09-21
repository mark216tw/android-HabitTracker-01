package com.example.habitwater.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.habitwater.data.ThemeMode

fun colorFromHue(hue: Float, lightness: Float = 0.52f): Color =
    Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.72f, lightness)))

@Composable
fun HabitWaterTheme(
    mode: ThemeMode,
    hue: Float,
    activity: Activity,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val primary = colorFromHue(hue, if (dark) 0.72f else 0.44f)
    val secondary = colorFromHue((hue + 45f) % 360f, if (dark) 0.72f else 0.42f)
    val colors = if (dark) {
        darkColorScheme(primary = primary, secondary = secondary)
    } else {
        lightColorScheme(primary = primary, secondary = secondary)
    }

    SideEffect {
        val window = activity.window
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}

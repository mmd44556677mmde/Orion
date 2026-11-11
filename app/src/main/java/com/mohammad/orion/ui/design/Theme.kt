package com.mohammad.orion.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object OrionColors {
    val Green = Color(0xFF78F6A9)
    val Cyan = Color(0xFF7ADCE5)
    val Warning = Color(0xFFFFD180)
    val Error = Color(0xFFFFB4AB)
    val Offline = Color(0xFFB6C4BE)
    val Connecting = Cyan
    val Background = Color(0xFF090E0D)
    val Surface = Color(0xFF131D1A)
}
object OrionSpacing { val xs = 4.dp; val sm = 8.dp; val md = 16.dp; val lg = 24.dp; val xl = 32.dp }
object OrionElevation { val flat = 0.dp; val card = 2.dp }
object OrionMotion { const val short = 120; const val medium = 240; const val reduced = 0 }
private val Dark = darkColorScheme(primary = OrionColors.Green, onPrimary = Color(0xFF00391D), secondary = OrionColors.Cyan, onSecondary = Color(0xFF00363B), background = OrionColors.Background, surface = OrionColors.Surface, error = OrionColors.Error)
private val Light = lightColorScheme(primary = Color(0xFF006D3A), onPrimary = Color.White, secondary = Color(0xFF006970), onSecondary = Color.White, background = Color(0xFFF5FBF7), surface = Color(0xFFEAF4ED), error = Color(0xFFBA1A1A))
private val Type = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 22.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp),
)

/** Dynamic color is opt-in; the default always preserves the Orion palette. */
@Composable
fun OrionTheme(dark: Boolean = true, dynamic: Boolean = false, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = if (dynamic && android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
    } else if (dark) Dark else Light
    MaterialTheme(colorScheme = colors, typography = Type, content = content)
}

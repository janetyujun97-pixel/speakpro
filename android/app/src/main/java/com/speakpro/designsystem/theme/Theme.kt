package com.speakpro.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SpeakPro 自定义 Light 配色方案
 * 当前仅支持浅色模式，与 iOS 端保持一致
 */
private val SpeakProColorScheme = lightColorScheme(
    primary = SpPrimary,
    onPrimary = SpIvory,
    primaryContainer = SpBgSoft,
    onPrimaryContainer = SpPrimary,
    secondary = SpAccent,
    onSecondary = SpIvory,
    secondaryContainer = SpAccentSoft,
    onSecondaryContainer = SpAccent,
    tertiary = SpMoss,
    onTertiary = SpIvory,
    background = SpBackground,
    onBackground = SpTextPrimary,
    surface = SpIvory,
    onSurface = SpTextPrimary,
    surfaceVariant = SpBgSoft,
    onSurfaceVariant = SpMuted,
    error = SpError,
    onError = SpWhite,
    errorContainer = SpError.copy(alpha = 0.1f),
    onErrorContainer = SpError,
    outline = SpLine,
    outlineVariant = SpLine,
)

/**
 * SpeakPro 全局主题
 *
 * 用法：
 * ```
 * SpeakProTheme {
 *     // 应用内容
 * }
 * ```
 */
@Composable
fun SpeakProTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = SpeakProColorScheme

    // 设置状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpeakProTypography,
        content = content,
    )
}

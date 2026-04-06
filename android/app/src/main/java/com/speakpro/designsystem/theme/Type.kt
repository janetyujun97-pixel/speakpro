package com.speakpro.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// SpeakPro 7 级字体层级（匹配 iOS 端）

/** Title Large — 28sp Bold */
val SpTitleLarge = TextStyle(
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 34.sp,
)

/** Title Medium — 22sp SemiBold */
val SpTitleMedium = TextStyle(
    fontSize = 22.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 28.sp,
)

/** Title Small — 18sp SemiBold */
val SpTitleSmall = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 24.sp,
)

/** Body Large — 17sp Regular */
val SpBodyLarge = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 24.sp,
)

/** Body Medium — 15sp Regular */
val SpBodyMedium = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 22.sp,
)

/** Body Small — 13sp Regular */
val SpBodySmall = TextStyle(
    fontSize = 13.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 18.sp,
)

/** Caption — 11sp Regular */
val SpCaption = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 14.sp,
)

/**
 * Material3 Typography 映射到 SpeakPro 字体层级
 */
val SpeakProTypography = Typography(
    displayLarge = SpTitleLarge,
    displayMedium = SpTitleMedium,
    displaySmall = SpTitleSmall,
    headlineLarge = SpTitleLarge,
    headlineMedium = SpTitleMedium,
    headlineSmall = SpTitleSmall,
    titleLarge = SpTitleLarge,
    titleMedium = SpTitleMedium,
    titleSmall = SpTitleSmall,
    bodyLarge = SpBodyLarge,
    bodyMedium = SpBodyMedium,
    bodySmall = SpBodySmall,
    labelLarge = SpBodyMedium,
    labelMedium = SpBodySmall,
    labelSmall = SpCaption,
)

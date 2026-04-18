package com.speakpro.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.speakpro.app.R

// SpeakPro 7 级字体层级（匹配 iOS 端）
// Editorial 风格：Fraunces 承担标题与数字；Inter 承担正文。字体文件位于 res/font/。

// ——— 字体族定义 ———

/** Fraunces Variable（serif）— 标题与数字 */
@OptIn(ExperimentalTextApi::class)
val FraunceFamily = FontFamily(
    Font(
        resId = R.font.fraunces_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
        ),
    ),
    Font(
        resId = R.font.fraunces_italic_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
        ),
    ),
)

/** Inter Variable（sans）— 正文与 UI */
@OptIn(ExperimentalTextApi::class)
val InterFamily = FontFamily(
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
        ),
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
        ),
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
        ),
    ),
    Font(
        resId = R.font.inter_italic_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
        ),
    ),
)

// ——— 既有 token（切字体族，尺寸保留） ———

/** Title Large — Fraunces 28sp */
val SpTitleLarge = TextStyle(
    fontFamily = FraunceFamily,
    fontSize = 28.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 32.sp,
    letterSpacing = (-0.5).sp,
)

/** Title Medium — Fraunces 22sp */
val SpTitleMedium = TextStyle(
    fontFamily = FraunceFamily,
    fontSize = 22.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 26.sp,
    letterSpacing = (-0.3).sp,
)

/** Title Small — Fraunces 18sp */
val SpTitleSmall = TextStyle(
    fontFamily = FraunceFamily,
    fontSize = 18.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 22.sp,
)

/** Body Large — Inter 17sp */
val SpBodyLarge = TextStyle(
    fontFamily = InterFamily,
    fontSize = 17.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 24.sp,
)

/** Body Medium — Inter 15sp */
val SpBodyMedium = TextStyle(
    fontFamily = InterFamily,
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 22.sp,
)

/** Body Small — Inter 13sp */
val SpBodySmall = TextStyle(
    fontFamily = InterFamily,
    fontSize = 13.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 18.sp,
)

/** Caption — Inter 11sp */
val SpCaption = TextStyle(
    fontFamily = InterFamily,
    fontSize = 11.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 14.sp,
)

// ——— 新增 token ———

/** Eyebrow — Inter 10sp / 600 / letterSpacing 2.2；调用方对文本做 `.uppercase()` */
val SpEyebrow = TextStyle(
    fontFamily = InterFamily,
    fontSize = 10.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 2.2.sp,
)

/** Numeric 大数字 — Fraunces，调用方传入尺寸（22–72） */
fun spNumericStyle(sizeSp: Int): TextStyle = TextStyle(
    fontFamily = FraunceFamily,
    fontSize = sizeSp.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = (sizeSp * 0.9f).sp,
    letterSpacing = (-1).sp,
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

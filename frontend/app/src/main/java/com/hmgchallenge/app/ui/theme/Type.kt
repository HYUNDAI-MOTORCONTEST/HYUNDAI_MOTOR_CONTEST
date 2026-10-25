package com.hmgchallenge.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hmgchallenge.app.R

/**
 * Asta Sans(42dot, SIL OFL 1.1) — Pleos 시스템 UI 글꼴.
 * 출처: https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography
 * 파일: res/font/asta_sans_variable.ttf = google/fonts `ofl/astasans/AstaSans[wght].ttf`(v1.000), 라이선스 licenses/AstaSans-OFL.txt
 *
 * 가변 폰트 한 파일(wght 300~800, 파일 기본값 300)에서 굵기별 [Font]를 만든다.
 * `variationSettings`는 API 26 이상에서 적용되므로 minSdk 28에서 문제없다. 같은 파일을 굵기마다 다시 읽지 않게
 * 플랫폼이 Typeface를 캐시한다. `weight`는 매칭용, `FontVariation.weight`는 실제 wght 축 값이다.
 */
private fun astaSans(weight: FontWeight) = Font(
    resId = R.font.asta_sans_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val AstaSans = FontFamily(
    astaSans(FontWeight.Normal),    // 400
    astaSans(FontWeight.Medium),    // 500
    astaSans(FontWeight.SemiBold),  // 600
    astaSans(FontWeight.Bold),      // 700
    astaSans(FontWeight.ExtraBold), // 800
)

/**
 * Pleos 타이포 위계(Headline / Title / Body / Label).
 *
 * - 크기: typography 페이지의 표(이미지)에서 읽은 값. **원문에 단위 표기가 없다.** Android 앱이라 sp로 둔다(팀 판단).
 * - 줄 높이: 원문 수치(line-height)는 확인 못 함. 본문 규칙 "줄 간격 1.5배 이상"에 맞춰 크기 × 1.5로 둔다.
 * - letter-spacing: 원문 수치 확인 못 함 → 0.
 * - 굵기: 원문은 Headline ExtraBold·Bold, Title ExtraBold·Bold·Regular, Body Strong·Normal, Label Normal.
 *   기본 스타일은 각 위계에서 가장 흔히 쓸 굵기(Headline·Title Bold, Body·Label Normal)로 두고,
 *   Body Strong은 [bodyLargeStrong]처럼 따로 둔다. Strong의 실제 wght 값은 원문에 없어 SemiBold(600)로 정했다.
 * - 최소 크기: 원문 12sp 이상. Label Small(20)이 가장 작다. 팀 규칙(본문·버튼 24sp 이상, pleos-ui.md 10장)에 따라
 *   버튼·본문에는 Label Medium(24) 이상만 쓴다.
 */
@Immutable
data class PleosTypography(
    val headlineLarge: TextStyle = style(60.sp, FontWeight.Bold),
    val headlineMedium: TextStyle = style(56.sp, FontWeight.Bold),
    val headlineSmall: TextStyle = style(48.sp, FontWeight.Bold),
    val titleLarge: TextStyle = style(40.sp, FontWeight.Bold),
    val titleMedium: TextStyle = style(36.sp, FontWeight.Bold),
    val titleSmall: TextStyle = style(32.sp, FontWeight.Bold),
    val bodyLarge: TextStyle = style(30.sp, FontWeight.Normal),
    val bodyMedium: TextStyle = style(28.sp, FontWeight.Normal),
    val bodySmall: TextStyle = style(26.sp, FontWeight.Normal),
    val bodyLargeStrong: TextStyle = style(30.sp, FontWeight.SemiBold),
    val bodyMediumStrong: TextStyle = style(28.sp, FontWeight.SemiBold),
    val labelMedium: TextStyle = style(24.sp, FontWeight.Normal),
    val labelSmall: TextStyle = style(20.sp, FontWeight.Normal),
) {
    /**
     * M3 Typography 슬롯에 Pleos 위계를 그대로 연결한다. M3 display 슬롯은 Pleos에 없어 Headline 값을 쓰고,
     * M3 labelLarge(버튼 기본)는 Pleos Label Medium(24), labelMedium·labelSmall은 Label Small(20)이다.
     */
    fun toMaterial() = Typography(
        displayLarge = headlineLarge,
        displayMedium = headlineMedium,
        displaySmall = headlineSmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelMedium,
        labelMedium = labelSmall,
        labelSmall = labelSmall,
    )

    private companion object {
        fun style(size: TextUnit, weight: FontWeight) = TextStyle(
            fontFamily = AstaSans,
            fontWeight = weight,
            fontSize = size,
            lineHeight = size * 1.5f,
            letterSpacing = 0.sp,
        )
    }
}

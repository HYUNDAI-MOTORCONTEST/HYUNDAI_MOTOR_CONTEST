package com.hmgchallenge.sandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hmgchallenge.sandbox.care.CareEvent
import com.hmgchallenge.sandbox.care.CareLogStore
import com.hmgchallenge.sandbox.care.CareType
import com.hmgchallenge.sandbox.care.FeedForecast
import com.hmgchallenge.sandbox.care.FeedingGuide
import com.hmgchallenge.sandbox.care.ForecastStage
import com.hmgchallenge.sandbox.care.isSleeping
import com.hmgchallenge.sandbox.care.lastDiaper
import com.hmgchallenge.sandbox.care.lastFeed
import com.hmgchallenge.sandbox.care.lastSleepEvent
import com.hmgchallenge.sandbox.driving.LocalUxRestrictions
import com.hmgchallenge.sandbox.driving.UxRestrictionsState
import com.hmgchallenge.sandbox.ui.theme.LocalPleosColors
import com.hmgchallenge.sandbox.ui.theme.LocalPleosTypography
import com.hmgchallenge.sandbox.ui.theme.VehicleTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VehicleTheme {
                val vm: MainViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                val ux by vm.uxRestrictions.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalUxRestrictions provides ux) {
                    SandboxScreen(state = state, onRecord = vm::record, onMonthChange = vm::changeMonthAge)
                }
            }
        }
    }
}

/**
 * 한 화면 앱. 운전 중 제한(근거: docs/guides/pleos-dev.md 2·3장, docs/guides/pleos-ui.md 1·10장):
 * - 주행 규제 중(`requiresDistractionOptimization`, 연결 중·확인 불가 포함)에는 상단 한 줄(앱 이름·배지),
 *   예보 한 줄, 큰 버튼 3개만 보인다. 월령 변경·최근 기록·배터리 줄·면책 문구는 숨긴다.
 * - 애니메이션 없음: animate* API와 ripple을 쓰지 않고, 눌림은 색만 즉시 바꾼다(0.25초 응답 기준).
 * - 수동 스크롤만 둔다(자동 스크롤 없음). 작은 window에서 버튼에 닿을 수 있게 하려는 것이다.
 * - 너비로 배치를 나눈다: 1100dp 이상이면 2단(왼쪽 예보·버튼, 오른쪽 최근 기록), 아니면 1단.
 *   버튼은 640dp 이상이면 가로 3개, 아니면 세로로 쌓는다. 기준 너비는 팀 판단(Pleos 수치 없음).
 */
@Composable
fun SandboxScreen(
    state: SandboxUiState,
    onRecord: (CareAction) -> Unit,
    onMonthChange: (Int) -> Unit,
) {
    val restricted = LocalUxRestrictions.current.requiresDistractionOptimization
    val forecast = FeedForecast.of(state.events, state.monthAge, state.nowMillis)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 1100.dp
            val buttonsInRow = maxWidth >= 640.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 40.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Header()
                if (restricted) {
                    ForecastLine(forecast)
                    CareButtons(state, onRecord, inRow = buttonsInRow, height = if (buttonsInRow) 200.dp else 128.dp)
                } else {
                    MonthSelector(state.monthAge, onMonthChange)
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                ForecastCard(forecast, state.monthAge)
                                CareButtons(state, onRecord, inRow = true, height = 160.dp)
                            }
                            RecentList(state.events, state.nowMillis, Modifier.weight(1f))
                        }
                    } else {
                        ForecastCard(forecast, state.monthAge)
                        CareButtons(state, onRecord, inRow = buttonsInRow, height = if (buttonsInRow) 160.dp else 112.dp)
                        RecentList(state.events, state.nowMillis, Modifier.fillMaxWidth())
                    }
                    BatteryLine(state.battery)
                }
            }
        }
    }
}

// ── 상단 ──

@Composable
private fun Header() {
    val type = LocalPleosTypography.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = type.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        DrivingBadge()
    }
}

/** 주차/주행 제한 배지. 주행 중(연결 중·확인 불가 포함)은 반전 색: inverseSurface/inverseOnSurface 대비 주간 12.80, 야간 15.89. */
@Composable
private fun DrivingBadge() {
    val ux = LocalUxRestrictions.current
    val scheme = MaterialTheme.colorScheme
    val restricted = ux.requiresDistractionOptimization
    val label = when (ux.source) {
        UxRestrictionsState.Source.CONNECTING -> stringResource(R.string.drive_connecting)
        UxRestrictionsState.Source.UNAVAILABLE -> stringResource(R.string.drive_unavailable)
        UxRestrictionsState.Source.CAR_SERVICE ->
            stringResource(if (restricted) R.string.drive_restricted else R.string.drive_parked)
    }
    Text(
        text = label,
        style = LocalPleosTypography.current.bodyMediumStrong,
        color = if (restricted) scheme.inverseOnSurface else scheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (restricted) scheme.inverseSurface else scheme.surfaceVariant)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

/** 월령 선택(0~24개월). 드롭다운 대신 −/+ 스테퍼(pleos-ui.md 10장 "조작 단순화"). 주차 중에만 보인다. */
@Composable
private fun MonthSelector(monthAge: Int, onMonthChange: (Int) -> Unit) {
    val type = LocalPleosTypography.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceVariant)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.month_label),
            style = type.bodyMedium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        StepperButton(
            label = stringResource(R.string.month_minus),
            description = stringResource(R.string.month_minus_desc),
            enabled = monthAge > CareLogStore.MIN_MONTH_AGE,
            onClick = { onMonthChange(-1) },
        )
        Text(
            text = stringResource(R.string.month_value, monthAge),
            style = type.titleSmall,
            color = scheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(140.dp),
        )
        StepperButton(
            label = stringResource(R.string.month_plus),
            description = stringResource(R.string.month_plus_desc),
            enabled = monthAge < CareLogStore.MAX_MONTH_AGE,
            onClick = { onMonthChange(+1) },
        )
    }
}

// ── 예보 ──

@Composable
private fun stageLabel(stage: ForecastStage) = stringResource(
    when (stage) {
        ForecastStage.RELAXED -> R.string.stage_relaxed
        ForecastStage.CAUTION -> R.string.stage_caution
        ForecastStage.IMMINENT -> R.string.stage_imminent
    },
)

/** 단계 표시 막대 색. 글자가 단계를 이미 전하는 보조 표시라 Informative·Regulation 색을 쓴다(PleosRoles 주석: 글자색 금지). */
@Composable
private fun stageColor(forecast: FeedForecast): Color {
    val colors = LocalPleosColors.current
    return when ((forecast as? FeedForecast.Ready)?.stage) {
        null -> MaterialTheme.colorScheme.surfaceVariant
        ForecastStage.RELAXED -> colors.informative_active
        ForecastStage.CAUTION -> colors.regulation_orange
        ForecastStage.IMMINENT -> colors.informative_negative
    }
}

@Composable
private fun durationText(minutes: Long): String {
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).toInt()
    return when {
        h == 0 -> stringResource(R.string.duration_m, m)
        m == 0 -> stringResource(R.string.duration_h, h)
        else -> stringResource(R.string.duration_hm, h, m)
    }
}

/** 주차 중 예보 카드: 남은 시간(핵심 수치 40sp) + 단계 + 경과 시간 + 기본 간격(예시 값 문구). */
@Composable
private fun ForecastCard(forecast: FeedForecast, monthAge: Int) {
    val type = LocalPleosTypography.current
    val scheme = MaterialTheme.colorScheme
    StageCard(stageColor(forecast)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.forecast_title),
                style = type.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (forecast is FeedForecast.Ready) StageChip(stageLabel(forecast.stage))
        }
        when (forecast) {
            FeedForecast.NoFeed -> {
                Text(stringResource(R.string.forecast_no_feed), style = type.titleMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.forecast_no_feed_detail), style = type.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            is FeedForecast.Ready -> {
                val main = if (forecast.remainingMinutes >= 0) {
                    stringResource(R.string.forecast_remaining, durationText(forecast.remainingMinutes))
                } else {
                    stringResource(R.string.forecast_overdue, durationText(-forecast.remainingMinutes))
                }
                Text(main, style = type.titleLarge, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.forecast_elapsed, durationText(forecast.elapsedMinutes), formatClock(forecast.lastFeedAtMillis)),
                    style = type.bodyMedium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            stringResource(R.string.forecast_interval, monthAge, durationText(FeedingGuide.intervalMinutes(monthAge).toLong())),
            style = type.labelMedium,
            color = scheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 주행 중 예보 한 줄(40자 안팎, 32sp). LIMIT_STRING_LENGTH면 플랫폼 길이로 자른다. */
@Composable
private fun ForecastLine(forecast: FeedForecast) {
    val ux = LocalUxRestrictions.current
    val text = when (forecast) {
        FeedForecast.NoFeed -> stringResource(R.string.forecast_line_none)
        is FeedForecast.Ready ->
            if (forecast.remainingMinutes >= 0) {
                stringResource(R.string.forecast_line_remaining, durationText(forecast.remainingMinutes), stageLabel(forecast.stage))
            } else {
                stringResource(R.string.forecast_line_overdue, durationText(-forecast.remainingMinutes), stageLabel(forecast.stage))
            }
    }
    StageCard(stageColor(forecast)) {
        Text(ux.limit(text), style = LocalPleosTypography.current.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 왼쪽에 단계 색 막대가 있는 카드(surface_high). */
@Composable
private fun StageCard(barColor: Color, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .width(12.dp)
                .fillMaxHeight()
                .background(barColor),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

/** 단계 글자 칩: text_dark_primary / background_primary 대비 주간 17.33, 야간 16.67. */
@Composable
private fun StageChip(label: String) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = label,
        style = LocalPleosTypography.current.bodyMediumStrong,
        color = scheme.onBackground,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.background)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

// ── 큰 버튼 3개 ──

@Composable
private fun CareButtons(state: SandboxUiState, onRecord: (CareAction) -> Unit, inRow: Boolean, height: Dp) {
    val events = state.events
    val sleeping = events.isSleeping
    val feedSub = events.lastFeed?.let { stringResource(R.string.btn_last, formatClock(it.atMillis)) } ?: stringResource(R.string.btn_none)
    val diaperSub = events.lastDiaper?.let { stringResource(R.string.btn_last, formatClock(it.atMillis)) } ?: stringResource(R.string.btn_none)
    val sleepSub = when {
        sleeping -> stringResource(R.string.btn_sleeping_since, formatClock(events.lastSleepEvent!!.atMillis))
        events.lastSleepEvent != null -> stringResource(R.string.btn_last, formatClock(events.lastSleepEvent!!.atMillis))
        else -> stringResource(R.string.btn_none)
    }
    val buttons: @Composable (Modifier) -> Unit = { m ->
        BigButton(stringResource(R.string.btn_feed), feedSub, { onRecord(CareAction.FEED) }, m)
        BigButton(stringResource(R.string.btn_diaper), diaperSub, { onRecord(CareAction.DIAPER) }, m)
        BigButton(
            stringResource(if (sleeping) R.string.btn_sleep_end else R.string.btn_sleep_start),
            sleepSub,
            { onRecord(CareAction.SLEEP_TOGGLE) },
            m,
            active = sleeping,
        )
    }
    // 버튼 간격 24dp(pleos-ui.md 10장, Google car-app-quality UX-2)
    if (inRow) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            buttons(Modifier.weight(1f).height(height))
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            buttons(Modifier.fillMaxWidth().height(height))
        }
    }
}

/**
 * 큰 기록 버튼. 색: button_filled_enabled / pressed(라벨 basic_00, 대비 주간 9.41·12.80, 야간 14.11·11.12 — PleosRoles 표).
 * 수면 중(active)이면 반전 배너와 같은 inverseSurface(basic_700)/basic_00(주간 12.80, 야간 15.89)로 구분한다.
 * 크기: 최소 64dp × 64dp 이상(실제 높이 112~200dp) — Pleos 수치 없음, Google car-app-quality UX-1.
 */
@Composable
private fun BigButton(label: String, sub: String, onClick: () -> Unit, modifier: Modifier, active: Boolean = false) {
    val colors = LocalPleosColors.current
    val scheme = MaterialTheme.colorScheme
    val type = LocalPleosTypography.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container = when {
        pressed -> colors.button_filled_pressed
        active -> scheme.inverseSurface
        else -> colors.button_filled_enabled
    }
    Column(
        modifier = modifier
            .sizeIn(minWidth = 64.dp, minHeight = 72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = type.titleMedium, color = colors.basic_00, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = type.labelMedium, color = colors.basic_00, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** −/+ 버튼 72dp(주요 버튼). disabled 라벨은 text_dark_tertiary(카드 위 대비 주간 4.98, 야간 5.60 — PleosRoles 표). */
@Composable
private fun StepperButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalPleosColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container = when {
        !enabled -> colors.button_filled_disabled
        pressed -> colors.button_filled_pressed
        else -> colors.button_filled_enabled
    }
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 96.dp, minHeight = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = LocalPleosTypography.current.titleMedium,
            color = if (enabled) colors.basic_00 else colors.text_dark_tertiary,
            maxLines = 1,
        )
    }
}

// ── 최근 기록 (주차 중) ──

@Composable
private fun RecentList(events: List<CareEvent>, nowMillis: Long, modifier: Modifier) {
    val type = LocalPleosTypography.current
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceVariant)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.recent_title), style = type.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1)
        val recent = events.take(5)
        if (recent.isEmpty()) {
            Text(stringResource(R.string.recent_empty), style = type.bodyMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        recent.forEach { e ->
            RecentRow(formatDateTime(e.atMillis, nowMillis), eventLabel(e.type))
        }
    }
}

@Composable
private fun RecentRow(time: String, label: String) {
    val type = LocalPleosTypography.current
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(time, style = type.bodyMediumStrong, color = scheme.onSurface, maxLines = 1)
        Text(label, style = type.bodyMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun eventLabel(type: CareType) = stringResource(
    when (type) {
        CareType.FEED -> R.string.event_feed
        CareType.DIAPER -> R.string.event_diaper
        CareType.SLEEP_START -> R.string.event_sleep_start
        CareType.SLEEP_END -> R.string.event_sleep_end
    },
)

// ── 하단 배터리 줄 (주차 중) ──

@Composable
private fun BatteryLine(battery: LoadState<Int>) {
    val text = when (battery) {
        LoadState.Loading -> stringResource(R.string.battery_loading)
        is LoadState.Success -> stringResource(R.string.battery_value, battery.value)
        is LoadState.Error -> stringResource(R.string.battery_error, battery.message)
    }
    Text(
        text = text,
        style = LocalPleosTypography.current.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// ── 시각 표기 ──

private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("M/d")

private fun formatClock(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(clockFormat)

/** 오늘이면 "14:05", 아니면 "10/24 14:05". */
@Composable
private fun formatDateTime(millis: Long, nowMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(millis).atZone(zone)
    val today: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return if (at.toLocalDate() == today) at.format(clockFormat) else stringResource(R.string.date_time, at.format(dateFormat), at.format(clockFormat))
}

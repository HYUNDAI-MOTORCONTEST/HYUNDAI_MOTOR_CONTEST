package com.hmgchallenge.sandbox.care

import android.content.Context

/** 기록 종류. [key]는 SharedPreferences에 저장하는 값이라 바꾸지 않는다. */
enum class CareType(val key: String) {
    FEED("feed"),
    DIAPER("diaper"),
    SLEEP_START("sleep_start"),
    SLEEP_END("sleep_end"),
    ;

    val isSleep get() = this == SLEEP_START || this == SLEEP_END

    companion object {
        fun fromKey(key: String): CareType? = entries.firstOrNull { it.key == key }
    }
}

data class CareEvent(val type: CareType, val atMillis: Long)

/** 기록 목록에서 바로 읽는 값들. 목록은 최신순이다. */
val List<CareEvent>.lastFeed get() = firstOrNull { it.type == CareType.FEED }
val List<CareEvent>.lastDiaper get() = firstOrNull { it.type == CareType.DIAPER }
val List<CareEvent>.lastSleepEvent get() = firstOrNull { it.type.isSleep }
val List<CareEvent>.isSleeping get() = lastSleepEvent?.type == CareType.SLEEP_START

/**
 * 기록·월령을 SharedPreferences에 저장한다(앱을 다시 켜도 남는다). 로컬 테스트용이라 가장 단순한 방법을 골랐다.
 * 형식: `events` = "feed:1729850000000;diaper:1729851000000"(최신순, 최대 [MAX_EVENTS]개), `month_age` = Int.
 * 읽을 수 없는 항목은 건너뛴다.
 */
class CareLogStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadEvents(): List<CareEvent> =
        prefs.getString(KEY_EVENTS, "").orEmpty()
            .split(';')
            .mapNotNull { item ->
                val (key, at) = item.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                val type = CareType.fromKey(key) ?: return@mapNotNull null
                at.toLongOrNull()?.let { CareEvent(type, it) }
            }
            .sortedByDescending { it.atMillis }

    fun saveEvents(events: List<CareEvent>) {
        val text = events.take(MAX_EVENTS).joinToString(";") { "${it.type.key}:${it.atMillis}" }
        prefs.edit().putString(KEY_EVENTS, text).apply()
    }

    fun loadMonthAge(): Int = prefs.getInt(KEY_MONTH, DEFAULT_MONTH_AGE).coerceIn(MIN_MONTH_AGE, MAX_MONTH_AGE)

    fun saveMonthAge(month: Int) {
        prefs.edit().putInt(KEY_MONTH, month).apply()
    }

    companion object {
        const val MIN_MONTH_AGE = 0
        const val MAX_MONTH_AGE = 24
        const val DEFAULT_MONTH_AGE = 2
        const val MAX_EVENTS = 50
        private const val PREFS = "agarang_mini"
        private const val KEY_EVENTS = "events"
        private const val KEY_MONTH = "month_age"
    }
}

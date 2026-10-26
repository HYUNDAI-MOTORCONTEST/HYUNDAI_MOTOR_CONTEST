package com.hmgchallenge.sandbox.care

/**
 * 월령별 기본 수유 간격.
 *
 * **예시 값 — 의학적 판단 아님.** 출처 없이 로컬 테스트용으로 정한 값이다. 실제 수유 간격은 아이마다 다르고
 * 의료진 안내를 따라야 한다. 화면에도 같은 문구를 보인다(strings.xml `forecast_interval`).
 */
object FeedingGuide {
    fun intervalMinutes(monthAge: Int): Int = when (monthAge) {
        in 0..1 -> 150
        in 2..3 -> 180
        in 4..5 -> 210
        in 6..11 -> 240
        else -> 300
    }

    /** 단계 기준(예시 값): 남은 시간이 이 값 이상이면 여유, [IMMINENT_MINUTES] 이상이면 주의, 그 밖(지남 포함)은 임박. */
    const val RELAXED_MINUTES = 60
    const val IMMINENT_MINUTES = 15
}

enum class ForecastStage { RELAXED, CAUTION, IMMINENT }

sealed interface FeedForecast {
    /** 수유 기록이 없어 예보할 수 없다. */
    data object NoFeed : FeedForecast

    /**
     * @param remainingMinutes 다음 수유 예상까지 남은 분. 음수면 예상 시각이 지났다.
     */
    data class Ready(
        val lastFeedAtMillis: Long,
        val elapsedMinutes: Long,
        val intervalMinutes: Int,
        val remainingMinutes: Long,
        val stage: ForecastStage,
    ) : FeedForecast

    companion object {
        fun of(events: List<CareEvent>, monthAge: Int, nowMillis: Long): FeedForecast {
            val last = events.lastFeed ?: return NoFeed
            val elapsed = ((nowMillis - last.atMillis).coerceAtLeast(0)) / 60_000
            val interval = FeedingGuide.intervalMinutes(monthAge)
            val remaining = interval - elapsed
            val stage = when {
                remaining >= FeedingGuide.RELAXED_MINUTES -> ForecastStage.RELAXED
                remaining >= FeedingGuide.IMMINENT_MINUTES -> ForecastStage.CAUTION
                else -> ForecastStage.IMMINENT
            }
            return Ready(last.atMillis, elapsed, interval, remaining, stage)
        }
    }
}

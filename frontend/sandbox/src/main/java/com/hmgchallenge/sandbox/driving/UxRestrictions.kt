// 복사본: 원본 frontend/app/src/main/java/com/hmgchallenge/app/driving/UxRestrictions.kt (패키지·R·로그 태그만 바꿈). 원본을 고치면 여기도 맞춘다.
package com.hmgchallenge.sandbox.driving

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 운전 중 UX 제한 상태(CarUxRestrictions)를 앱 안에서 쓰는 형태로 옮긴 값.
 *
 * 근거: docs/guides/pleos-dev.md 2.2·2.4장
 * - `isRequiresDistractionOptimization()`이 참이면 애니메이션·자동 스크롤을 끄고,
 * - `getActiveRestrictions()` 비트마스크의 플래그별로 기능을 막는다.
 * - 지역별 규제는 플랫폼이 플래그로 내려주므로 앱은 플래그만 본다(지역 판단 금지).
 */
data class UxRestrictionsState(
    val source: Source,
    /** 주행 규제 중(DO 필요). 참이면 애니메이션·자동 스크롤 끔, 상세 정보 축약. */
    val requiresDistractionOptimization: Boolean,
    /** `CarUxRestrictions.UX_RESTRICTIONS_*` 비트마스크 */
    val activeRestrictions: Int,
    /** LIMIT_STRING_LENGTH일 때 문자열 최대 길이(플랫폼 설정값) */
    val maxStringLength: Int,
) {
    enum class Source {
        /** Car 서비스 연결 대기 중 */
        CONNECTING,
        /** CarUxRestrictionsManager에서 받은 값 */
        CAR_SERVICE,
        /** android.car 미지원·연결 실패. 안전하게 제한 상태로 본다. */
        UNAVAILABLE,
    }

    /** 플래그가 켜져 있는지. DO가 필요 없는 상태(주차)면 항상 false. */
    fun has(flag: Int): Boolean = requiresDistractionOptimization && (activeRestrictions and flag) != 0

    val noVideo get() = has(FLAG_NO_VIDEO)
    val noKeyboard get() = has(FLAG_NO_KEYBOARD)
    val noSetup get() = has(FLAG_NO_SETUP)
    val limitStringLength get() = has(FLAG_LIMIT_STRING_LENGTH)
    val limitContent get() = has(FLAG_LIMIT_CONTENT)

    /** 화면에 보이는 문자열 길이 제한. LIMIT_STRING_LENGTH가 아니면 원문 그대로. */
    fun limit(text: String): String =
        if (limitStringLength && text.length > maxStringLength) text.take(maxStringLength - 1) + "…" else text

    companion object {
        // CarUxRestrictions 상수 값(AAOS 공개 API, docs/guides/pleos-dev.md 2.3장 표와 같음).
        // android.car가 없는 환경에서도 클래스 로딩 없이 쓰려고 값으로 둔다.
        const val FLAG_NO_DIALPAD = 0x1
        const val FLAG_NO_FILTERING = 0x2
        const val FLAG_LIMIT_STRING_LENGTH = 0x4
        const val FLAG_NO_KEYBOARD = 0x8
        const val FLAG_NO_VIDEO = 0x10
        const val FLAG_LIMIT_CONTENT = 0x20
        const val FLAG_NO_SETUP = 0x40
        const val FLAG_NO_TEXT_MESSAGE = 0x80
        const val FLAG_NO_VOICE_TRANSCRIPTION = 0x100
        const val FLAG_FULLY_RESTRICTED = 0x1ff

        /** AAOS 기본 설정의 문자열 제한값(에뮬레이터 dumpsys에서도 120). */
        private const val DEFAULT_MAX_STRING_LENGTH = 120

        val Connecting = UxRestrictionsState(Source.CONNECTING, true, FLAG_FULLY_RESTRICTED, DEFAULT_MAX_STRING_LENGTH)
        val Unavailable = UxRestrictionsState(Source.UNAVAILABLE, true, FLAG_FULLY_RESTRICTED, DEFAULT_MAX_STRING_LENGTH)

        /** Preview·테스트용 주차 상태 */
        val Parked = UxRestrictionsState(Source.CAR_SERVICE, false, 0, DEFAULT_MAX_STRING_LENGTH)

        internal fun from(r: CarUxRestrictions) = UxRestrictionsState(
            source = Source.CAR_SERVICE,
            requiresDistractionOptimization = r.isRequiresDistractionOptimization,
            activeRestrictions = r.activeRestrictions,
            maxStringLength = r.maxRestrictedStringLength.takeIf { it > 1 } ?: DEFAULT_MAX_STRING_LENGTH,
        )
    }
}

/**
 * Compose 트리 어디서든 같은 제한 상태를 읽게 한다(docs/guides/pleos-dev.md 2.4장 "한 곳에서 읽게").
 * 기본값은 안전한 쪽(제한)으로 둔다.
 */
val LocalUxRestrictions = staticCompositionLocalOf { UxRestrictionsState.Connecting }

/**
 * `Car.createCar` + `CarUxRestrictionsManager`로 UX 제한을 구독한다.
 *
 * - 권한: UX 제한 조회·구독은 `android.car.permission.*`이 필요 없다(AAOS 공개 API에 권한 요구 없음).
 *   그래서 매니페스트에 car 권한을 추가하지 않는다.
 * - `android.car`가 없거나(일반 폰) Car 서비스 연결이 실패해도 앱이 죽지 않게 모든 호출을 감싼다.
 *   이때 상태는 [UxRestrictionsState.Unavailable](제한)로 둔다: 주행 여부를 모르면 안전한 쪽을 택한다.
 * - [start]·[stop]은 메인 스레드에서 부른다.
 */
class UxRestrictionsMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(UxRestrictionsState.Connecting)
    val state: StateFlow<UxRestrictionsState> = _state.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        if (car != null) return
        if (!isCarApiAvailable()) {
            Log.w(TAG, "android.car API 없음 → 제한 상태(Unavailable)로 표시")
            _state.value = UxRestrictionsState.Unavailable
            return
        }
        try {
            // DO_NOT_WAIT: 메인 스레드를 막지 않는다. Car 서비스가 재시작되면 리스너가 다시 불린다.
            car = Car.createCar(appContext, mainHandler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { c, ready ->
                if (ready) onCarReady(c) else onCarLost()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Car.createCar failed", e)
            car = null
            _state.value = UxRestrictionsState.Unavailable
        }
    }

    fun stop() {
        try {
            manager?.unregisterListener()
        } catch (e: Throwable) {
            Log.w(TAG, "unregisterListener failed", e)
        }
        manager = null
        try {
            car?.disconnect()
        } catch (e: Throwable) {
            Log.w(TAG, "Car.disconnect failed", e)
        }
        car = null
    }

    private fun onCarReady(c: Car) {
        try {
            val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            if (m == null) {
                Log.w(TAG, "CarUxRestrictionsManager 없음")
                _state.value = UxRestrictionsState.Unavailable
                return
            }
            manager = m
            m.registerListener { r -> update(r) }
            update(m.currentCarUxRestrictions)
        } catch (e: Throwable) {
            Log.e(TAG, "UX restrictions subscribe failed", e)
            _state.value = UxRestrictionsState.Unavailable
        }
    }

    private fun onCarLost() {
        // Car 서비스가 죽었다. 다시 연결되면 onCarReady가 다시 불린다.
        manager = null
        _state.value = UxRestrictionsState.Connecting
    }

    private fun update(r: CarUxRestrictions?) {
        val next = if (r == null) UxRestrictionsState.Unavailable else UxRestrictionsState.from(r)
        Log.i(TAG, "UX restrictions: DO=${next.requiresDistractionOptimization} active=0x${next.activeRestrictions.toString(16)}")
        _state.value = next
    }

    // 자동차 기기가 아니면 android.car가 없다. 기기는 맞는데 클래스가 없으면 아래 createCar의
    // catch(Throwable)이 NoClassDefFoundError를 잡는다(리플렉션은 쓰지 않는다: lint PrivateApi).
    private fun isCarApiAvailable(): Boolean =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)

    private companion object {
        const val TAG = "AgarangMini"
    }
}

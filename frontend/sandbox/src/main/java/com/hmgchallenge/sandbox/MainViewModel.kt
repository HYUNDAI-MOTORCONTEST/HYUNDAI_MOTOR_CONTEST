package com.hmgchallenge.sandbox

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hmgchallenge.sandbox.care.CareEvent
import com.hmgchallenge.sandbox.care.CareLogStore
import com.hmgchallenge.sandbox.care.CareType
import com.hmgchallenge.sandbox.care.isSleeping
import com.hmgchallenge.sandbox.driving.UxRestrictionsMonitor
import com.hmgchallenge.sandbox.driving.UxRestrictionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val value: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

data class SandboxUiState(
    /** 최신순 */
    val events: List<CareEvent> = emptyList(),
    val monthAge: Int = CareLogStore.DEFAULT_MONTH_AGE,
    /** 예보 계산 기준 시각. 1분마다, 그리고 기록할 때 갱신한다. */
    val nowMillis: Long = System.currentTimeMillis(),
    val battery: LoadState<Int> = LoadState.Loading,
)

/** 큰 버튼 3개 */
enum class CareAction { FEED, DIAPER, SLEEP_TOGGLE }

/**
 * Vehicle SDK·UX 제한 구독은 본 앱 frontend/app/.../MainViewModel.kt와 같은 패턴이다
 * (initialize → 한 번 읽기(재시도 1회) → 리스너 등록, onCleared에서 해제·release).
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = CareLogStore(application)

    private val _state = MutableStateFlow(
        SandboxUiState(events = store.loadEvents(), monthAge = store.loadMonthAge()),
    )
    val state: StateFlow<SandboxUiState> = _state.asStateFlow()

    private val uxMonitor = UxRestrictionsMonitor(application)
    val uxRestrictions: StateFlow<UxRestrictionsState> = uxMonitor.state

    private var vehicle: Vehicle? = null

    // init에서 등록하므로 init보다 먼저 선언한다.
    private val batteryListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(percent: Int?) = onBatteryUpdated(percent)

        override fun onFailed(e: Exception) {
            Log.w(TAG, "EvBatteryStateOfCharge listener failed", e)
        }
    }

    init {
        uxMonitor.start()
        startMinuteTicker()
        initVehicleAndReadBattery()
    }

    fun record(action: CareAction) {
        val now = System.currentTimeMillis()
        val type = when (action) {
            CareAction.FEED -> CareType.FEED
            CareAction.DIAPER -> CareType.DIAPER
            CareAction.SLEEP_TOGGLE -> if (_state.value.events.isSleeping) CareType.SLEEP_END else CareType.SLEEP_START
        }
        val next = (listOf(CareEvent(type, now)) + _state.value.events).take(CareLogStore.MAX_EVENTS)
        _state.update { it.copy(events = next, nowMillis = now) }
        store.saveEvents(next)
        Log.i(TAG, "record ${type.key} at $now")
    }

    /** 월령 변경은 주차 중에만 허용한다(화면에서도 주행 중에는 숨긴다). */
    fun changeMonthAge(delta: Int) {
        if (uxRestrictions.value.requiresDistractionOptimization) return
        val next = (_state.value.monthAge + delta).coerceIn(CareLogStore.MIN_MONTH_AGE, CareLogStore.MAX_MONTH_AGE)
        if (next == _state.value.monthAge) return
        _state.update { it.copy(monthAge = next) }
        store.saveMonthAge(next)
    }

    // 분이 바뀌는 시점에 맞춰 1분마다 예보 기준 시각을 갱신한다.
    private fun startMinuteTicker() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                delay(60_000 - now % 60_000)
                _state.update { it.copy(nowMillis = System.currentTimeMillis()) }
            }
        }
    }

    private fun initVehicleAndReadBattery() {
        try {
            vehicle = Vehicle(getApplication()).also { it.initialize() }
        } catch (e: Throwable) {
            // SDK 서비스가 없는 기기 등에서도 앱이 죽지 않게 한다.
            Log.e(TAG, "Vehicle initialize failed", e)
            _state.update { it.copy(battery = LoadState.Error("SDK 초기화 실패")) }
            return
        }
        // initialize()는 서비스 바인딩을 비동기로 진행할 수 있어, 첫 호출이 실패하면 한 번 더 시도한다.
        readBattery(retriesLeft = 1)
        registerBatteryListener()
    }

    private fun readBattery(retriesLeft: Int) {
        val v = vehicle ?: return
        try {
            v.getEvBattery().getEvBatteryStateOfCharge(
                { percent: Int? -> onBatteryUpdated(percent) },
                { e: Exception ->
                    Log.w(TAG, "getEvBatteryStateOfCharge failed (retriesLeft=$retriesLeft)", e)
                    if (retriesLeft > 0) {
                        viewModelScope.launch {
                            delay(1_500)
                            readBattery(retriesLeft - 1)
                        }
                    } else {
                        _state.update { it.copy(battery = LoadState.Error(e.message ?: e.javaClass.simpleName)) }
                    }
                },
            )
        } catch (e: Throwable) {
            Log.e(TAG, "getEvBatteryStateOfCharge threw", e)
            _state.update { it.copy(battery = LoadState.Error(e.message ?: e.javaClass.simpleName)) }
        }
    }

    // Virtual Console 등에서 값이 바뀌면 화면에 바로 반영되도록 구독한다.
    private fun registerBatteryListener() {
        try {
            vehicle?.getEvBattery()?.registerEvBatteryStateOfCharge(batteryListener)
        } catch (e: Throwable) {
            Log.w(TAG, "registerEvBatteryStateOfCharge failed", e)
        }
    }

    private fun onBatteryUpdated(percent: Int?) {
        _state.update {
            it.copy(battery = if (percent == null) LoadState.Error("값 없음") else LoadState.Success(percent))
        }
    }

    override fun onCleared() {
        uxMonitor.stop()
        try {
            vehicle?.getEvBattery()?.unregisterEvBatteryStateOfCharge(batteryListener)
        } catch (e: Throwable) {
            Log.w(TAG, "unregisterEvBatteryStateOfCharge failed", e)
        }
        try {
            vehicle?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Vehicle release failed", e)
        }
        vehicle = null
        super.onCleared()
    }

    private companion object {
        const val TAG = "AgarangMini"
    }
}

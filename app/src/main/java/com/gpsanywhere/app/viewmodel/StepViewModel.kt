package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.health.HealthConnectSteps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Manual step input backed by Health Connect. */
class StepViewModel(application: Application) : AndroidViewModel(application) {

    /** Outcome of the last manual write: steps added on success, error text on failure. */
    data class StepsWriteResult(val success: Boolean, val steps: Long = 0, val error: String? = null)

    private val _healthStatus = MutableStateFlow<HealthConnectSteps.Status?>(null)
    val healthStatus: StateFlow<HealthConnectSteps.Status?> = _healthStatus

    private val _todayHealthSteps = MutableStateFlow<Long?>(null)
    val todayHealthSteps: StateFlow<Long?> = _todayHealthSteps

    private val _stepsWriteResult = MutableStateFlow<StepsWriteResult?>(null)
    val stepsWriteResult: StateFlow<StepsWriteResult?> = _stepsWriteResult

    fun refreshHealthStatus() {
        viewModelScope.launch {
            val status = HealthConnectSteps.status(getApplication())
            _healthStatus.value = status
            if (status == HealthConnectSteps.Status.READY) {
                _todayHealthSteps.value = HealthConnectSteps.readTodayTotal(getApplication())
            }
        }
    }

    /** Dismiss the last write result, e.g. when leaving the screen or editing the input. */
    fun clearWriteResult() {
        _stepsWriteResult.value = null
    }

    fun addManualSteps(count: Long) {
        if (count <= 0) return
        _stepsWriteResult.value = null
        viewModelScope.launch {
            val result = HealthConnectSteps.writeSteps(getApplication(), count)
            _stepsWriteResult.value = result.fold(
                onSuccess = { StepsWriteResult(success = true, steps = count) },
                onFailure = { StepsWriteResult(success = false, error = it.message ?: it.javaClass.simpleName) }
            )
            if (result.isSuccess) {
                _todayHealthSteps.value = HealthConnectSteps.readTodayTotal(getApplication())
            }
        }
    }
}

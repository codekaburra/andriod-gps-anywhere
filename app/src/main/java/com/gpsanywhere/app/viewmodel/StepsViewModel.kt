package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gpsanywhere.app.service.SpoofService

class StepsViewModel(application: Application) : AndroidViewModel(application) {

    val steps: LiveData<Int> = SpoofService.stepCount

    fun increment(by: Int) {
        SpoofService.incrementSteps(by)
    }

    fun reset() {
        SpoofService.resetSteps()
    }
}

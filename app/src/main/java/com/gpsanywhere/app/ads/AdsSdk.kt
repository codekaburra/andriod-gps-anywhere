package com.gpsanywhere.app.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds

private const val TAG = "AdsSdk"

/**
 * One-shot initialisation of the Mobile Ads SDK, shared by every ad format.
 *
 * A load issued before [MobileAds.initialize] reports back is dropped in
 * silence — neither the success nor the failure callback fires — so every
 * request has to queue behind this rather than race it.
 *
 * Main thread only. The state and the queue are deliberately unsynchronised:
 * composition and the SDK's completion listener both run there, and a lock
 * would only hide a call from somewhere that should not be calling.
 */
object AdsSdk {

    private enum class State { IDLE, STARTING, READY }

    private var state = State.IDLE
    private val pending = mutableListOf<() -> Unit>()

    /**
     * Runs [action] once the SDK is usable — immediately if it already is,
     * otherwise when initialisation completes.
     */
    fun whenReady(context: Context, action: () -> Unit) {
        when (state) {
            State.READY -> action()
            State.STARTING -> pending += action
            State.IDLE -> {
                state = State.STARTING
                pending += action
                MobileAds.initialize(context.applicationContext) { status ->
                    status.adapterStatusMap.forEach { (adapter, adapterState) ->
                        Log.i(TAG, "adapter $adapter: ${adapterState.initializationState} ${adapterState.description}")
                    }
                    state = State.READY
                    // Copied before running: an action may enqueue the next load.
                    val queued = pending.toList()
                    pending.clear()
                    queued.forEach { it() }
                }
            }
        }
    }
}

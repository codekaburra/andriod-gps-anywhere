package com.gpsanywhere.app.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.gpsanywhere.app.BuildConfig

private const val TAG = "InterstitialAds"

/**
 * The shortest gap allowed between two interstitials.
 *
 * Saving a location takes seconds, so without a floor a user adding a handful
 * in a row would get a full-screen ad after each one. That is the pattern
 * AdMob treats as accidental-click bait, and the penalty lands on the account
 * rather than on the build.
 */
private const val MIN_GAP_MS = 60_000L

/**
 * The single interstitial slot, shared by every call site.
 *
 * One ad is kept loaded ahead of time because [InterstitialAd.load] takes long
 * enough that requesting one at the moment of the tap would show nothing. After
 * an ad is shown or fails, the next is fetched straight away.
 *
 * Main thread only, like [AdsSdk].
 */
object InterstitialAds {

    private var ad: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L

    /** Google's sample unit in debug, the real one in release. See build.gradle.kts. */
    private val unitId: String get() = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID

    /** Fetches the next ad unless one is already loaded or in flight. */
    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true

        // The application context, not the activity: this object outlives any
        // screen, and an InterstitialAd held here would pin the activity.
        val appContext = context.applicationContext

        AdsSdk.whenReady(appContext) {
            InterstitialAd.load(
                appContext,
                unitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(loaded: InterstitialAd) {
                        loading = false
                        ad = loaded
                        Log.i(TAG, "loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading = false
                        ad = null
                        Log.w(TAG, "failed: code=${error.code} ${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Shows the waiting ad, if there is one and [MIN_GAP_MS] has passed since
     * the last.
     *
     * Returns without doing anything when no ad is ready — the save that
     * triggered this has already happened, and blocking on a network fetch to
     * show an ad would punish the user for a slow request.
     */
    fun show(activity: Activity) {
        val ready = ad
        if (ready == null) {
            preload(activity)
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (lastShownAt != 0L && now - lastShownAt < MIN_GAP_MS) {
            Log.i(TAG, "held back, ${(MIN_GAP_MS - (now - lastShownAt)) / 1000}s of the gap left")
            return
        }

        // Cleared before show(), so a second tap cannot present the same ad twice.
        ad = null
        lastShownAt = now

        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "show failed: code=${error.code} ${error.message}")
                preload(activity)
            }
        }
        ready.show(activity)
    }
}

/**
 * Keeps an interstitial loaded for this screen and returns the call to make
 * once the user's action has completed.
 *
 * Invoke it *after* the save, not instead of it: the ad is a transition over
 * work that already happened, so a failed or rate-limited ad costs nothing.
 *
 * A no-op in Compose previews, where there is no SDK and no activity.
 */
@Composable
fun rememberInterstitialAd(): () -> Unit {
    if (LocalInspectionMode.current) return {}

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(Unit) { InterstitialAds.preload(context) }

    return remember(activity) {
        { activity?.let { InterstitialAds.show(it) } }
    }
}

/** Unwraps the themed/wrapped context Compose hands out down to the hosting activity. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

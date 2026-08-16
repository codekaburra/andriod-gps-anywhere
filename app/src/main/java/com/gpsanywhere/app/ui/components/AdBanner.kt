package com.gpsanywhere.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.gpsanywhere.app.BuildConfig

private const val TAG = "AdBanner"

/**
 * Supplied per build type from build.gradle.kts: Google's sample unit in debug,
 * the real one in release. A debug build therefore cannot request live ads from
 * a development device, which is the traffic AdMob suspends accounts over.
 */
private val BANNER_UNIT_ID: String get() = BuildConfig.ADMOB_BANNER_UNIT_ID

/**
 * The banner at the top of every screen.
 *
 * The AdView is created only after [MobileAds.initialize] reports back. Building
 * it during composition looks equivalent but is not: an AndroidView factory runs
 * while the tree is being composed, whereas a LaunchedEffect runs after, so
 * loadAd() went out roughly a third of a second before the SDK was initialised
 * and the request was dropped without either callback firing.
 *
 * The slot reserves [AdSize.height] from the start, so the screen below does not
 * jump when an ad arrives, and an empty slot looks deliberate rather than broken.
 *
 * Renders nothing in Compose previews, where there is no ad SDK to talk to.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = BANNER_UNIT_ID
) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp

    var initialised by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        MobileAds.initialize(context.applicationContext) { status ->
            status.adapterStatusMap.forEach { (adapter, state) ->
                Log.i(TAG, "adapter $adapter: ${state.initializationState} ${state.description}")
            }
            initialised = true
        }
    }

    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!initialised) return@Box

        val adView = remember(adUnitId, adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.i(TAG, "loaded ${adSize.width}x${adSize.height}")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "failed: code=${error.code} ${error.message}")
                    }
                }
            }
        }

        // An AdView holds a WebView; leaving it attached leaks the activity.
        DisposableEffect(adView) {
            adView.loadAd(AdRequest.Builder().build())
            onDispose { adView.destroy() }
        }

        AndroidView(modifier = Modifier.fillMaxWidth(), factory = { adView })
    }
}

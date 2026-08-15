package com.gpsanywhere.app.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AdBanner"

/**
 * Google's own sample banner unit. It always fills, never earns, and is the only
 * id that may be used while developing — requesting live ads from a device you
 * are testing on is what gets an AdMob account suspended.
 *
 * Replace with the real unit id from the AdMob console before release, keeping
 * this one for debug builds.
 */
private const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/** MobileAds.initialize is safe to call twice but only does work once. */
private val adsInitialised = AtomicBoolean(false)

private fun ensureInitialised(context: Context) {
    if (adsInitialised.compareAndSet(false, true)) {
        MobileAds.initialize(context.applicationContext)
    }
}

/**
 * An anchored adaptive banner, sized to the window width rather than the fixed
 * 320x50 slot — Google serves a taller, better-filling creative for it and has
 * deprecated the fixed sizes.
 *
 * The slot reserves [AdSize.height] up front. Without an explicit height the
 * AndroidView measures to zero before the first ad arrives, and a banner that
 * fills later has nowhere to draw. Reserving it also stops the rest of the UI
 * jumping down the moment an ad loads.
 *
 * Renders nothing in Compose previews, where there is no ad SDK to talk to.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_UNIT_ID
) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp

    LaunchedEffect(Unit) { ensureInitialised(context) }

    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
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
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

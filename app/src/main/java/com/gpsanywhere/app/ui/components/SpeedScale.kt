package com.gpsanywhere.app.ui.components

import com.gpsanywhere.app.viewmodel.LocationViewModel.Companion.MAX_SPEED_KMH

/**
 * Non-linear speed <-> slider mapping: the slow "walking" zone (0–20 km/h) occupies
 * the first 80% of the slider so fine control stays usable, while the remaining 20%
 * spans the much larger fast range up to [MAX_SPEED_KMH].
 */
const val WALK_ZONE_KMH = 20f
const val WALK_ZONE_FRAC = 0.8f

fun speedToSlider(kmh: Float, maxKmh: Float = MAX_SPEED_KMH): Float {
    return if (kmh <= WALK_ZONE_KMH) {
        (kmh / WALK_ZONE_KMH) * WALK_ZONE_FRAC
    } else {
        WALK_ZONE_FRAC + ((kmh - WALK_ZONE_KMH) / (maxKmh - WALK_ZONE_KMH)) * (1f - WALK_ZONE_FRAC)
    }
}

fun sliderToSpeed(frac: Float, maxKmh: Float = MAX_SPEED_KMH): Float {
    return if (frac <= WALK_ZONE_FRAC) {
        (frac / WALK_ZONE_FRAC) * WALK_ZONE_KMH
    } else {
        WALK_ZONE_KMH + ((frac - WALK_ZONE_FRAC) / (1f - WALK_ZONE_FRAC)) * (maxKmh - WALK_ZONE_KMH)
    }
}

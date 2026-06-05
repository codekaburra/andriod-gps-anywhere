package com.gpsanywhere.app.routes

/**
 * Generates individual step-level waypoints for an outward square spiral walk.
 *
 * Each waypoint is exactly [stepDeg] degrees from the previous one, so the walk
 * engine moves visibly at every tick rather than interpolating across huge segments.
 *
 * Pattern (move group i, 0-based):
 *   direction  = i % 4  →  East, South, West, North
 *   step count = i / 2 + 1
 *
 * Total waypoints for [rings] rings:
 *   1 (center) + 2 × (1 + 2 + … + 2×rings) = 1 + 2×rings×(2×rings+1)
 *
 * Example with stepDeg=0.0002, center (22.3166, 114.0453):
 *   (22.3166, 114.0453) ← center
 *   (22.3166, 114.0455) ← E step 1
 *   (22.3164, 114.0455) ← S step 1
 *   (22.3164, 114.0453) ← W step 1
 *   (22.3164, 114.0451) ← W step 2
 *   (22.3166, 114.0451) ← N step 1
 *   (22.3168, 114.0451) ← N step 2
 *   …
 */
object SpiralWalkGenerator {

    /**
     * @param centerLat  Starting latitude  (degrees)
     * @param centerLng  Starting longitude (degrees)
     * @param stepDeg    Size of one step   (degrees). Default 0.0002 ≈ 22 m.
     * @param rings      Number of full spiral rings.
     * @return Pair of (lats, lngs) DoubleArrays — one entry per GPS step.
     */
    fun generate(
        centerLat: Double,
        centerLng: Double,
        stepDeg: Double = 0.0002,
        rings: Int = 15
    ): Pair<DoubleArray, DoubleArray> {
        val lats = mutableListOf(centerLat)
        val lngs = mutableListOf(centerLng)

        var lat = centerLat
        var lng = centerLng

        for (i in 0 until rings * 4) {
            val numSteps = i / 2 + 1
            val dLat: Double
            val dLng: Double
            when (i % 4) {
                0    -> { dLat = 0.0;     dLng = stepDeg  }  // East
                1    -> { dLat = -stepDeg; dLng = 0.0     }  // South
                2    -> { dLat = 0.0;     dLng = -stepDeg }  // West
                else -> { dLat = stepDeg; dLng = 0.0      }  // North
            }
            repeat(numSteps) {
                lat += dLat
                lng += dLng
                lats.add(lat)
                lngs.add(lng)
            }
        }

        return lats.toDoubleArray() to lngs.toDoubleArray()
    }
}

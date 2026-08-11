package com.gpsanywhere.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Procedural app background: soft colour blooms over a flat base, finished with a
 * faint grain.
 *
 * Every tone is derived from [base], so changing the theme colour keeps the
 * texture in tune — unlike a bitmap, which has to be re-authored per theme and
 * shipped in the APK at every density.
 */
@Composable
fun TexturedBackground(
    base: Color,
    modifier: Modifier = Modifier,
    /**
     * Bloom tints, painted in order. Pass palette colours to keep the wash on
     * brand; leave empty to derive light/warm/dark variants from [base].
     */
    accents: List<Color> = emptyList(),
    /** Strength of the grain. Keep it low; this sits under the whole UI. */
    grainAlpha: Float = 0.05f,
    /** How strongly each bloom tints the base. */
    bloomAlpha: Float = 0.55f
) {
    // Fixed seed so the grain doesn't reshuffle on every recomposition. Points are
    // normalised (0..1) and scaled at draw time, so they survive rotation/resize.
    val grain = remember {
        val rng = Random(seed = 0x6A5D)
        List(GRAIN_DOTS) { Grain(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) }
    }

    val tints = accents.ifEmpty {
        listOf(
            lerp(base, Color.White, 0.38f),
            lerp(base, Color(0xFFF0D9B5), 0.45f),
            lerp(base, Color.Black, 0.16f),
            lerp(base, Color.White, 0.38f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(base)

        // Overlapping blooms read as one soft wash rather than distinct circles.
        val span = size.minDimension
        BLOOM_SPOTS.forEachIndexed { i, (fx, fy, scale) ->
            bloom(
                color = tints[i % tints.size],
                center = Offset(size.width * fx, size.height * fy),
                radius = span * scale,
                alpha = bloomAlpha
            )
        }

        val dotRadius = 0.9.dp.toPx()
        grain.forEach { (fx, fy, fv) ->
            drawCircle(
                color = if (fv > 0.5f) Color.White else Color.Black,
                radius = dotRadius * (0.6f + fv * 0.8f),
                center = Offset(fx * size.width, fy * size.height),
                alpha = grainAlpha * (0.35f + fv * 0.65f)
            )
        }
    }
}

private const val GRAIN_DOTS = 900

/** Bloom placement as fractions of the canvas: x, y, and radius vs. the short edge. */
private val BLOOM_SPOTS = listOf(
    Triple(0.16f, 0.10f, 1.20f),
    Triple(0.95f, 0.28f, 1.00f),
    Triple(0.72f, 0.86f, 1.10f),
    Triple(0.02f, 0.72f, 0.85f)
)

private data class Grain(val x: Float, val y: Float, val v: Float)

private fun DrawScope.bloom(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        )
    )
}


package com.gpsanywhere.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gpsanywhere.app.routes.LocationPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapViewComposable(
    modifier: Modifier = Modifier,
    center: GeoPoint? = null,
    zoom: Double = 14.0,
    waypoints: List<LocationPoint> = emptyList(),
    showNumberedPins: Boolean = false,
    onMapClick: ((LocationPoint) -> Unit)? = null,
    onMapReady: ((MapView) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            center?.let { controller.setCenter(it) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    DisposableEffect(center, waypoints, showNumberedPins) {
        mapView.overlays.clear()

        if (onMapClick != null) {
            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let {
                        onMapClick(LocationPoint(it.latitude, it.longitude))
                    }
                    return true
                }
                override fun longPressHelper(p: GeoPoint?): Boolean = false
            })
            mapView.overlays.add(eventsOverlay)
        }

        waypoints.forEachIndexed { index, point ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(point.latitude, point.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (showNumberedPins) {
                    icon = createTeardropPin(context, com.gpsanywhere.app.ui.theme.MapPolyline, index + 1)
                    title = point.name?.takeIf { it.isNotBlank() }?.let { "${index + 1}. $it" }
                        ?: "${index + 1}"
                    snippet = "${point.latitude}, ${point.longitude}"
                } else {
                    icon = androidx.core.content.ContextCompat
                        .getDrawable(context, com.gpsanywhere.app.R.drawable.ic_pin_drop)
                        ?.mutate()
                    title = point.name
                }
            }
            mapView.overlays.add(marker)
        }

        if (waypoints.size >= 2) {
            val polyline = Polyline(mapView).apply {
                setPoints(waypoints.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.strokeWidth = 8f
                outlinePaint.color = com.gpsanywhere.app.ui.theme.MapPolyline
            }
            mapView.overlays.add(polyline)
        } else if (waypoints.size == 1) {
            mapView.controller.setCenter(GeoPoint(waypoints[0].latitude, waypoints[0].longitude))
        }

        mapView.invalidate()
        onDispose { }
    }

    AndroidView(
        factory = {
            onMapReady?.invoke(mapView)
            mapView
        },
        modifier = modifier,
        update = { view ->
            center?.let { view.controller.setCenter(it) }
        }
    )
}

private fun createTeardropPin(
    context: android.content.Context,
    pinColor: Int,
    number: Int?
): Drawable {
    val density = context.resources.displayMetrics.density
    val w = (36 * density).toInt()
    val h = (52 * density).toInt()
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = w / 2f
    val headRadius = w / 2f - 2 * density
    val headCy = headRadius + 2 * density
    val tipY = h - 2 * density

    // Smooth teardrop: circular head that tapers into a sharp bottom tip
    val path = android.graphics.Path().apply {
        val dx = headRadius * 0.78f
        moveTo(cx, tipY)
        cubicTo(
            cx - dx, headCy + headRadius * 0.62f,
            cx - headRadius, headCy + headRadius * 0.28f,
            cx - headRadius, headCy
        )
        arcTo(
            android.graphics.RectF(cx - headRadius, headCy - headRadius, cx + headRadius, headCy + headRadius),
            180f, -180f, false
        )
        cubicTo(
            cx + headRadius, headCy + headRadius * 0.28f,
            cx + dx, headCy + headRadius * 0.62f,
            cx, tipY
        )
        close()
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, bodyPaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, headCy, headRadius * 0.5f, innerPaint)

    if (number != null) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            textSize = headRadius * 0.7f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(number.toString(), cx, headCy + textPaint.textSize / 3f, textPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

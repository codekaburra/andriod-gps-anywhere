package com.gpsanywhere.app.ui.components

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
    center: GeoPoint = GeoPoint(25.0330, 121.5654),
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
            controller.setCenter(center)
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
                    title = "${index + 1}"
                    snippet = "${point.latitude}, ${point.longitude}"
                }
            }
            mapView.overlays.add(marker)
        }

        if (waypoints.size >= 2) {
            val polyline = Polyline(mapView).apply {
                setPoints(waypoints.map { GeoPoint(it.latitude, it.longitude) })
                outlinePaint.strokeWidth = 8f
                outlinePaint.color = android.graphics.Color.parseColor("#22D3EE")
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
            view.controller.setCenter(center)
        }
    )
}

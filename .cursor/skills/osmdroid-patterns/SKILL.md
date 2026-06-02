---
name: osmdroid-patterns
description: OSMDroid-specific patterns for the GPS Anywhere project. Covers how to use MapViewComposable, draw polylines, add numbered markers, generate static map thumbnails, configure tile caching, and avoid common performance mistakes. Use when implementing map features, adding overlays, working with route previews, or debugging OSMDroid in Compose.
---

# OSMDroid Patterns — GPS Anywhere

## Always Use MapViewComposable

Never instantiate `MapView` directly in screens. Always use the wrapper:

```kotlin
MapViewComposable(
    modifier = Modifier.fillMaxWidth().weight(0.55f),
    center = GeoPoint(lat, lng),        // current map center
    waypoints = listOf(point1, point2), // draws markers + polyline
    showNumberedPins = true,            // shows "1", "2", "3" labels
    onMapClick = { point -> },          // null = read-only map
    onMapReady = { mapView -> }         // optional: access raw MapView
)
```

Located at: `ui/components/MapViewComposable.kt`

## Polyline Style

The default polyline in `MapViewComposable` is a solid blue line (width 8f, `#3B82F6`).

To draw a **dashed** polyline (e.g. for route preview), access via `onMapReady`:

```kotlin
onMapReady = { mapView ->
    val polyline = Polyline(mapView).apply {
        setPoints(waypoints.map { GeoPoint(it.latitude, it.longitude) })
        outlinePaint.strokeWidth = 6f
        outlinePaint.color = Color.parseColor("#3B82F6")
        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(30f, 15f), 0f)
    }
    mapView.overlays.add(polyline)
    mapView.invalidate()
}
```

## Custom Numbered Markers

When `showNumberedPins = true`, the wrapper adds a text title but uses the default OSMDroid marker icon. For custom colored numbered pins, use `onMapReady`:

```kotlin
val marker = Marker(mapView).apply {
    position = GeoPoint(point.latitude, point.longitude)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    title = "${index + 1}"
    // To use a custom drawable: icon = ContextCompat.getDrawable(context, R.drawable.pin_numbered)
}
mapView.overlays.add(marker)
```

## Static Map Thumbnail (for SavedRoutesScreen)

Do NOT put a live `MapView` in each `LazyColumn` row — it will cause severe performance issues.

Instead, render a bitmap snapshot off-screen:

```kotlin
fun renderRouteThumbnail(
    context: Context,
    waypoints: List<LocationPoint>,
    widthPx: Int = 200,
    heightPx: Int = 150
): Bitmap {
    val mapView = MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
        measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, widthPx, heightPx)
    }
    // Add polyline overlay
    if (waypoints.size >= 2) {
        val polyline = Polyline(mapView).apply {
            setPoints(waypoints.map { GeoPoint(it.latitude, it.longitude) })
        }
        mapView.overlays.add(polyline)
        val bounds = BoundingBox.fromGeoPoints(waypoints.map { GeoPoint(it.latitude, it.longitude) })
        mapView.zoomToBoundingBox(bounds, false, 20)
    }
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    mapView.draw(canvas)
    return bitmap
}
```

Call this in a coroutine on `Dispatchers.Default`. Cache by route ID in the ViewModel.

## Tile Caching

OSMDroid caches tiles to disk automatically. To configure cache location and size:

```kotlin
// In Application.onCreate() or MainActivity:
val config = Configuration.getInstance()
config.userAgentValue = packageName
config.osmdroidTileCache = File(cacheDir, "osm_tiles")
config.tileDownloadMaxQueueSize = 4  // limit on old devices
```

## Performance Rules

- **One map per screen** — never in `LazyColumn`, `LazyRow`, or `RecyclerView`
- **Always call** `mapView.onResume()` / `mapView.onPause()` — handled by `DisposableEffect` in `MapViewComposable`
- **Clear overlays** before re-adding: `mapView.overlays.clear()` then `mapView.invalidate()`
- For old devices (Android 7–8): set `mapView.setHardwareAccelerated(false)` if tiles flicker

## Coordinate System

OSMDroid uses `GeoPoint(latitude, longitude)` — same order as GPS (lat first).
Project model uses `LocationPoint(latitude, longitude)` — same order.

Convert: `GeoPoint(point.latitude, point.longitude)`

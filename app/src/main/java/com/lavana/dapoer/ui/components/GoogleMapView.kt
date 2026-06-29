package com.lavana.dapoer.ui.components

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.lavana.dapoer.ui.screens.LatLng
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    center: LatLng,
    markers: List<Pair<LatLng, String>> = emptyList(),
    onMapClick: ((LatLng) -> Unit)? = null,
    zoom: Float = 15f
) {
    val context = LocalContext.current
    val currentOnMapClick by rememberUpdatedState(onMapClick)

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            // Prevent parent scroll container from stealing touch events from map
            setOnTouchListener { view, event ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Manage MapEventsOverlay dynamically to avoid stale lambda captures
    DisposableEffect(mapView) {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                currentOnMapClick?.invoke(LatLng(p.latitude, p.longitude))
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        val overlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(0, overlay)
        onDispose {
            mapView.overlays.remove(overlay)
        }
    }

    AndroidView(
        factory = {
            mapView
        },
        update = { map ->
            map.overlays.removeIf { it is Marker }
            markers.forEach { (latLng, title) ->
                val marker = Marker(map).apply {
                    position = GeoPoint(latLng.latitude, latLng.longitude)
                    this.title = title
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
            }
            map.controller.setZoom(zoom.toDouble())
            map.controller.setCenter(GeoPoint(center.latitude, center.longitude))
            map.invalidate()
        },
        modifier = modifier
    )
}


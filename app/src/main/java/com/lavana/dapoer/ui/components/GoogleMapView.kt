package com.lavana.dapoer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as GLatLng
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.lavana.dapoer.ui.screens.LatLng

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    center: LatLng,
    markers: List<Pair<LatLng, String>> = emptyList(),
    onMapClick: ((LatLng) -> Unit)? = null,
    onMarkerDragEnd: ((LatLng) -> Unit)? = null,
    zoom: Float = 15f
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            GLatLng(center.latitude, center.longitude),
            zoom
        )
    }

    LaunchedEffect(center, zoom) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            GLatLng(center.latitude, center.longitude),
            zoom
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false
        ),
        onMapClick = { gLatLng ->
            onMapClick?.invoke(LatLng(gLatLng.latitude, gLatLng.longitude))
        }
    ) {
        markers.forEachIndexed { index, pair ->
            val latLng = pair.first
            val title = pair.second
            // Marker pertama bisa digeser jika onMarkerDragEnd disediakan (pemilih lokasi alamat).
            val draggable = onMarkerDragEnd != null && index == 0

            // rememberMarkerState hanya menerapkan posisi saat komposisi pertama, sehingga marker
            // yang bergerak (live tracking kurir) akan diam di tempat. Hoist MarkerState yang stabil
            // lalu dorong posisi terbaru setiap koordinat berubah.
            val markerState = remember { MarkerState(position = GLatLng(latLng.latitude, latLng.longitude)) }

            // Sinkronkan posisi dari data — TAPI jangan saat sedang digeser agar tidak melawan drag.
            LaunchedEffect(latLng.latitude, latLng.longitude) {
                if (markerState.dragState != DragState.DRAG) {
                    markerState.position = GLatLng(latLng.latitude, latLng.longitude)
                }
            }

            // Laporkan posisi akhir saat selesai digeser.
            if (draggable) {
                LaunchedEffect(markerState) {
                    snapshotFlow { markerState.dragState }
                        .collect { state ->
                            if (state == DragState.END) {
                                val p = markerState.position
                                onMarkerDragEnd?.invoke(LatLng(p.latitude, p.longitude))
                            }
                        }
                }
            }

            Marker(
                state = markerState,
                title = title,
                draggable = draggable
            )
        }
    }
}

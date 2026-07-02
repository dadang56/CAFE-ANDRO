package com.lavana.dapoer.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.lavana.dapoer.data.DELIVERY_DISTANCE_TOLERANCE_METERS
import com.lavana.dapoer.data.Order
import com.lavana.dapoer.data.SupabaseClient
import com.lavana.dapoer.data.distanceInMeters
import com.lavana.dapoer.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Dialog konfirmasi driver sampai lokasi: wajib foto bukti, sistem cek jarak GPS
 * driver terhadap titik alamat pesanan (toleransi [DELIVERY_DISTANCE_TOLERANCE_METERS]).
 * Jika jarak melebihi toleransi, pengantaran TETAP bisa diselesaikan (tidak diblokir)
 * namun ditandai delivery_within_tolerance=false agar admin bisa mengecek manual.
 */
@Composable
fun DeliveryConfirmDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            photoUri = null
            photoFile = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true
        if (cameraGranted) {
            val file = File(context.cacheDir, "delivery_proof_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoFile = file
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk foto bukti pengantaran.", Toast.LENGTH_LONG).show()
        }
    }

    fun launchCamera() {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            val file = File(context.cacheDir, "delivery_proof_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoFile = file
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text("Konfirmasi Sampai Lokasi", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Ambil foto bukti di lokasi pelanggan. Sistem akan mengecek jarak GPS Anda terhadap alamat pesanan.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto bukti",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { launchCamera() },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                        border = BorderStroke(1.dp, OrangeJco),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ambil Ulang Foto", fontSize = 12.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightOrangeJco)
                            .clickable(enabled = !isSubmitting) { launchCamera() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ketuk untuk ambil foto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OrangeJco)
                        }
                    }
                }

                if (isSubmitting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator(color = OrangeJco, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(statusText, fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (photoFile == null || photoUri == null) {
                        Toast.makeText(context, "Ambil foto bukti terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    coroutineScope.launch {
                        try {
                            statusText = "Mengecek lokasi GPS..."
                            val currentLoc = withTimeoutOrNull(10_000) { getSingleLocation(context) }

                            var distanceMeters: Double? = null
                            var withinTolerance: Boolean? = null
                            val destParts = order.coordinates?.split(",")
                            val destLat = destParts?.getOrNull(0)?.toDoubleOrNull()
                            val destLon = destParts?.getOrNull(1)?.toDoubleOrNull()
                            if (currentLoc != null && destLat != null && destLon != null) {
                                distanceMeters = distanceInMeters(
                                    LatLng(currentLoc.latitude, currentLoc.longitude),
                                    LatLng(destLat, destLon)
                                )
                                withinTolerance = distanceMeters <= DELIVERY_DISTANCE_TOLERANCE_METERS
                            }

                            statusText = "Mengunggah foto bukti..."
                            val bytes = withContext(Dispatchers.IO) { photoFile!!.readBytes() }
                            val fileName = "delivery-proof/${order.id}_${UUID.randomUUID()}.jpg"
                            val bucket = SupabaseClient.storage["menu-images"]
                            bucket.upload(fileName, bytes, upsert = true)
                            val proofUrl = bucket.publicUrl(fileName)

                            statusText = "Menyimpan konfirmasi..."
                            SupabaseClient.db["orders"].update({
                                set("status", "Selesai")
                                set("payment_status", "Terbayar")
                                set("delivery_proof_url", proofUrl)
                                if (distanceMeters != null) set("delivery_distance_meters", distanceMeters)
                                if (withinTolerance != null) set("delivery_within_tolerance", withinTolerance)
                            }) { filter { eq("id", order.id ?: "") } }

                            isSubmitting = false
                            if (withinTolerance == false) {
                                Toast.makeText(
                                    context,
                                    "Pengantaran selesai. Jarak ~${distanceMeters?.toInt()}m melebihi toleransi -- ditandai untuk dicek admin.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Pengantaran berhasil dikonfirmasi!", Toast.LENGTH_LONG).show()
                            }
                            onConfirmed()
                            onDismiss()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isSubmitting = false
                            Toast.makeText(context, "Gagal konfirmasi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnAccentDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kirim Konfirmasi", color = OnAccentDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }, enabled = !isSubmitting) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}

private suspend fun getSingleLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    try {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, context.mainLooper)
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, context.mainLooper)
            else -> {
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                cont.resume(lastKnown)
                return@suspendCancellableCoroutine
            }
        }

        cont.invokeOnCancellation {
            try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
        }
    } catch (e: Exception) {
        e.printStackTrace()
        if (cont.isActive) cont.resume(null)
    }
}

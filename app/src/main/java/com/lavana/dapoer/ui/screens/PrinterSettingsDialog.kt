package com.lavana.dapoer.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavana.dapoer.data.PrinterDevice
import com.lavana.dapoer.data.PrinterManager
import com.lavana.dapoer.ui.theme.ForestGreen
import com.lavana.dapoer.ui.theme.OrangeJco
import kotlinx.coroutines.launch

@Composable
fun PrinterSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var devices by remember { mutableStateOf<List<PrinterDevice>>(emptyList()) }
    var selectedMac by remember { mutableStateOf(PrinterManager.getSavedPrinter(context)?.mac) }
    var isPrinting by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        )
    }

    fun loadDevices() {
        devices = PrinterManager.getBondedPrinters(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            loadDevices()
        } else {
            Toast.makeText(
                context,
                "Izin Bluetooth diperlukan untuk menampilkan printer",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Minta izin BLUETOOTH_CONNECT (API 31+) lalu muat daftar printer
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission = true
            loadDevices()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Pengaturan Printer Thermal (58mm)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ForestGreen
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (devices.isEmpty()) {
                    Text(
                        "Pasangkan (pair) printer Bluetooth Anda lewat Pengaturan Android terlebih dahulu.",
                        fontSize = 13.sp,
                        color = ForestGreen.copy(alpha = 0.8f)
                    )
                    if (!hasPermission) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Izin Bluetooth belum diberikan.",
                            fontSize = 12.sp,
                            color = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Text(
                        "Pilih printer yang akan digunakan:",
                        fontSize = 13.sp,
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                    devices.forEach { device ->
                        val isSelected = device.mac == selectedMac
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) OrangeJco.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) OrangeJco else ForestGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        selectedMac = device.mac
                                        PrinterManager.savePrinter(context, device)
                                        Toast.makeText(
                                            context,
                                            "Printer tersimpan",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedMac = device.mac
                                    PrinterManager.savePrinter(context, device)
                                    Toast.makeText(
                                        context,
                                        "Printer tersimpan",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = OrangeJco,
                                    unselectedColor = ForestGreen.copy(alpha = 0.5f)
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    device.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen
                                )
                                Text(
                                    device.mac,
                                    fontSize = 11.sp,
                                    color = ForestGreen.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (isPrinting) return@Button
                            isPrinting = true
                            scope.launch {
                                val testReceipt = buildTestReceipt()
                                val result = PrinterManager.printText(context, testReceipt)
                                isPrinting = false
                                result.fold(
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Berhasil mencetak struk uji",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(
                                            context,
                                            "Gagal mencetak: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPrinting && selectedMac != null,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            if (isPrinting) "Mencetak..." else "Test Cetak",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = ForestGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

/** Bangun struk uji singkat (32 karakter per baris). */
private fun buildTestReceipt(): String {
    val sb = StringBuilder()
    sb.appendLine(PrinterManager.lineCenter("DAPOER LAVANA"))
    sb.appendLine(PrinterManager.lineCenter("Test Cetak Printer"))
    sb.appendLine("--------------------------------")
    sb.appendLine(PrinterManager.lineLeftRight("Status", "OK"))
    sb.appendLine(PrinterManager.lineLeftRight("Lebar", "58mm / 32 kar"))
    sb.appendLine("--------------------------------")
    sb.appendLine(PrinterManager.lineCenter("Printer siap digunakan"))
    return sb.toString()
}

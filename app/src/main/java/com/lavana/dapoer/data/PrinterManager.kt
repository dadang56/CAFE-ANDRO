package com.lavana.dapoer.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Representasi printer Bluetooth yang sudah ter-pairing.
 */
data class PrinterDevice(val name: String, val mac: String)

/**
 * Manajer printer thermal Bluetooth (58mm, ESC/POS via RFCOMM SPP).
 *
 * 58mm = 32 karakter per baris (Font A). Pemanggil mengirim teks yang sudah
 * diformat; PrinterManager hanya mencetak apa adanya.
 */
object PrinterManager {

    private const val PREFS_NAME = "printer_prefs"
    private const val KEY_MAC = "printer_mac"
    private const val KEY_NAME = "printer_name"

    /** Lebar kertas 58mm = 32 karakter per baris (Font A). */
    const val LINE_WIDTH = 32

    /** UUID standar untuk Serial Port Profile (SPP). */
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS commands
    private val CMD_INIT = byteArrayOf(0x1B, 0x40)                       // ESC @  (initialize)
    private val CMD_FEED = byteArrayOf(0x0A, 0x0A, 0x0A)                 // 3x line feed
    private val CMD_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00)           // GS V B 0 (partial cut)

    /**
     * Daftar printer Bluetooth yang sudah ter-pairing (bonded).
     * Mengembalikan list kosong bila Bluetooth tidak tersedia atau izin ditolak.
     */
    fun getBondedPrinters(context: Context): List<PrinterDevice> {
        val adapter = getAdapter(context) ?: return emptyList()
        return try {
            adapter.bondedDevices?.map { device ->
                PrinterDevice(
                    name = device.name ?: "Unknown",
                    mac = device.address
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            // Izin BLUETOOTH_CONNECT (API 31+) ditolak
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Ambil printer yang tersimpan, atau null bila belum ada. */
    fun getSavedPrinter(context: Context): PrinterDevice? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mac = prefs.getString(KEY_MAC, null) ?: return null
        val name = prefs.getString(KEY_NAME, null) ?: "Unknown"
        return PrinterDevice(name, mac)
    }

    /** Simpan printer terpilih ke SharedPreferences. */
    fun savePrinter(context: Context, device: PrinterDevice) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAC, device.mac)
            .putString(KEY_NAME, device.name)
            .apply()
    }

    /** Hapus printer tersimpan. */
    fun clearSavedPrinter(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MAC)
            .remove(KEY_NAME)
            .apply()
    }

    /**
     * Cetak teks ke printer thermal tersimpan via Bluetooth RFCOMM (ESC/POS).
     * Berjalan di Dispatchers.IO. Mengembalikan Result.success bila berhasil.
     */
    suspend fun printText(context: Context, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        val saved = getSavedPrinter(context)
            ?: return@withContext Result.failure(Exception("Belum ada printer tersimpan"))

        val adapter = getAdapter(context)
            ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia di perangkat ini"))

        var socket: BluetoothSocket? = null
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(saved.mac)

            // Hentikan discovery agar koneksi lebih cepat & stabil
            try {
                adapter.cancelDiscovery()
            } catch (e: SecurityException) {
                // abaikan, lanjut mencoba konek
            }

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            val out = socket.outputStream
            out.write(CMD_INIT)
            out.write(encode(content))
            out.write(CMD_FEED)
            out.write(CMD_CUT)
            out.flush()

            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("Izin Bluetooth ditolak. Aktifkan izin lalu coba lagi."))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // abaikan error saat menutup socket
            }
        }
    }

    /**
     * Encode teks ke byte untuk printer thermal.
     * Pakai US-ASCII (cocok untuk struk standar), fallback ke ISO-8859-1.
     */
    private fun encode(content: String): ByteArray {
        return try {
            content.toByteArray(Charsets.US_ASCII)
        } catch (e: Exception) {
            content.toByteArray(Charsets.ISO_8859_1)
        }
    }

    /**
     * Helper opsional: format satu baris dengan label di kiri dan nilai di kanan
     * agar pas pada lebar 58mm (32 karakter).
     */
    fun lineLeftRight(left: String, right: String, width: Int = LINE_WIDTH): String {
        val space = width - left.length - right.length
        return if (space > 0) {
            left + " ".repeat(space) + right
        } else {
            // Bila terlalu panjang, potong bagian kiri
            val maxLeft = (width - right.length - 1).coerceAtLeast(0)
            left.take(maxLeft) + " " + right
        }
    }

    /** Helper opsional: rata tengah teks pada lebar 58mm. */
    fun lineCenter(text: String, width: Int = LINE_WIDTH): String {
        if (text.length >= width) return text
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun getAdapter(context: Context): BluetoothAdapter? {
        return try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            manager?.adapter
        } catch (e: Exception) {
            null
        }
    }
}

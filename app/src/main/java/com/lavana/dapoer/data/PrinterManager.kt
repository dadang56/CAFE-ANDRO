package com.lavana.dapoer.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Representasi printer Bluetooth yang sudah ter-pairing.
 */
data class PrinterDevice(val name: String, val mac: String)

/** Perataan teks pada struk. */
enum class ReceiptAlign { LEFT, CENTER, RIGHT }

/** Satu baris/elemen pada struk yang bisa diberi gaya (bold, rata tengah, dst). */
sealed class ReceiptElement {
    data class TextLine(
        val text: String,
        val bold: Boolean = false,
        val align: ReceiptAlign = ReceiptAlign.LEFT,
        val doubleHeight: Boolean = false
    ) : ReceiptElement()
    data class Logo(val bitmap: Bitmap) : ReceiptElement()
    object Divider : ReceiptElement()
    object Spacer : ReceiptElement()
}

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

    /** Lebar kertas 58mm pada Font A = 32 karakter per baris. Dipakai jalur
     *  printText() polos (test print & cetak laporan). */
    const val LINE_WIDTH = 32

    /**
     * Lebar untuk STRUK (printReceipt) yang memakai Font B (font kecil/rapat):
     * ~42 karakter per baris pada 58mm (384 dot / 9 dot per char), vs 32 di
     * Font A. Font B dipilih agar baris panjang (mis. "Kasir : Admin Demo
     * (Dapoer Lavana)", "Metode: QRIS (Menunggu Verifikasi)") muat dalam satu
     * baris -- tidak turun/terpotong ke baris kedua.
     */
    const val RECEIPT_WIDTH = 42

    /** UUID standar untuk Serial Port Profile (SPP). */
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS commands
    private val CMD_INIT = byteArrayOf(0x1B, 0x40)                       // ESC @  (initialize)
    private val CMD_FEED = byteArrayOf(0x0A, 0x0A, 0x0A)                 // 3x line feed
    private val CMD_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00)           // GS V B 0 (partial cut)
    private val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)              // ESC E 1
    private val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)             // ESC E 0
    private val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)           // ESC a 0
    private val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)         // ESC a 1
    private val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)          // ESC a 2
    // Ganda-tinggi via GS ! (0x1D 0x21) -- BUKAN ESC ! (0x1B 0x21) -- karena ESC !
    // ikut menyetel pilihan font (bit 0) sehingga akan me-reset Font B kembali ke
    // Font A. GS ! hanya mengatur ukuran (perbesaran), tidak menyentuh font.
    private val CMD_DOUBLE_HEIGHT_ON = byteArrayOf(0x1D, 0x21, 0x01)     // GS ! (tinggi x2)
    private val CMD_DOUBLE_HEIGHT_OFF = byteArrayOf(0x1D, 0x21, 0x00)    // GS ! (normal)
    private val CMD_FONT_B = byteArrayOf(0x1B, 0x4D, 0x01)               // ESC M 1 (font kecil, ~42 kolom)
    private val CMD_FONT_A = byteArrayOf(0x1B, 0x4D, 0x00)               // ESC M 0 (font normal, 32 kolom)

    /** Lebar printer dalam dot untuk kertas 58mm @ 203dpi (umum di printer thermal murah). */
    private const val PRINTER_DOTS_WIDTH = 384

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
     * Cetak struk bergaya (logo, teks bold/rata tengah/dobel tinggi, garis pemisah)
     * ke printer thermal tersimpan. Dipakai untuk desain struk yang lebih rapi
     * dibanding printText() polos.
     */
    suspend fun printReceipt(context: Context, elements: List<ReceiptElement>): Result<Unit> = withContext(Dispatchers.IO) {
        val saved = getSavedPrinter(context)
            ?: return@withContext Result.failure(Exception("Belum ada printer tersimpan"))

        val adapter = getAdapter(context)
            ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia di perangkat ini"))

        var socket: BluetoothSocket? = null
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(saved.mac)
            try {
                adapter.cancelDiscovery()
            } catch (e: SecurityException) {
                // abaikan, lanjut mencoba konek
            }

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            val out = socket.outputStream
            out.write(CMD_INIT)
            // Seluruh struk memakai Font B (rapat, ~42 kolom) agar baris panjang muat
            // satu baris. Logo (raster) tidak terpengaruh perintah font ini.
            out.write(CMD_FONT_B)

            for (el in elements) {
                when (el) {
                    is ReceiptElement.Logo -> {
                        out.write(bitmapToEscPosRaster(el.bitmap))
                    }
                    is ReceiptElement.TextLine -> {
                        out.write(
                            when (el.align) {
                                ReceiptAlign.LEFT -> CMD_ALIGN_LEFT
                                ReceiptAlign.CENTER -> CMD_ALIGN_CENTER
                                ReceiptAlign.RIGHT -> CMD_ALIGN_RIGHT
                            }
                        )
                        if (el.bold) out.write(CMD_BOLD_ON)
                        if (el.doubleHeight) out.write(CMD_DOUBLE_HEIGHT_ON)
                        // Bungkus teks yang melebihi lebar kertas pada batas kata agar
                        // tidak terpotong di tengah kata oleh hardware printer
                        // (mis. "Kasir : Admin Demo (Dapoer Lavan" + "a)").
                        for (line in wrapText(el.text, RECEIPT_WIDTH)) {
                            out.write(encode(line))
                            out.write(byteArrayOf(0x0A))
                        }
                        if (el.doubleHeight) out.write(CMD_DOUBLE_HEIGHT_OFF)
                        if (el.bold) out.write(CMD_BOLD_OFF)
                    }
                    ReceiptElement.Divider -> {
                        out.write(CMD_ALIGN_LEFT)
                        out.write(encode("-".repeat(RECEIPT_WIDTH)))
                        out.write(byteArrayOf(0x0A))
                    }
                    ReceiptElement.Spacer -> {
                        out.write(byteArrayOf(0x0A))
                    }
                }
            }

            out.write(CMD_ALIGN_LEFT)
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
     * Konversi bitmap ke perintah raster image ESC/POS (GS v 0), monokrom 1-bit.
     * Bitmap diskalakan agar lebarnya pas dengan [PRINTER_DOTS_WIDTH] (384 dot / 58mm).
     */
    private fun bitmapToEscPosRaster(original: Bitmap): ByteArray {
        val targetWidth = PRINTER_DOTS_WIDTH
        val scale = targetWidth.toFloat() / original.width
        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)

        val bytesPerRow = (targetWidth + 7) / 8
        val imageBytes = ByteArray(bytesPerRow * targetHeight)

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                // Anggap transparan sebagai putih (tidak dicetak); selain itu ambang gray-scale.
                val luminance = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114)
                val isBlack = alpha > 32 && luminance < 128
                if (isBlack) {
                    val byteIndex = y * bytesPerRow + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    imageBytes[byteIndex] = (imageBytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }

        val xL = (bytesPerRow and 0xFF).toByte()
        val xH = ((bytesPerRow shr 8) and 0xFF).toByte()
        val yL = (targetHeight and 0xFF).toByte()
        val yH = ((targetHeight shr 8) and 0xFF).toByte()

        val header = byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH) // GS v 0
        val output = ByteArrayOutputStream()
        output.write(header)
        output.write(imageBytes)
        output.write(byteArrayOf(0x0A))
        return output.toByteArray()
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
     * Pecah teks agar tiap baris tidak melebihi [width] karakter, memutus pada
     * batas kata (spasi) bila memungkinkan. Baris yang sudah dipisah manual
     * dengan '\n' (mis. dari format kiri-kanan) tetap dihormati. Kata tunggal
     * yang lebih panjang dari [width] dipotong keras agar tetap muat.
     */
    private fun wrapText(text: String, width: Int): List<String> {
        val result = mutableListOf<String>()
        for (segment in text.split("\n")) {
            if (segment.length <= width) {
                result.add(segment)
                continue
            }
            val line = StringBuilder()
            for (word in segment.split(" ")) {
                if (word.length > width) {
                    // Kata tunggal terlalu panjang: keluarkan baris berjalan lalu potong keras.
                    if (line.isNotEmpty()) {
                        result.add(line.toString())
                        line.setLength(0)
                    }
                    var rest = word
                    while (rest.length > width) {
                        result.add(rest.substring(0, width))
                        rest = rest.substring(width)
                    }
                    if (rest.isNotEmpty()) line.append(rest)
                    continue
                }
                val needed = if (line.isEmpty()) word.length else line.length + 1 + word.length
                if (needed > width) {
                    result.add(line.toString())
                    line.setLength(0)
                    line.append(word)
                } else {
                    if (line.isNotEmpty()) line.append(' ')
                    line.append(word)
                }
            }
            if (line.isNotEmpty()) result.add(line.toString())
        }
        return result
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

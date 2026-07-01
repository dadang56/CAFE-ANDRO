package com.lavana.dapoer.data

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Helper untuk membuat dan membagikan file PDF sederhana berbasis teks.
 */
object PdfHelper {

    // Ukuran A4 dalam point (72 dpi)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val TITLE_SIZE = 16f
    private const val BODY_SIZE = 11f
    private const val LINE_HEIGHT = 16f

    /**
     * Buat PDF teks (A4) dengan judul tebal lalu setiap baris dari [lines].
     * Otomatis membuat halaman baru bila konten melebihi tinggi halaman.
     * Disimpan ke cacheDir. Mengembalikan File, atau null bila gagal.
     */
    fun generateTextPdf(context: Context, title: String, lines: List<String>): File? {
        val document = PdfDocument()
        // Lacak halaman yang masih terbuka; PdfDocument.close() melempar IllegalStateException
        // bila ada halaman yang belum di-finishPage(). Tanpa ini, error render apa pun akan
        // menggagalkan blok finally dan men-crash pemanggil alih-alih mengembalikan null.
        var openPage: PdfDocument.Page? = null
        try {
            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textSize = TITLE_SIZE
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                typeface = Typeface.MONOSPACE
                textSize = BODY_SIZE
                isAntiAlias = true
            }

            var pageNumber = 1
            var currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            openPage = currentPage
            var canvas = currentPage.canvas
            var y = MARGIN + TITLE_SIZE

            // Gambar judul (hanya di halaman pertama)
            canvas.drawText(title, MARGIN, y, titlePaint)
            y += LINE_HEIGHT * 2

            for (line in lines) {
                if (y > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(currentPage)
                    openPage = null
                    pageNumber++
                    currentPage = document.startPage(
                        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    )
                    openPage = currentPage
                    canvas = currentPage.canvas
                    y = MARGIN + BODY_SIZE
                }
                canvas.drawText(line, MARGIN, y, bodyPaint)
                y += LINE_HEIGHT
            }

            document.finishPage(currentPage)
            openPage = null

            val fileName = sanitizeFileName(title) + ".pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                openPage?.let { document.finishPage(it) }
            } catch (_: Exception) {}
            try {
                document.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Bagikan/buka file PDF lewat FileProvider + chooser Intent.
     */
    fun sharePdf(context: Context, file: File) {
        try {
            val authority = context.packageName + ".fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Bagikan PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.trim().replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (cleaned.isBlank()) "dokumen" else cleaned
    }
}

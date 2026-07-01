package com.lavana.dapoer.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Palet "Teal Fresh" — diselaraskan dengan desain referensi.
// CATATAN: nama token lama DIPERTAHANKAN agar tidak perlu refactor
// 200+ pemakaian di seluruh kode; hanya NILAINYA yang direvaluasi.
// "OrangeJco" sekarang = Teal (bukan oranye/sage). Untuk tombol CTA
// gunakan token baru OrangeAccent.
// ============================================================

// --- Brand utama (header, ikon, harga, elemen terpilih) ---
val OrangeJco = Color(0xFF13807A)       // Teal Primary
val TealPrimary = OrangeJco
val ForestGreen = Color(0xFF0B524E)     // Teal Dark (akhir gradient, judul gelap)
val TealDark = ForestGreen

// --- Aksen oranye: KHUSUS tombol CTA utama & highlight ---
val OrangeAccent = Color(0xFFF4A82C)    // Login, Pesan, Tambah, +, Favorit aktif
val OnAccentDark = Color(0xFF3A2A06)    // teks gelap di atas oranye
val Mustard = OrangeAccent              // kompatibilitas nama lama

// --- Latar & permukaan ---
val LightGrayJco = Color(0xFFEAF4EE)    // Mint background (latar layar)
val MintBg = LightGrayJco
val LightOrangeJco = Color(0xFFD8EAE3)  // Soft teal tint (indikator/bg lembut)
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// --- Teks ---
val DarkCharcoal = Color(0xFF1B3A33)    // Teal-tinted dark untuk keterbacaan

// --- Aksen lain ---
val RedPromo = Color(0xFFC84B4B)        // diskon / promo

// --- Kompatibilitas nama lama ---
val SageGreen = OrangeJco
val WarmCream = LightGrayJco
val Terracotta = Color(0xFF0E6B66)      // teal medium (alternatif aksen)

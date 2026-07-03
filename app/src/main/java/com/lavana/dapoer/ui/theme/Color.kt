package com.lavana.dapoer.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Palet "Lemon Teal" — kode warna yang diberikan user (2026-07-02):
// #0E6973, #118C8C, #BAD9CE, #F2BB16, #BF820F.
// CATATAN: nama token lama DIPERTAHANKAN agar tidak perlu refactor
// 200+ pemakaian di seluruh kode; hanya NILAINYA yang direvaluasi.
// ============================================================

// --- Brand utama (header, ikon, harga, elemen terpilih) ---
val OrangeJco = Color(0xFF118C8C)       // Teal Primary (#118C8C)
val TealPrimary = OrangeJco
val ForestGreen = Color(0xFF0E6973)     // Teal Dark (#0E6973, akhir gradient, judul gelap)
val TealDark = ForestGreen

// --- Aksen kuning: KHUSUS tombol CTA utama & highlight ---
val OrangeAccent = Color(0xFFF2BB16)    // Login, Pesan, Tambah, +, Favorit aktif (#F2BB16)
val OnAccentDark = Color(0xFF3A2A06)    // teks gelap di atas kuning (kontras, bukan dari palet)
val Mustard = Color(0xFFBF820F)         // aksen amber sekunder (#BF820F)

// --- Latar & permukaan ---
val LightGrayJco = Color(0xFFBAD9CE)    // Mint background (latar layar, #BAD9CE)
val MintBg = LightGrayJco
val LightOrangeJco = Color(0xFFCFE8E8)  // Soft teal tint (indikator/bg lembut, turunan #118C8C)
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

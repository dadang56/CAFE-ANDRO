package com.lavana.dapoer.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json

object SupabaseClient {
    const val SUPABASE_URL = "https://mtjyggxyjojcvcjxiblo.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_nmPTNpDZPNHnuVaGUcBmKQ_3KgP_twU"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        // coerceInputValues: kolom DB yang NULLABLE tanpa default (mis. description,
        // image_url pada menu_items) tersimpan NULL bila field itu tidak dikirim saat
        // insert (encodeDefaults=false membuang field yang nilainya sama dengan default
        // Kotlin, mis. ""). Tanpa flag ini, decode baris ber-NULL ke properti non-null
        // ("String") melempar JsonDecodingException dan MENGGAGALKAN SELURUH daftar
        // (bukan cuma 1 baris) -- inilah sebab "tambah menu tidak muncul, tanpa
        // notifikasi": exception itu tertangkap diam-diam di initializeMenuIfEmpty()
        // dan cuma di-printStackTrace, mengembalikan list kosong.
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
        install(Auth)      // Autentikasi User (Login/Register)
        install(Postgrest) // CRUD Query Database (Insert, Select, Update, Delete)
        install(Realtime)  // Sinkronisasi Pesanan Real-time
        install(Storage)   // Unggah Bukti Bayar dan Foto Menu
    }

    val auth get() = client.auth
    val db get() = client.postgrest
    val realtime get() = client.realtime
    val storage get() = client.storage

    /** Tanggal lokal perangkat untuk timestamp UTC yang dikirim Supabase. */
    fun localOrderDate(createdAt: String?): String? {
        if (createdAt.isNullOrBlank()) return null
        return try {
            // Postgres/Supabase returns timestamptz as an ISO timestamp. Parse its
            // UTC date portion and then format it in the device's local timezone.
            val utcTimestamp = createdAt.take(19)
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val parsed = parser.parse(utcTimestamp) ?: return null
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(parsed)
        } catch (_: Exception) {
            null
        }
    }

    /** UTC timestamp for midnight at the start of the device's local day (e.g. WIB). */
    fun localDayStartUtc(): String {
        val localDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        val localMidnight = localDateFormat.parse(localDateFormat.format(java.util.Date()))
            ?: java.util.Date()
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(localMidnight)
    }

    fun syncDynamicCategories(items: List<MenuItem>) {
        val unique = items.map { it.category }.filter { it.isNotBlank() }.distinct()
        val all = (unique + CUSTOM_CATEGORIES).distinct()
        DYNAMIC_CATEGORIES.clear()
        DYNAMIC_CATEGORIES.addAll(all)
    }

    suspend fun initializeMenuIfEmpty(): List<MenuItem> {
        return try {
            try {
                val bannerList = db["banners"].select().decodeList<BannerItem>()
                val customCats = bannerList
                    .filter { it.title?.startsWith("category|") == true }
                    .map { it.title!!.removePrefix("category|") }
                CUSTOM_CATEGORIES.clear()
                CUSTOM_CATEGORIES.addAll(customCats)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            val fetched = db["menu_items"].select().decodeList<MenuItem>()
            syncDynamicCategories(fetched)
            // Pustaka varian ikut dimuat di sini agar setiap layar yang menampilkan
            // menu (pelanggan, POS kasir, admin) langsung tahu produk mana bervarian.
            // Kegagalannya ditangani di dalam refresh() dan tidak menggagalkan daftar menu.
            VariantLibrary.refresh()
            fetched
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Nomor order yang RESET setiap hari (mulai dari 1 lagi tiap hari baru).
     *
     * Cara kerja: ambil nomor order TERBESAR di antara pesanan HARI INI, lalu +1.
     * Batas "hari ini" sengaja disamakan dengan dashboard admin -- tanggal lokal
     * perangkat (WIB) dibandingkan bagian tanggal `created_at` -- agar konsisten
     * dengan hitungan "Pesanan hari ini". Filter `gte` di server hanya untuk
     * memperkecil data yang diambil; kecocokan pasti ditentukan filter tanggal
     * di sisi Kotlin.
     *
     * Mengembalikan null bila query gagal, sehingga pemanggil membiarkan kolom
     * `order_number` kosong dan DB (SERIAL) yang menetapkan nomor global -- lebih
     * aman daripada menggagalkan pesanan.
     *
     * Catatan: penomoran dihitung di sisi aplikasi (bukan atomik di DB), jadi dua
     * pesanan yang dibuat pada detik yang sama BISA mendapat nomor sama. Untuk
     * satu kasir hal ini praktis tak terjadi; `id` (UUID) tetap unik.
     */
    suspend fun nextDailyOrderNumber(): Int? {
        return try {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            // Supabase/PostgREST limits an unordered select to its first 1,000 rows.
            // With a busy table those can be old orders, silently resetting the
            // daily counter and hiding today's orders after the client-side filter.
            val todays = db["orders"].select {
                filter { gte("created_at", localDayStartUtc()) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<Order>()
                .filter { localOrderDate(it.createdAt) == todayStr }
            (todays.mapNotNull { it.orderNumber }.maxOrNull() ?: 0) + 1
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    var mockUserId: String? = null
    var mockUserEmail: String? = null

    fun setMockUser(id: String?, email: String?) {
        mockUserId = id
        mockUserEmail = email
    }

    val currentUserId: String?
        get() = try {
            auth.currentUserOrNull()?.id ?: mockUserId
        } catch (e: Exception) {
            mockUserId
        }

    val currentUserEmail: String?
        get() = try {
            auth.currentUserOrNull()?.email ?: mockUserEmail
        } catch (e: Exception) {
            mockUserEmail
        }
}

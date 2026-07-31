package com.lavana.dapoer.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cache pustaka varian: isi tabel variant_groups + menu_item_variants.
 *
 * Dipegang sebagai satu objek global (senada dengan [CartManager] dan
 * DYNAMIC_CATEGORIES) supaya sisi pelanggan, POS kasir, dan admin membaca
 * sumber yang sama tanpa harus meneruskan map varian lewat banyak composable.
 *
 * Ukuran datanya kecil (puluhan baris), jadi cukup dimuat sekali per sesi lalu
 * di-refresh setelah admin mengubah varian.
 */
object VariantLibrary {

    /** Seluruh varian di pustaka, termasuk yang non-aktif (admin perlu melihatnya). */
    val groups = mutableStateListOf<VariantGroupRow>()

    /** menuItemId -> himpunan groupId yang diterapkan ke produk itu. */
    private val assignments = mutableStateMapOf<String, Set<String>>()

    /** Sudah pernah selesai memuat (berhasil maupun gagal). */
    var loaded by mutableStateOf(false)
        private set

    /**
     * false bila tabel pustaka varian belum ada di database (migrasi belum
     * dijalankan). Dipakai UI admin untuk memberi tahu langkah yang kurang,
     * bukan menampilkan daftar kosong yang membingungkan.
     */
    var tablesReady by mutableStateOf(true)
        private set

    /** Pesan error terakhir saat refresh, untuk ditampilkan ke admin. */
    var lastError by mutableStateOf<String?>(null)
        private set

    /** Muat ulang pustaka + penerapannya dari Supabase. */
    suspend fun refresh() {
        try {
            val rows = SupabaseClient.db["variant_groups"].select().decodeList<VariantGroupRow>()
            val links = SupabaseClient.db["menu_item_variants"].select().decodeList<MenuItemVariantRow>()

            groups.clear()
            groups.addAll(rows.sortedBy { it.name.lowercase() })

            assignments.clear()
            links.groupBy { it.menuItemId }.forEach { (menuId, list) ->
                assignments[menuId] = list.map { it.groupId }.toSet()
            }

            tablesReady = true
            lastError = null
        } catch (e: Exception) {
            // Tabel belum dibuat: PostgREST menjawab 404 dengan pesan yang menyebut
            // nama relasinya. Bedakan dari error jaringan biasa agar pesan ke admin tepat.
            val msg = e.message ?: ""
            val missingTable = msg.contains("variant_groups", ignoreCase = true) ||
                msg.contains("menu_item_variants", ignoreCase = true) ||
                msg.contains("does not exist", ignoreCase = true) ||
                msg.contains("PGRST205", ignoreCase = true)
            tablesReady = !missingTable
            lastError = msg.ifBlank { "Gagal memuat pustaka varian" }
            groups.clear()
            assignments.clear()
        } finally {
            loaded = true
        }
    }

    /** groupId yang diterapkan ke satu produk (termasuk yang non-aktif). */
    fun groupIdsFor(menuItemId: String): Set<String> = assignments[menuItemId] ?: emptySet()

    /** Produk yang memakai satu varian. */
    fun productIdsFor(groupId: String): Set<String> =
        assignments.filterValues { it.contains(groupId) }.keys.toSet()

    /** Jumlah produk yang memakai varian ini -- untuk label "Diterapkan ke N produk". */
    fun productCount(groupId: String): Int = productIdsFor(groupId).size

    /**
     * Varian yang harus ditawarkan ke pembeli untuk [menuItem].
     *
     * Hanya varian AKTIF dan yang punya minimal satu pilihan. Bila produk tidak
     * punya penerapan dari pustaka, jatuh kembali ke kolom per-produk
     * (menu_items.variants) supaya varian yang pernah diisi manual tetap jalan.
     */
    fun groupsFor(menuItem: MenuItem): List<VariantGroup> {
        val ids = groupIdsFor(menuItem.id)
        if (ids.isEmpty()) return menuItem.getVariantGroups()
        return groups
            .filter { it.isActive && it.id != null && ids.contains(it.id) }
            .map { it.toVariantGroup() }
            .filter { it.options.isNotEmpty() }
    }

    /** true bila produk ini punya varian untuk dipilih pembeli. */
    fun hasVariants(menuItem: MenuItem): Boolean = groupsFor(menuItem).isNotEmpty()
}

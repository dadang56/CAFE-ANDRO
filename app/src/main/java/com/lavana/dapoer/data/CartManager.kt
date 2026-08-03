package com.lavana.dapoer.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lavana.dapoer.ui.screens.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

// Varian yang dipilih pembeli untuk satu baris keranjang, mis. group="Level Pedas", option="Pedas".
data class SelectedVariant(
    val group: String,
    val option: String,
    val price: Double = 0.0
)

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int,
    val selectedVariants: List<SelectedVariant> = emptyList(),
    // Identitas unik satu baris keranjang. Dibutuhkan karena produk yang sama dengan
    // pilihan varian berbeda menjadi baris terpisah, jadi tidak bisa lagi dikenali dari menuItem.id saja.
    val lineId: String = UUID.randomUUID().toString()
)

// Harga satuan baris = harga dasar + total tambahan harga varian yang dipilih.
fun CartItem.unitPrice(): Double = menuItem.price + selectedVariants.sumOf { it.price }
fun CartItem.lineTotal(): Double = unitPrice() * quantity
// Label ringkas untuk tampilan, mis. "Pedas, Bawang Goreng".
fun CartItem.variantLabel(): String = selectedVariants.joinToString(", ") { it.option }
// Catatan lengkap untuk struk/dapur, mis. "Level Pedas: Pedas, Topping: Bawang Goreng".
// Nama grup TIDAK disertakan (mis. bukan "Level Pedas: Pedas") -- struk & slip
// dapur cukup menyebut pilihannya saja, nama grup umumnya sudah jelas dari nama
// menu itu sendiri dan hanya membuat catatan lebih panjang tanpa informasi baru.
fun CartItem.variantNotes(): String = selectedVariants.joinToString(", ") { it.option }

object CartManager {
    val items = mutableStateListOf<CartItem>()
    var selectedOrderType by mutableStateOf<String?>(null)
    var currentStaff by mutableStateOf<StaffAccount?>(null)
    val simulatedDriverLocations = mutableStateMapOf<String, LatLng>()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeSimulations = mutableSetOf<String>()

    fun startDriverSimulation(orderId: String, destCoordinates: String?) {
        if (activeSimulations.contains(orderId)) return
        activeSimulations.add(orderId)
        
        scope.launch {
            try {
                val destCoord = destCoordinates?.split(",")
                val destLat = destCoord?.getOrNull(0)?.toDoubleOrNull() ?: -6.2410
                val destLon = destCoord?.getOrNull(1)?.toDoubleOrNull() ?: 106.8350
                
                val startLat = -6.2297
                val startLon = 106.8296
                
                for (step in 0..10) {
                    if (!activeSimulations.contains(orderId)) break
                    val fraction = step / 10.0
                    val currentLat = startLat + (destLat - startLat) * fraction
                    val currentLon = startLon + (destLon - startLon) * fraction
                    val currentLoc = LatLng(currentLat, currentLon)

                    // Selalu perbarui peta lokasi di memori (jalur instan satu perangkat),
                    // baik untuk order simulasi maupun order DB nyata.
                    simulatedDriverLocations[orderId] = currentLoc

                    // Untuk order nyata (non sim_), tetap tulis lokasi ke notes DB
                    // agar tracking lintas perangkat tetap berjalan.
                    if (!orderId.startsWith("sim_")) {
                        try {
                            val freshOrder = SupabaseClient.db["orders"].select {
                                filter { eq("id", orderId) }
                            }.decodeSingle<Order>()
                            val updatedNotes = appendDriverLocation(freshOrder.notes, currentLoc)

                            SupabaseClient.db["orders"].update({
                                set("notes", updatedNotes)
                            }) {
                                filter { eq("id", orderId) }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    delay(3000)
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                activeSimulations.remove(orderId)
            }
        }
    }
    
    fun stopDriverSimulation(orderId: String) {
        // Hentikan simulasi tetapi JANGAN hapus lokasi terakhir dari peta,
        // agar pin kurir tetap berhenti di titik tujuan (destination).
        activeSimulations.remove(orderId)
    }

    // Kunci unik kombinasi varian, agar pilihan yang sama (urutan bebas) digabung ke satu baris.
    private fun variantKey(vs: List<SelectedVariant>): String =
        vs.map { "${it.group}=${it.option}" }.sorted().joinToString("|")

    // Tambah produk TANPA varian (dipakai tombol "Beli" cepat & stepper produk polos).
    // Hanya menyentuh baris tanpa-varian, tidak mengganggu baris yang punya varian.
    fun addItem(menuItem: MenuItem) {
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id && it.selectedVariants.isEmpty() }
        if (index != -1) {
            val current = items[index]
            items[index] = current.copy(quantity = current.quantity + 1)
        } else {
            items.add(CartItem(menuItem, 1))
        }
    }

    // Tambah produk DENGAN pilihan varian tertentu. Kombinasi varian yang sama digabung
    // (quantity bertambah); kombinasi berbeda menjadi baris keranjang baru.
    fun addConfiguredItem(menuItem: MenuItem, quantity: Int, selectedVariants: List<SelectedVariant>) {
        if (quantity <= 0) return
        val key = variantKey(selectedVariants)
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id && variantKey(it.selectedVariants) == key }
        if (index != -1) {
            val current = items[index]
            items[index] = current.copy(quantity = current.quantity + quantity)
        } else {
            items.add(CartItem(menuItem, quantity, selectedVariants))
        }
    }

    fun removeItem(menuItem: MenuItem) {
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id && it.selectedVariants.isEmpty() }
        if (index != -1) {
            val current = items[index]
            if (current.quantity > 1) {
                items[index] = current.copy(quantity = current.quantity - 1)
            } else {
                items.removeAt(index)
            }
        }
    }

    // Operasi berbasis baris (lineId) untuk keranjang di Checkout — tidak ambigu walau ada
    // beberapa baris dari produk yang sama dengan varian berbeda.
    fun incrementLine(lineId: String) {
        val index = items.indexOfFirst { it.lineId == lineId }
        if (index != -1) {
            val current = items[index]
            items[index] = current.copy(quantity = current.quantity + 1)
        }
    }

    fun decrementLine(lineId: String) {
        val index = items.indexOfFirst { it.lineId == lineId }
        if (index != -1) {
            val current = items[index]
            if (current.quantity > 1) {
                items[index] = current.copy(quantity = current.quantity - 1)
            } else {
                items.removeAt(index)
            }
        }
    }

    fun deleteLine(lineId: String) {
        val index = items.indexOfFirst { it.lineId == lineId }
        if (index != -1) {
            items.removeAt(index)
        }
    }

    // Total kuantitas produk ini di keranjang, dijumlah lintas semua baris variannya
    // (dipakai untuk badge jumlah pada kartu produk).
    fun getItemQuantity(menuItem: MenuItem): Int {
        return items.filter { it.menuItem.id == menuItem.id }.sumOf { it.quantity }
    }

    fun getItemsCount(): Int {
        return items.sumOf { it.quantity }
    }

    fun getTotalPrice(): Double {
        return items.sumOf { (it.menuItem.price + it.selectedVariants.sumOf { v -> v.price }) * it.quantity }
    }

    fun deleteItem(menuItem: MenuItem) {
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id && it.selectedVariants.isEmpty() }
        if (index != -1) {
            items.removeAt(index)
        }
    }

    fun clearCart() {
        items.clear()
    }
}

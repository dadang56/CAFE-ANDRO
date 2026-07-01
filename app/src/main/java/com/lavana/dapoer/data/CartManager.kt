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

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int
)

object CartManager {
    val items = mutableStateListOf<CartItem>()
    var selectedOrderType by mutableStateOf<String?>("Delivery")
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

    fun addItem(menuItem: MenuItem) {
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id }
        if (index != -1) {
            val current = items[index]
            items[index] = current.copy(quantity = current.quantity + 1)
        } else {
            items.add(CartItem(menuItem, 1))
        }
    }

    fun removeItem(menuItem: MenuItem) {
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id }
        if (index != -1) {
            val current = items[index]
            if (current.quantity > 1) {
                items[index] = current.copy(quantity = current.quantity - 1)
            } else {
                items.removeAt(index)
            }
        }
    }

    fun getItemQuantity(menuItem: MenuItem): Int {
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id }
        return if (index != -1) items[index].quantity else 0
    }

    fun getItemsCount(): Int {
        return items.sumOf { it.quantity }
    }

    fun getTotalPrice(): Double {
        return items.sumOf { it.menuItem.price * it.quantity }
    }

    fun deleteItem(menuItem: MenuItem) {
        val index = items.indexOfFirst { it.menuItem.id == menuItem.id }
        if (index != -1) {
            items.removeAt(index)
        }
    }

    fun clearCart() {
        items.clear()
    }
}

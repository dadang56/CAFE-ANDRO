package com.lavana.dapoer.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.lavana.dapoer.ui.screens.LatLng
import androidx.compose.runtime.mutableStateListOf

@Serializable
data class MenuItem(
    val id: String,
    val name: String,
    val description: String = "",
    val price: Double,
    val category: String,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("is_available") val isAvailable: Boolean = true
)

@Serializable
data class Promo(
    val id: String? = null,
    val code: String,
    @SerialName("discount_percent") val discountPercent: Double,
    val description: String = "",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class CustomerProfile(
    val id: String,
    val email: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BannerItem(
    val id: String? = null,
    @SerialName("image_url") val imageUrl: String,
    val title: String? = ""
)


@Serializable
data class StaffAccount(
    val id: String? = null,
    val username: String,
    val password: String,
    val role: String, // 'Admin', 'Kasir', 'Driver'
    val name: String,
    @SerialName("contact_number") val contactNumber: String? = null
)

@Serializable
data class Order(
    val id: String? = null,
    @SerialName("order_number") val orderNumber: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("order_type") val orderType: String,
    val status: String = "Pending",
    @SerialName("table_number") val tableNumber: String? = null,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("delivery_distance_km") val deliveryDistanceKm: Double? = null,
    @SerialName("delivery_fee") val deliveryFee: Double = 0.0,
    val subtotal: Double,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    val total: Double,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("payment_status") val paymentStatus: String = "Belum Bayar",
    @SerialName("payment_receipt_url") val paymentReceiptUrl: String? = null,
    val notes: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    val coordinates: String? = null, // "lat,lon"
    @SerialName("driver_id") val driverId: String? = null,
    @SerialName("cashier_username") val cashierUsername: String? = null,
    @SerialName("delivery_proof_url") val deliveryProofUrl: String? = null,
    @SerialName("delivery_distance_meters") val deliveryDistanceMeters: Double? = null,
    @SerialName("delivery_within_tolerance") val deliveryWithinTolerance: Boolean? = null
)

@Serializable
data class ChatMessage(
    val id: String? = null,
    @SerialName("order_id") val orderId: String,
    @SerialName("sender_role") val senderRole: String, // "Admin" atau "Customer"
    @SerialName("sender_name") val senderName: String? = null,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class OrderItem(
    val id: String? = null,
    @SerialName("order_id") val orderId: String,
    @SerialName("menu_item_id") val menuItemId: String,
    val quantity: Int,
    @SerialName("price_at_order") val priceAtOrder: Double,
    val notes: String? = null
)

@Serializable
data class Expense(
    val id: String = "",
    val description: String,
    val amount: Double,
    @SerialName("created_at") val createdAt: String
)

fun parseDriverLocation(notes: String?): LatLng? {
    if (notes == null) return null
    val regex = "\\[DriverLoc:(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)\\]".toRegex()
    val match = regex.find(notes)
    if (match != null) {
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return LatLng(lat, lon)
    }
    return null
}

fun cleanNotesFromLocation(notes: String?): String? {
    if (notes == null) return null
    val clean = notes.replace("\\[DriverLoc:(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)\\]".toRegex(), "").trim()
    return if (clean.isEmpty()) null else clean
}

fun appendDriverLocation(notes: String?, location: LatLng): String {
    val clean = cleanNotesFromLocation(notes) ?: ""
    val tag = "[DriverLoc:${location.latitude},${location.longitude}]"
    return if (clean.isEmpty()) tag else "$clean $tag"
}

// Jarak antara dua titik koordinat dalam METER (formula Haversine).
fun distanceInMeters(a: LatLng, b: LatLng): Double {
    val earthRadiusM = 6371000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
    return earthRadiusM * c
}

const val DELIVERY_DISTANCE_TOLERANCE_METERS = 100.0

fun getFallbackMenu(): List<MenuItem> {
    return listOf(
        MenuItem("jco-1", "Alcapone Donut", "Donat lembut dengan topping cokelat putih Belgia dan irisan almond panggang renyah.", 12000.0, "Donuts", "https://images.unsplash.com/photo-1551024601-bec78aea704b?w=500"),
        MenuItem("jco-2", "Oreology Donut", "Donat Dapoer Lavana dengan taburan biskuit Oreo hancur dan krim manis lembut.", 12000.0, "Donuts", "https://images.unsplash.com/photo-1612240498936-65f5101365d2?w=500"),
        MenuItem("jco-3", "Tiramisu Donut", "Donat isi krim tiramisu lezat dilapisi cokelat tiramisu gurih.", 12000.0, "Donuts", "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?w=500"),
        MenuItem("jco-4", "Iced Avocado Frappe", "Espresso premium dicampur alpukat segar kental dengan es batu melimpah.", 38000.0, "Coffee", "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=500"),
        MenuItem("jco-5", "Jcoccino Hot", "Cappuccino klasik ala Dapoer Lavana dengan espresso arabika pilihan.", 32000.0, "Coffee", "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500"),
        MenuItem("jco-6", "Iced Lychee Tea", "Teh manis segar rasa leci dengan buah leci utuh.", 28000.0, "Non-Coffee", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500"),
        MenuItem("jco-7", "D.Club Cheezy Rich", "Sandwich donat gurih dengan isi keju cheddar melimpah dan saus mayo.", 20000.0, "J-Club", "https://images.unsplash.com/photo-1509722747041-616f39b57569?w=500")
    )
}

val DYNAMIC_CATEGORIES = mutableStateListOf("Donuts", "J-Club", "Coffee", "Non-Coffee", "Combos")
val CUSTOM_CATEGORIES = mutableStateListOf<String>()
val MENU_CATEGORIES: List<String> get() = DYNAMIC_CATEGORIES.toList()

fun MenuItem.getOriginalPrice(): Double? {
    val regex = "\\[OriginalPrice:(\\d+(?:\\.\\d+)?)\\]".toRegex()
    val match = regex.find(description)
    return match?.groupValues?.get(1)?.toDoubleOrNull()
}

fun MenuItem.getCleanDescription(): String {
    return description.replace("\\[OriginalPrice:(-?\\d+(?:\\.\\d+)?)\\]".toRegex(), "").trim()
}

fun createDescriptionWithOriginalPrice(cleanDesc: String, originalPrice: Double?): String {
    if (originalPrice == null || originalPrice <= 0.0) return cleanDesc
    val tag = "[OriginalPrice:$originalPrice]"
    return if (cleanDesc.isEmpty()) tag else "$cleanDesc $tag"
}

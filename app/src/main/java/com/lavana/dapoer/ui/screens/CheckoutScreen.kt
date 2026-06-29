package com.lavana.dapoer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lavana.dapoer.ui.components.GoogleMapView
import com.lavana.dapoer.ui.theme.*
import com.lavana.dapoer.data.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.coroutines.launch
import kotlin.math.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast

@Serializable
data class SettingsRow(
    val key: String,
    val value: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    orderType: String,
    onNavigateBack: () -> Unit,
    onOrderPlaced: (String) -> Unit
) {
    if (CartManager.items.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Checkout Pesanan", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = DarkCharcoal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = LightGrayJco
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Keranjang Belanja Anda Kosong",
                        fontWeight = FontWeight.Bold,
                        color = DarkCharcoal
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
                    ) {
                        Text("Kembali ke Menu", color = Color.White)
                    }
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val sharedPref = remember(context) { context.getSharedPreferences("lavana_prefs", android.content.Context.MODE_PRIVATE) }
    val defaultAddress = remember(sharedPref) { sharedPref.getString("default_address", "Jl. Melati No. 12, Kebayoran Baru, Jakarta Selatan") ?: "Jl. Melati No. 12, Kebayoran Baru, Jakarta Selatan" }
    var addressInput by remember { mutableStateOf(defaultAddress) }
    val defaultCoordsString = remember(sharedPref) { sharedPref.getString("default_coordinates", "-6.2410,106.8350") ?: "-6.2410,106.8350" }
    val parsedCoords = remember(defaultCoordsString) {
        val parts = defaultCoordsString.split(",")
        if (parts.size == 2) {
            LatLng(parts[0].toDoubleOrNull() ?: -6.2410, parts[1].toDoubleOrNull() ?: 106.8350)
        } else {
            LatLng(-6.2410, 106.8350)
        }
    }
    
    // Titik Lokasi Kafe Dapoer Lavana (Default: Jakarta Selatan)
    var cafeLocation by remember { mutableStateOf(LatLng(-6.2297, 106.8296)) }
    
    // Lokasi Default Pengiriman (dari SharedPreferences)
    var userLocation by remember { mutableStateOf(parsedCoords) }
    
    // Perhitungan Jarak Real-time menggunakan Rumus Haversine (Sebagai representasi Map API)
    val distanceKm = remember(userLocation, cafeLocation) {
        calculateDistance(cafeLocation.latitude, cafeLocation.longitude, userLocation.latitude, userLocation.longitude)
    }
    
    val coroutineScope = rememberCoroutineScope()
    var isPlacingOrder by remember { mutableStateOf(false) }
    var errorPlaceOrder by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchingAddress by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<com.lavana.dapoer.data.SearchResult>>(emptyList()) }
    
    var qrisUrl by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("BCA") }
    var bankNo by remember { mutableStateOf("1234567890") }
    var bankOwner by remember { mutableStateOf("Dapoer Lavana") }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            kotlinx.coroutines.delay(500)
            isSearchingAddress = true
            searchSuggestions = com.lavana.dapoer.data.GeocodingHelper.searchAddress(searchQuery)
            isSearchingAddress = false
        } else {
            searchSuggestions = emptyList()
        }
    }
    
    // Launcher untuk perizinan lokasi Android native
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            getUserLocation(context) { loc ->
                userLocation = loc
                coroutineScope.launch {
                    val street = com.lavana.dapoer.data.GeocodingHelper.reverseGeocode(loc.latitude, loc.longitude)
                    if (street.isNotBlank()) {
                        addressInput = street
                    }
                }
            }
        }
    }

    // Parameter Tarif Dinamis dari Supabase
    var baseDistance by remember { mutableStateOf(2.0) }
    var baseFee by remember { mutableStateOf(5000.0) }
    var perKmFee by remember { mutableStateOf(2500.0) }

    LaunchedEffect(Unit) {
        if (orderType == "Delivery") {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (hasFineLocation || hasCoarseLocation) {
                getUserLocation(context) { loc ->
                    userLocation = loc
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        try {
            val settings = SupabaseClient.db["settings"].select().decodeList<SettingsRow>()
            var lat = cafeLocation.latitude
            var lng = cafeLocation.longitude
            settings.forEach { row ->
                when (row.key) {
                    "delivery_base_distance_km" -> baseDistance = row.value
                    "delivery_base_fee" -> baseFee = row.value
                    "delivery_per_km_fee" -> perKmFee = row.value
                    "cafe_latitude" -> lat = row.value
                    "cafe_longitude" -> lng = row.value
                }
            }
            cafeLocation = LatLng(lat, lng)
        } catch (e: Exception) {
            // Gunakan default fallback
        }
        
        try {
            val bannerList = SupabaseClient.db["banners"].select().decodeList<BannerItem>()
            val qrisBanner = bannerList.firstOrNull { it.title == "payment_qris" }
            if (qrisBanner != null && qrisBanner.imageUrl.isNotBlank()) {
                qrisUrl = qrisBanner.imageUrl
            }
            
            val bankBanner = bannerList.firstOrNull { it.title?.startsWith("payment_bank|") == true }
            if (bankBanner != null && bankBanner.title != null) {
                val parts = bankBanner.title.split("|")
                if (parts.size >= 4) {
                    bankName = parts[1]
                    bankNo = parts[2]
                    bankOwner = parts[3]
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val deliveryFee = remember(distanceKm, baseDistance, baseFee, perKmFee) {
        if (distanceKm <= baseDistance) {
            baseFee
        } else {
            baseFee + ((distanceKm - baseDistance) * perKmFee)
        }
    }
    
    val subtotal = CartManager.getTotalPrice()
    val totalPayment = if (orderType == "Delivery") subtotal + deliveryFee else subtotal
    
    var tableNumberInput by remember { mutableStateOf("Meja Nomor 05") }
    var selectedPayment by remember { mutableStateOf("QRIS") }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout Pesanan", fontWeight = FontWeight.Bold, color = DarkCharcoal) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = DarkCharcoal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = LightGrayJco
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            
            // JIKA DELIVERY: Tampilkan Peta Google Maps Interaktif
            if (orderType == "Delivery") {
                Text(
                    text = "Tentukan Lokasi Pengiriman",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkCharcoal,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Search Address Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari alamat...") },
                        label = { Text("Cari Lokasi") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearchingAddress = true
                                coroutineScope.launch {
                                    searchSuggestions = com.lavana.dapoer.data.GeocodingHelper.searchAddress(searchQuery)
                                    isSearchingAddress = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Cari", color = Color.White, fontSize = 12.sp)
                    }
                }

                if (isSearchingAddress) {
                    CircularProgressIndicator(color = OrangeJco, modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
                }

                // Suggestions List
                if (searchSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .heightIn(max = 150.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(searchSuggestions) { result ->
                                Text(
                                    text = result.displayName,
                                    fontSize = 11.sp,
                                    color = DarkCharcoal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            addressInput = result.displayName
                                            userLocation = LatLng(result.latitude, result.longitude)
                                            searchSuggestions = emptyList()
                                            searchQuery = ""
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }

                // Scan GPS Button
                Button(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gunakan Lokasi GPS Saat Ini", color = Color.White, fontSize = 12.sp)
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                ) {
                    GoogleMapView(
                        center = userLocation,
                        markers = listOf(
                            cafeLocation to "Dapoer Lavana",
                            userLocation to "Tujuan Pengiriman Anda"
                        ),
                        onMapClick = { p ->
                            userLocation = p
                            coroutineScope.launch {
                                val street = com.lavana.dapoer.data.GeocodingHelper.reverseGeocode(p.latitude, p.longitude)
                                if (street.isNotBlank()) {
                                    addressInput = street
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Info Jarak & Ongkir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(LightOrangeJco, RoundedCornerShape(8.dp))
                            .border(1.dp, OrangeJco.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Jarak Antar", fontSize = 10.sp, color = DarkCharcoal)
                            Text(String.format("%.1f km", distanceKm), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OrangeJco)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(LightOrangeJco, RoundedCornerShape(8.dp))
                            .border(1.dp, OrangeJco.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Ongkos Kirim", fontSize = 10.sp, color = DarkCharcoal)
                            Text("Rp ${String.format("%,.0f", deliveryFee)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OrangeJco)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (orderType == "Dine In") {
                    Text("Nomor Meja", fontWeight = FontWeight.Bold, color = DarkCharcoal, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = tableNumberInput,
                        onValueChange = { tableNumberInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                } else {
                    Text("Alamat Lengkap", fontWeight = FontWeight.Bold, color = DarkCharcoal, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeJco,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Rincian Pesanan
            Text("Rincian Pesanan", fontWeight = FontWeight.Bold, color = DarkCharcoal, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (CartManager.items.isEmpty()) {
                Text("Keranjang Anda kosong. Menggunakan item demo.", fontSize = 12.sp, color = DarkCharcoal.copy(alpha = 0.5f))
                OrderSummaryItem(name = "Alcapone Donut x2", price = 24000.0)
            } else {
                CartManager.items.forEach { cartItem ->
                    EditableCartItem(
                        cartItem = cartItem,
                        onAdd = { CartManager.addItem(cartItem.menuItem) },
                        onRemove = { CartManager.removeItem(cartItem.menuItem) },
                        onDeleteAll = { CartManager.deleteItem(cartItem.menuItem) }
                    )
                }
            }
            
            if (orderType == "Delivery") {
                OrderSummaryItem(name = "Ongkos Kirim (${String.format("%.1f km", distanceKm)})", price = deliveryFee)
            }
            
            HorizontalDivider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pembayaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkCharcoal)
                Text("Rp ${String.format("%,.0f", totalPayment)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrangeJco)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (errorPlaceOrder != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorPlaceOrder!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
 
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tombol "Pesan Sekarang" (Modern J.CO style)
            Button(
                onClick = {
                    isPlacingOrder = true
                    errorPlaceOrder = null
                    coroutineScope.launch {
                        try {
                            val currentUserId = SupabaseClient.currentUserId
 
                            val newOrder = Order(
                                userId = currentUserId,
                                orderType = orderType,
                                status = "Pending",
                                tableNumber = if (orderType == "Dine In") tableNumberInput else null,
                                deliveryAddress = if (orderType == "Delivery") addressInput else null,
                                deliveryDistanceKm = if (orderType == "Delivery") distanceKm else null,
                                deliveryFee = if (orderType == "Delivery") deliveryFee else 0.0,
                                subtotal = subtotal,
                                total = totalPayment,
                                paymentMethod = "Belum Memilih",
                                paymentStatus = "Belum Bayar",
                                coordinates = if (orderType == "Delivery") "${userLocation.latitude},${userLocation.longitude}" else null,
                                notes = "Dipesan via Android App"
                            )
                            
                            // 1. Kirim order ke Supabase
                            val insertedOrder = SupabaseClient.db["orders"].insert(newOrder) {
                                select()
                            }.decodeSingle<Order>()
                            
                            val orderId = insertedOrder.id!!
                            
                            // 2. Kirim order items ke Supabase jika cart berisi
                            val itemsToInsert = if (CartManager.items.isNotEmpty()) {
                                CartManager.items.map { cartItem ->
                                    OrderItem(
                                        orderId = orderId,
                                        menuItemId = cartItem.menuItem.id,
                                        quantity = cartItem.quantity,
                                        priceAtOrder = cartItem.menuItem.price
                                    )
                                }
                            } else {
                                emptyList()
                            }
                            
                            if (itemsToInsert.isNotEmpty()) {
                                SupabaseClient.db["order_items"].insert(itemsToInsert)
                            }
                            
                            // 3. Bersihkan keranjang
                            CartManager.clearCart()
                            Toast.makeText(context, "Pesanan berhasil dibuat!", Toast.LENGTH_LONG).show()
                            
                            // 4. Pindah ke Halaman Lacak
                            onOrderPlaced(orderId)
                        } catch (e: Exception) {
                            errorPlaceOrder = "Gagal memproses pesanan: ${e.localizedMessage}"
                            Toast.makeText(context, "Gagal memproses pesanan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isPlacingOrder = false
                        }
                    }
                },
                enabled = !isPlacingOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeJco)
            ) {
                Text(
                    text = if (isPlacingOrder) "Memproses Pesanan..." else "Pesan Sekarang",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
 
@Composable
fun OrderSummaryItem(name: String, price: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = DarkCharcoal.copy(alpha = 0.8f), fontSize = 14.sp)
        Text("Rp ${String.format("%,.0f", price)}", color = DarkCharcoal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EditableCartItem(
    cartItem: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cartItem.menuItem.imageUrl != null) {
                AsyncImage(
                    model = cartItem.menuItem.imageUrl,
                    contentDescription = cartItem.menuItem.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.menuItem.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkCharcoal
                )
                Text(
                    text = "Rp ${String.format("%,.0f", cartItem.menuItem.price)}",
                    fontSize = 11.sp,
                    color = OrangeJco,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onDeleteAll() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hapus",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightOrangeJco)
                            .border(1.dp, OrangeJco, RoundedCornerShape(6.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, color = OrangeJco, fontSize = 12.sp)
                        }
                        Text(
                            text = cartItem.quantity.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeJco,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, color = OrangeJco, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Rumus Haversine untuk kalkulasi jarak jalan
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radius bumi dalam KM
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun getUserLocation(context: Context, onLocationReceived: (LatLng) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    try {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            // Coba GPS provider
            var loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc == null) {
                // Coba Network provider
                loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (loc == null) {
                // Coba Passive provider
                loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
            
            if (loc != null) {
                onLocationReceived(LatLng(loc.latitude, loc.longitude))
            }
            
            // Selalu minta update baru satu kali secara dinamis agar data GPS segar & akurat
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationReceived(LatLng(location.latitude, location.longitude))
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, context.mainLooper)
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, context.mainLooper)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

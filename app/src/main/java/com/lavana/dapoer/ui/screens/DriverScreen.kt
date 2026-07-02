package com.lavana.dapoer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lavana.dapoer.data.*
import com.lavana.dapoer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.lavana.dapoer.ui.components.GoogleMapView

@Composable
fun DriverScreen(
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) } // 0: Active, 1: History, 2: Profile
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Tugas Aktif") },
                    label = { Text("Tugas Aktif", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
                    label = { Text("Riwayat", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeJco,
                        selectedTextColor = OrangeJco,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightOrangeJco
                    )
                )
            }
        },
        containerColor = LightGrayJco
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                0 -> TabDriverActive()
                1 -> TabDriverHistory()
                2 -> TabDriverProfile(onLogout = onLogout)
            }
        }
    }
}

// ==========================================================
// TABS 0: ACTIVE DELIVERY TASK WITH REALTIME OSM SIMULATION
// ==========================================================
@Composable
fun TabDriverActive() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeOrder by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSimulating by remember { mutableStateOf(false) }
    var showDeliveryConfirmDialog by remember { mutableStateOf(false) }

    val driverId = remember { SupabaseClient.currentUserId ?: "00000000-0000-0000-0000-0000000000d1" }

    fun loadActiveTask(showLoading: Boolean = true) {
        if (showLoading) {
            isLoading = true
        }
        coroutineScope.launch {
            try {
                // Fetch orders assigned to this driver that are not finished
                val orders = SupabaseClient.db["orders"].select {
                    filter {
                        eq("driver_id", driverId)
                        neq("status", "Selesai")
                    }
                }.decodeList<Order>()
                activeOrder = orders.firstOrNull()
            } catch (e: Exception) {
                // DB Error
            } finally {
                if (showLoading) {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadActiveTask()
    }

    // Polling: muat ulang tugas aktif setiap 3 detik (tanpa spinner) agar
    // penanda lokasi driver di peta ikut bergerak selama pengantaran.
    // Coroutine otomatis dibatalkan saat composable keluar.
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            loadActiveTask(showLoading = false)
        }
    }

    val customerHome = remember(activeOrder?.coordinates) {
        val parts = activeOrder?.coordinates?.split(",")
        val lat = parts?.getOrNull(0)?.toDoubleOrNull() ?: -6.2410
        val lon = parts?.getOrNull(1)?.toDoubleOrNull() ?: 106.8350
        LatLng(lat, lon)
    }

    val driverLocation = remember(activeOrder?.notes, activeOrder?.id, CartManager.simulatedDriverLocations[activeOrder?.id ?: ""]) {
        val loc = parseDriverLocation(activeOrder?.notes)
        if (loc != null) return@remember loc
        val simLoc = CartManager.simulatedDriverLocations[activeOrder?.id ?: ""]
        if (simLoc != null) return@remember simLoc
        LatLng(-6.2297, 106.8296) // Cafe location fallback
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Gradient floating header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tugas Pengantaran Aktif", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Pantau dan selesaikan pengiriman Anda", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
                IconButton(
                    onClick = { loadActiveTask() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrangeJco)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Memuat tugas...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else if (activeOrder == null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(LightOrangeJco),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TaskAlt, contentDescription = "No active task", tint = OrangeJco, modifier = Modifier.size(64.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Kerja bagus! Tidak ada tugas aktif.", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Menunggu Admin menugaskan pesanan baru.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            val order = activeOrder!!

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Status banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (order.status == "Diantar") LightOrangeJco else LightGrayJco
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, OrangeJco.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (order.status == "Diantar") Icons.Default.LocalShipping else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = OrangeJco,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            if (order.status == "Diantar") "Sedang Mengantar" else "Siap Diantar",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreen
                        )
                        Text(
                            if (order.status == "Diantar") "Pelanggan sedang menunggu pesanan." else "Tekan Mulai Kirim untuk memulai pengiriman.",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }
                }
            }

            // Map View
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    GoogleMapView(
                        center = driverLocation,
                        markers = listOf(
                            driverLocation to "Lokasi Saya",
                            customerHome to "Rumah Pelanggan"
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Customer details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LightOrangeJco),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Tujuan Pengiriman", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OrangeJco)
                            Text("No. Order: #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    InfoRowDriver(Icons.Default.Home, "Alamat", order.deliveryAddress ?: "-")
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowDriver(Icons.Default.Payments, "Pembayaran", "${order.paymentMethod} (Rp ${String.format("%,.0f", order.total)})")
                    Spacer(modifier = Modifier.height(8.dp))
                    val customerPhone = order.customerPhone ?: "08123456789"
                    InfoRowDriver(Icons.Default.Phone, "Telepon", customerPhone)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val destination = order.coordinates ?: order.deliveryAddress ?: ""
                                val uri = Uri.parse("google.navigation:q=$destination")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
                                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                                    try { context.startActivity(webIntent) } catch (ex: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                            border = BorderStroke(1.5.dp, ForestGreen),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigasi Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val rawPhone = order.customerPhone ?: ""
                                val cleanPhone = if (rawPhone.startsWith("0")) "62" + rawPhone.substring(1) else rawPhone
                                val uri = Uri.parse("https://wa.me/$cleanPhone")
                                val waIntent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawPhone"))
                                    try { context.startActivity(dialIntent) } catch (ex: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeJco),
                            border = BorderStroke(1.5.dp, OrangeJco),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.4f))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (order.status != "Diantar") {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            SupabaseClient.db["orders"].update({
                                                set("status", "Diantar")
                                            }) { filter { eq("id", order.id ?: "") } }
                                            CartManager.startDriverSimulation(order.id ?: "", order.coordinates)
                                            loadActiveTask()
                                        } catch (e: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = OnAccentDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mulai Kirim", color = OnAccentDark, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { showDeliveryConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Konfirmasi Sampai (Foto)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            }

            if (showDeliveryConfirmDialog) {
                DeliveryConfirmDialog(
                    order = order,
                    onDismiss = { showDeliveryConfirmDialog = false },
                    onConfirmed = {
                        CartManager.stopDriverSimulation(order.id ?: "")
                        isSimulating = false
                        loadActiveTask()
                    }
                )
            }
        }
        }
    }
}

@Composable
private fun InfoRowDriver(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, color = DarkCharcoal, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==========================================================
// TABS 1: DRIVER COMPLETED HISTORIES
// ==========================================================
@Composable
fun TabDriverHistory() {
    val coroutineScope = rememberCoroutineScope()
    var completedOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val driverId = remember { SupabaseClient.currentUserId ?: "00000000-0000-0000-0000-0000000000d1" }

    fun loadHistory() {
        isLoading = true
        coroutineScope.launch {
            try {
                val orders = SupabaseClient.db["orders"].select {
                    filter {
                        eq("driver_id", driverId)
                        eq("status", "Selesai")
                    }
                }.decodeList<Order>().sortedByDescending { it.orderNumber ?: 0 }
                completedOrders = orders
            } catch (e: Exception) {}
            finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHistory()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Gradient floating header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Riwayat Pengantaran", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Daftar pengiriman yang telah selesai", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
                IconButton(
                    onClick = { loadHistory() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrangeJco)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Memuat riwayat...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (completedOrders.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(LightOrangeJco),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(64.dp))
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("Belum ada riwayat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkCharcoal)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Belum ada pengantaran yang diselesaikan.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(completedOrders) { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreen)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(LightOrangeJco)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Selesai", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${order.deliveryAddress}", fontSize = 12.sp, color = DarkCharcoal)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = OrangeJco, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rp ${String.format("%,.0f", order.total)} • ${order.paymentMethod}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

// ==========================================================
// TABS 2: DRIVER PROFILE
// ==========================================================
@Composable
fun TabDriverProfile(onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // Gradient header with avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(OrangeJco, ForestGreen)))
                .padding(top = 32.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeliveryDining, contentDescription = "Driver", tint = OrangeJco, modifier = Modifier.size(60.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Kurir Delivery Dapoer Lavana", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mitra Pengirim / Driver", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Informasi Akun", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkCharcoal)
                    Spacer(modifier = Modifier.height(14.dp))
                    InfoRowDriver(Icons.Default.Email, "Email", "driver@lavana.com")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    InfoRowDriver(Icons.Default.Badge, "Level Akses", "Mitra Pengirim / Driver")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = RedPromo),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Aplikasi", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

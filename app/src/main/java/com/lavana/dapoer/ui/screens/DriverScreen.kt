package com.lavana.dapoer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tugas Pengantaran Aktif", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            IconButton(onClick = { loadActiveTask() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = OrangeJco)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangeJco)
            }
        } else if (activeOrder == null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TaskAlt, contentDescription = "No active task", tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kerja bagus! Tidak ada tugas aktif.", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Menunggu Admin menugaskan pesanan baru.", fontSize = 12.sp, color = Color.LightGray)
                }
            }
        } else {
            val order = activeOrder!!
            
            // Map View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
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

            Spacer(modifier = Modifier.height(14.dp))

            // Customer details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tujuan Pengiriman", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OrangeJco)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No. Order: #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreen)
                    Text("Alamat: ${order.deliveryAddress}", fontSize = 13.sp, color = DarkCharcoal)
                    Text("Pembayaran: ${order.paymentMethod} (Rp ${String.format("%,.0f", order.total)})", fontSize = 12.sp, color = Color.Gray)
                    
                    val customerPhone = order.customerPhone ?: "08123456789"
                    Text("Telepon: $customerPhone", fontSize = 12.sp, color = Color.Gray)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                            border = BorderStroke(1.dp, ForestGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
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
                            border = BorderStroke(1.dp, OrangeJco),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    
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
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeJco),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Mulai Kirim", color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            isSimulating = false
                                            CartManager.stopDriverSimulation(order.id ?: "")
                                            
                                            SupabaseClient.db["orders"].update({
                                                set("status", "Selesai")
                                                set("payment_status", "Terbayar")
                                            }) { filter { eq("id", order.id ?: "") } }
                                            
                                            loadActiveTask()
                                        } catch (e: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Selesai Kirim", color = Color.White)
                            }
                        }
                    }
                }
            }
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Pengantaran", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            IconButton(onClick = { loadHistory() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = OrangeJco)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangeJco)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (completedOrders.isEmpty()) {
                    item {
                        Text("Belum ada pengantaran yang diselesaikan.", modifier = Modifier.fillMaxWidth().padding(top = 40.dp), textAlign = TextAlign.Center, color = Color.Gray)
                    }
                } else {
                    items(completedOrders) { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreen)
                                    Text("Selesai", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tujuan: ${order.deliveryAddress}", fontSize = 12.sp, color = DarkCharcoal)
                                Text("Total: Rp ${String.format("%,.0f", order.total)} | Pembayaran: ${order.paymentMethod}", fontSize = 11.sp, color = Color.Gray)
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(LightOrangeJco).border(2.dp, OrangeJco, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DeliveryDining, contentDescription = "Driver", tint = OrangeJco, modifier = Modifier.size(60.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Kurir Delivery Dapoer Lavana", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
        Text("Email: driver@lavana.com", fontSize = 14.sp, color = Color.Gray)
        Text("Level Akses: Mitra Pengirim / Driver", fontSize = 12.sp, color = OrangeJco, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = RedPromo),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Keluar dari Aplikasi", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
